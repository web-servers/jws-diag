package org.jboss.jws.diag.validate.rules.tls;

import org.jboss.jws.diag.validate.RuleContext;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Finds the keystores a {@code server.xml} configures, the way Tomcat reads them.
 *
 * <p>The TLS rules share this so they agree on which files to look at. It covers the three
 * shapes Tomcat accepts: attributes on {@code <Certificate>}, the same attributes inherited
 * from the enclosing {@code <SSLHostConfig>}, and the older {@code keystoreFile} form on
 * {@code <Connector>}.
 */
final class KeystoreLocator {

    /** Tomcat's default when no password attribute is present. */
    static final String DEFAULT_PASSWORD = "changeit";

    private static final String CATALINA_BASE_PROPERTY = "${catalina.base}";

    private KeystoreLocator() {
    }

    /** One configured keystore. */
    static final class Keystore {
        /** The path exactly as written in server.xml, used in messages. */
        final String declaredFile;
        /** The resolved path, or null when the path still holds an unresolved placeholder. */
        final Path path;
        final String password;
        final String type;

        Keystore(String declaredFile, Path path, String password, String type) {
            this.declaredFile = declaredFile;
            this.path = path;
            this.password = password;
            this.type = type;
        }

        /** False when the path could not be worked out, so the rules skip it rather than guess. */
        boolean isResolved() {
            return path != null;
        }
    }

    static List<Keystore> locate(RuleContext ctx) {
        Document doc = ctx.getServerXml();
        if (doc == null) {
            return List.of();
        }

        Map<String, Keystore> found = new LinkedHashMap<>();
        Set<Node> connectorsWithKeystore = new HashSet<>();

        NodeList certificates = doc.getElementsByTagName("Certificate");
        for (int i = 0; i < certificates.getLength(); i++) {
            Node certificate = certificates.item(i);
            if (add(found, ctx,
                    inherited(certificate, "certificateKeystoreFile"),
                    inherited(certificate, "certificateKeystorePassword"),
                    inherited(certificate, "certificateKeystoreType"))) {
                connectorEnclosing(certificate).ifPresent(connectorsWithKeystore::add);
            }
        }

        // An SSLHostConfig may carry the attributes itself, with no nested Certificate.
        NodeList sslHostConfigs = doc.getElementsByTagName("SSLHostConfig");
        for (int i = 0; i < sslHostConfigs.getLength(); i++) {
            Node sslHostConfig = sslHostConfigs.item(i);
            if (hasChildElement(sslHostConfig, "Certificate")) {
                continue;
            }
            if (add(found, ctx,
                    attribute(sslHostConfig, "certificateKeystoreFile"),
                    attribute(sslHostConfig, "certificateKeystorePassword"),
                    attribute(sslHostConfig, "certificateKeystoreType"))) {
                connectorEnclosing(sslHostConfig).ifPresent(connectorsWithKeystore::add);
            }
        }

        // The older form, still accepted by Tomcat and common in JWS 5 configurations.
        // Tomcat folds these attributes into the connector's default SSLHostConfig, so they
        // still apply when the SSLHostConfig elements name no keystore of their own. An empty
        // <SSLHostConfig/> is exactly that case.
        NodeList connectors = doc.getElementsByTagName("Connector");
        for (int i = 0; i < connectors.getLength(); i++) {
            Node connector = connectors.item(i);
            if (connectorsWithKeystore.contains(connector)) {
                continue;
            }
            add(found, ctx,
                    attribute(connector, "keystoreFile"),
                    attribute(connector, "keystorePass"),
                    attribute(connector, "keystoreType"));
        }

        return new ArrayList<>(found.values());
    }

    /** Records the keystore and reports whether the attributes named one at all. */
    private static boolean add(Map<String, Keystore> found, RuleContext ctx,
                               String file, String password, String type) {
        if (file == null) {
            return false;
        }
        // An attribute present but empty is kept, not skipped: it is a misconfiguration and
        // the rules already report it. Only an absent attribute means "no keystore here".
        String declaredFile = file.trim();
        String effectivePassword = password != null ? password : DEFAULT_PASSWORD;
        String effectiveType = type != null ? type.trim().toUpperCase() : typeFromExtension(declaredFile);
        String key = declaredFile + '|' + effectivePassword + '|' + effectiveType;
        found.computeIfAbsent(key, k ->
                new Keystore(declaredFile, resolve(ctx, declaredFile), effectivePassword, effectiveType));
        return true;
    }

    private static String typeFromExtension(String file) {
        String lower = file.toLowerCase();
        return lower.endsWith(".p12") || lower.endsWith(".pfx") ? "PKCS12" : "JKS";
    }

    /**
     * Expands the placeholders jws-diag knows and resolves a relative path against
     * CATALINA_BASE, as Tomcat does. Returns null when a placeholder remains, such as
     * {@code ${catalina.home}}, whose value is not known here. Reporting a missing file for
     * a path that was never really missing would be worse than reporting nothing.
     */
    private static Path resolve(RuleContext ctx, String declaredFile) {
        Path catalinaBase = ctx.getCatalinaBase();
        if (catalinaBase == null) {
            return null;
        }
        boolean startedAtBase = declaredFile.contains(CATALINA_BASE_PROPERTY);
        String expanded = declaredFile.replace(CATALINA_BASE_PROPERTY, catalinaBase.toString());
        if (expanded.contains("${")) {
            return null;
        }
        try {
            Path candidate = Path.of(expanded);
            // A path the placeholder already anchored at CATALINA_BASE must not be resolved
            // against it a second time, or the base appears twice.
            if (startedAtBase || candidate.isAbsolute()) {
                return candidate;
            }
            return catalinaBase.resolve(candidate);
        } catch (InvalidPathException e) {
            return null;
        }
    }

    // The attribute from this element, or the nearest ancestor up to and including Connector.
    private static String inherited(Node node, String name) {
        for (Node current = node; current != null; current = current.getParentNode()) {
            String value = attribute(current, name);
            if (value != null) {
                return value;
            }
            if ("Connector".equals(current.getNodeName())) {
                break;
            }
        }
        return null;
    }

    private static String attribute(Node node, String name) {
        if (node == null || node.getAttributes() == null) {
            return null;
        }
        Node attribute = node.getAttributes().getNamedItem(name);
        return attribute != null ? attribute.getNodeValue() : null;
    }

    private static boolean hasChildElement(Node node, String name) {
        NodeList children = node.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            if (children.item(i) instanceof Element && name.equals(children.item(i).getNodeName())) {
                return true;
            }
        }
        return false;
    }

    private static java.util.Optional<Node> connectorEnclosing(Node node) {
        for (Node current = node; current != null; current = current.getParentNode()) {
            if ("Connector".equals(current.getNodeName())) {
                return java.util.Optional.of(current);
            }
        }
        return java.util.Optional.empty();
    }
}
