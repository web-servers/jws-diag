package org.jboss.jws.diag.summary.discovery;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Parses key=value configuration files used by systemd service overrides,
 * such as {@code /etc/sysconfig/tomcat} and {@code /etc/default/tomcat}.
 *
 * <p>Handles blank lines, {@code #} comments, {@code export KEY=VALUE} syntax,
 * and single- or double-quoted values.
 */
class SystemdConfigParser {

    private final Path configFile;

    SystemdConfigParser(Path configFile) {
        this.configFile = configFile;
    }

    /**
     * Parses the config file and returns all key-value pairs found.
     * Returns an empty map if the file cannot be read.
     */
    Map<String, String> parse() {
        Map<String, String> result = new LinkedHashMap<>();
        List<String> lines;
        try {
            lines = Files.readAllLines(configFile);
        } catch (IOException e) {
            return result;
        }
        for (String raw : lines) {
            String line = raw.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            if (line.startsWith("export ")) {
                line = line.substring("export ".length()).trim();
            }
            int eqIdx = line.indexOf('=');
            if (eqIdx <= 0) {
                continue;
            }
            String key = line.substring(0, eqIdx).trim();
            String value = unquote(line.substring(eqIdx + 1).trim());
            if (!key.isEmpty()) {
                result.put(key, value);
            }
        }
        return result;
    }

    private static String unquote(String value) {
        if (value.length() >= 2) {
            char first = value.charAt(0);
            char last = value.charAt(value.length() - 1);
            if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
                return value.substring(1, value.length() - 1);
            }
        }
        return value;
    }
}
