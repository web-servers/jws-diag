package org.jboss.jws.diag.validate.rules.connector;

import java.io.IOException;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * TCP sockets in the LISTEN state, read from {@code /proc/net/tcp} and {@code /proc/net/tcp6}.
 *
 * <p>Reading these files is how the port check learns what is listening without binding a
 * socket itself, which would be an action on the host. They are readable without privileges.
 */
final class ListeningSockets {

    private static final String LISTEN = "0A";

    private ListeningSockets() {
    }

    /** One listening socket. {@code address} is null for a wildcard bind (0.0.0.0 or ::). */
    static final class Listener {
        final byte[] address;
        final int port;
        final String inode;

        Listener(byte[] address, int port, String inode) {
            this.address = address;
            this.port = port;
            this.inode = inode;
        }

        boolean isWildcard() {
            return address == null;
        }
    }

    /**
     * All listening TCP sockets, or null when {@code procRoot/net/tcp} does not exist, which
     * means this is not Linux and the information is not available.
     */
    static List<Listener> read(Path procRoot) {
        Path tcp = procRoot.resolve("net/tcp");
        if (!Files.isRegularFile(tcp)) {
            return null;
        }
        List<Listener> listeners = new ArrayList<>();
        readInto(tcp, listeners);
        readInto(procRoot.resolve("net/tcp6"), listeners);
        return Collections.unmodifiableList(listeners);
    }

    // Line format: "sl local_address rem_address st ... uid timeout inode ...", with the
    // local address as HEXIP:HEXPORT. Unparseable lines are skipped, not fatal.
    private static void readInto(Path file, List<Listener> listeners) {
        List<String> lines;
        try {
            lines = Files.readAllLines(file, StandardCharsets.US_ASCII);
        } catch (IOException e) {
            return;
        }
        for (int i = 1; i < lines.size(); i++) {
            String[] columns = lines.get(i).trim().split("\\s+");
            if (columns.length < 10 || !LISTEN.equals(columns[3])) {
                continue;
            }
            int colon = columns[1].indexOf(':');
            if (colon < 0) {
                continue;
            }
            try {
                byte[] address = decodeAddress(columns[1].substring(0, colon));
                int port = Integer.parseInt(columns[1].substring(colon + 1), 16);
                listeners.add(new Listener(isAllZero(address) ? null : address, port, columns[9]));
            } catch (IllegalArgumentException e) {
                // Malformed line; ignore it rather than fail the whole check.
            }
        }
    }

    // The kernel writes each 32-bit word of the address in host byte order.
    private static byte[] decodeAddress(String hex) {
        if (hex.length() != 8 && hex.length() != 32) {
            throw new IllegalArgumentException("unexpected address length: " + hex);
        }
        byte[] bytes = new byte[hex.length() / 2];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) Integer.parseInt(hex.substring(i * 2, i * 2 + 2), 16);
        }
        if (ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN) {
            for (int word = 0; word < bytes.length; word += 4) {
                reverse(bytes, word, word + 3);
            }
        }
        return bytes;
    }

    private static void reverse(byte[] bytes, int from, int to) {
        while (from < to) {
            byte b = bytes[from];
            bytes[from++] = bytes[to];
            bytes[to--] = b;
        }
    }

    private static boolean isAllZero(byte[] bytes) {
        for (byte b : bytes) {
            if (b != 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * Parses an IP literal from a connector's {@code address} attribute without any name
     * lookup. Returns null for anything that is not a literal, such as a host name, because
     * resolving it would be a network request.
     */
    static byte[] parseLiteral(String address) {
        String trimmed = address.trim();
        if (trimmed.matches("\\d{1,3}(\\.\\d{1,3}){3}")) {
            byte[] bytes = new byte[4];
            String[] parts = trimmed.split("\\.");
            for (int i = 0; i < 4; i++) {
                int value = Integer.parseInt(parts[i]);
                if (value > 255) {
                    return null;
                }
                bytes[i] = (byte) value;
            }
            return bytes;
        }
        if (trimmed.contains(":") && trimmed.matches("[0-9A-Fa-f:.\\[\\]]+")) {
            try {
                // A literal IPv6 address never triggers a lookup.
                return java.net.InetAddress.getByName(trimmed.replace("[", "").replace("]", "")).getAddress();
            } catch (java.net.UnknownHostException e) {
                return null;
            }
        }
        return null;
    }

    static boolean sameAddress(byte[] a, byte[] b) {
        if (Arrays.equals(a, b)) {
            return true;
        }
        // An IPv4 address and its IPv4-mapped IPv6 form (::ffff:a.b.c.d) are the same bind.
        byte[] v4 = a.length == 4 ? a : b.length == 4 ? b : null;
        byte[] v6 = a.length == 16 ? a : b.length == 16 ? b : null;
        if (v4 == null || v6 == null) {
            return false;
        }
        for (int i = 0; i < 10; i++) {
            if (v6[i] != 0) {
                return false;
            }
        }
        return v6[10] == (byte) 0xff && v6[11] == (byte) 0xff
                && Arrays.equals(Arrays.copyOfRange(v6, 12, 16), v4);
    }
}
