package org.jboss.jws.diag.config.parser;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resolves {@code ${property.name}} placeholders in server.xml attribute values.
 *
 * <p>Resolution order:
 * <ol>
 *   <li>System properties ({@code -D} flags)</li>
 *   <li>{@code catalina.properties} from {@code CATALINA_BASE/conf/}</li>
 *   <li>Environment variables via {@code ${env.VAR_NAME}}</li>
 *   <li>{@code ${VAULT::block::attr::}} — preserved as-is (opaque token)</li>
 *   <li>Unresolved placeholders — kept as original text</li>
 * </ol>
 */
public class PropertyResolver {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\$\\{([^}]+)}");

    private final Map<String, String> systemProperties;
    private final Map<String, String> catalinaProperties;
    private final Map<String, String> environment;

    /** Production factory — reads catalina.properties from disk. */
    public static PropertyResolver create(Path catalinaBase) {
        Map<String, String> catProps = loadCatalinaProperties(catalinaBase);
        Map<String, String> sysProps = new HashMap<>();
        System.getProperties().forEach((k, v) -> sysProps.put(k.toString(), v.toString()));
        return new PropertyResolver(sysProps, catProps, System.getenv());
    }

    public PropertyResolver(Map<String, String> systemProperties,
                     Map<String, String> catalinaProperties,
                     Map<String, String> environment) {
        this.systemProperties = systemProperties;
        this.catalinaProperties = catalinaProperties;
        this.environment = environment;
    }

    /**
     * Resolves all {@code ${...}} placeholders in {@code value}.
     * Returns {@code null} if {@code value} is {@code null}.
     */
    public String resolve(String value) {
        if (value == null) return null;
        Matcher m = PLACEHOLDER.matcher(value);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String key = m.group(1);
            m.appendReplacement(sb, Matcher.quoteReplacement(resolveKey(key)));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private String resolveKey(String key) {
        if (key.startsWith("VAULT::")) {
            return "${" + key + "}";
        }
        if (key.startsWith("env.")) {
            String envKey = key.substring(4);
            String val = environment.get(envKey);
            return val != null ? val : "${" + key + "}";
        }
        String val = systemProperties.get(key);
        if (val == null) val = catalinaProperties.get(key);
        return val != null ? val : "${" + key + "}";
    }

    private static Map<String, String> loadCatalinaProperties(Path catalinaBase) {
        if (catalinaBase == null) return Collections.emptyMap();
        Path file = catalinaBase.resolve("conf/catalina.properties");
        if (!Files.isRegularFile(file)) return Collections.emptyMap();
        Properties props = new Properties();
        try (InputStream in = Files.newInputStream(file)) {
            props.load(in);
        } catch (IOException e) {
            return Collections.emptyMap();
        }
        Map<String, String> map = new HashMap<>();
        props.forEach((k, v) -> map.put(k.toString(), v.toString()));
        return Collections.unmodifiableMap(map);
    }
}
