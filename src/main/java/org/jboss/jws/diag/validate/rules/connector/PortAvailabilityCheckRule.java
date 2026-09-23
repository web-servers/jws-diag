package org.jboss.jws.diag.validate.rules.connector;

import org.jboss.jws.diag.common.RuleId;
import org.jboss.jws.diag.common.Severity;
import org.jboss.jws.diag.instances.InstanceScanner;
import org.jboss.jws.diag.instances.model.TomcatInstance;
import org.jboss.jws.diag.validate.Rule;
import org.jboss.jws.diag.validate.RuleContext;
import org.jboss.jws.diag.validate.model.Finding;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * CONN-006: a port this installation needs is held by another process.
 *
 * <p>Reads the listening sockets from {@code /proc/net/tcp} and {@code /proc/net/tcp6}
 * rather than binding the port, so the check never acts on the host. When this
 * installation's own Tomcat is running, its connectors hold their ports; only a listener
 * that belongs to a different process is reported. Where {@code /proc} is not available
 * the check is skipped.
 */
public class PortAvailabilityCheckRule implements Rule {

    private static final Path DEFAULT_PROC = Path.of("/proc");

    private final Path procRoot;

    public PortAvailabilityCheckRule() {
        this(DEFAULT_PROC);
    }

    PortAvailabilityCheckRule(Path procRoot) {
        this.procRoot = procRoot;
    }

    @Override
    public List<Finding> evaluate(RuleContext ctx) {
        Document doc = ctx.getServerXml();
        if (doc == null) {
            return List.of();
        }

        List<ListeningSockets.Listener> listeners = ListeningSockets.read(procRoot);
        if (listeners == null || listeners.isEmpty()) {
            return List.of();
        }

        TomcatInstance self = new InstanceScanner(procRoot).findByCatalinaBase(ctx.getCatalinaBase());
        Set<String> ownInodes = self != null ? socketInodesOf(self.getPid()) : null;

        NodeList connectors = doc.getElementsByTagName("Connector");
        List<Finding> findings = new ArrayList<>();

        for (int i = 0; i < connectors.getLength(); i++) {
            Node connector = connectors.item(i);
            Node portAttr = connector.getAttributes().getNamedItem("port");
            if (portAttr == null) {
                continue;
            }
            int port;
            try {
                port = Integer.parseInt(portAttr.getNodeValue().trim());
            } catch (NumberFormatException e) {
                continue;
            }
            if (port <= 0) {
                continue;
            }
            Node addressAttr = connector.getAttributes().getNamedItem("address");
            String address = addressAttr != null ? addressAttr.getNodeValue().trim() : "";

            for (ListeningSockets.Listener listener : listeners) {
                if (listener.port != port || !overlaps(address, listener)) {
                    continue;
                }
                if (self != null) {
                    // This installation is running, so it normally owns the port. Only a
                    // listener we can attribute to another process is a conflict.
                    if (ownInodes == null || ownInodes.contains(listener.inode)) {
                        continue;
                    }
                    findings.add(finding(port, "Port " + port + " is held by a process other than this "
                            + "Tomcat (PID " + self.getPid() + "), so this connector could not bind it."));
                } else {
                    findings.add(finding(port, "Port " + port + " is already in use, so this Tomcat "
                            + "would fail to bind it on startup."));
                }
                break;
            }
        }
        return findings;
    }

    // A connector with no address, or 0.0.0.0 or ::, binds every address, so it collides with
    // any listener on the port. A specific address collides with a wildcard listener or the
    // same address. A host name cannot be compared without a lookup, so it is treated as
    // colliding, which errs towards reporting.
    private static boolean overlaps(String connectorAddress, ListeningSockets.Listener listener) {
        if (listener.isWildcard() || connectorAddress.isEmpty()) {
            return true;
        }
        byte[] literal = ListeningSockets.parseLiteral(connectorAddress);
        if (literal == null) {
            return true;
        }
        boolean connectorWildcard = true;
        for (byte b : literal) {
            if (b != 0) {
                connectorWildcard = false;
                break;
            }
        }
        return connectorWildcard || ListeningSockets.sameAddress(literal, listener.address);
    }

    // Socket inodes held by a process, from the "socket:[inode]" targets in /proc/<pid>/fd.
    // Returns null when the directory cannot be read, usually because the process belongs to
    // another user, so the caller knows attribution is not possible.
    private Set<String> socketInodesOf(int pid) {
        Path fdDir = procRoot.resolve(Integer.toString(pid)).resolve("fd");
        Set<String> inodes = new HashSet<>();
        try (DirectoryStream<Path> fds = Files.newDirectoryStream(fdDir)) {
            for (Path fd : fds) {
                try {
                    String target = Files.readSymbolicLink(fd).toString();
                    if (target.startsWith("socket:[") && target.endsWith("]")) {
                        inodes.add(target.substring("socket:[".length(), target.length() - 1));
                    }
                } catch (IOException | UnsupportedOperationException e) {
                    // A descriptor closed while reading, or not a link; skip it.
                }
            }
        } catch (IOException e) {
            return null;
        }
        return inodes;
    }

    private static Finding finding(int port, String detail) {
        return Finding.builder()
                .ruleId(RuleId.CONN_006)
                .category("Connector")
                .severity(Severity.WARN)
                .summary("Port Availability Check")
                .detail(detail)
                .file("Process State")
                .fix("Stop the process using port " + port + ", or change the Tomcat port in server.xml.")
                .build();
    }
}
