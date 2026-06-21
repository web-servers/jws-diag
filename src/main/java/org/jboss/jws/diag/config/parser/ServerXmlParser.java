package org.jboss.jws.diag.config.parser;

import org.jboss.jws.diag.common.RedactionFilter;
import org.jboss.jws.diag.config.model.CertificateConfig;
import org.jboss.jws.diag.config.model.ConfigValue;
import org.jboss.jws.diag.config.model.ConnectorConfig;
import org.jboss.jws.diag.config.model.EngineConfig;
import org.jboss.jws.diag.config.model.ExecutorConfig;
import org.jboss.jws.diag.config.model.HostConfig;
import org.jboss.jws.diag.config.model.ListenerConfig;
import org.jboss.jws.diag.config.model.RealmConfig;
import org.jboss.jws.diag.config.model.ServerConfig;
import org.jboss.jws.diag.config.model.ServiceConfig;
import org.jboss.jws.diag.config.model.SslHostConfig;
import org.jboss.jws.diag.config.model.ValveConfig;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * XXE-safe DOM parser for Tomcat {@code server.xml}.
 *
 * <p>Extracts connectors, executors, SSL host config, realms, hosts, and valves.
 * Applies {@link TomcatDefaults} for absent attributes, resolves {@code ${...}}
 * placeholders via {@link PropertyResolver}, and redacts sensitive values via
 * {@link RedactionFilter}.
 */
public final class ServerXmlParser {

    private final PropertyResolver resolver;

    public ServerXmlParser(PropertyResolver resolver) {
        this.resolver = resolver;
    }

    public ServerConfig parse(Path serverXml) throws IOException {
        Document doc = loadDocument(serverXml);
        Element root = doc.getDocumentElement();
        return ServerConfig.builder()
                .shutdownPort(intAttr(root, "port", 8005))
                .shutdownCommand(attr(root, "shutdown", "SHUTDOWN"))
                .listeners(parseListeners(root))
                .services(parseServices(root))
                .build();
    }

    private static Document loadDocument(Path path) throws IOException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        // Disable all external entity processing to prevent XXE attacks.
        try {
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        } catch (ParserConfigurationException e) {
            throw new IOException("Failed to configure XXE-safe parser", e);
        }
        factory.setExpandEntityReferences(false);
        factory.setNamespaceAware(false);

        DocumentBuilder builder;
        try {
            builder = factory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new IOException("Failed to create DocumentBuilder", e);
        }

        try (InputStream in = Files.newInputStream(path)) {
            return builder.parse(in);
        } catch (SAXException e) {
            throw new IOException("Failed to parse " + path.getFileName() + ": " + e.getMessage(), e);
        }
    }

    private List<ListenerConfig> parseListeners(Element server) {
        List<ListenerConfig> result = new ArrayList<>();
        NodeList nodes = server.getElementsByTagName("Listener");
        for (int i = 0; i < nodes.getLength(); i++) {
            Element el = (Element) nodes.item(i);
            if (el.getParentNode() == server) {
                result.add(ListenerConfig.builder()
                        .className(attr(el, "className", null))
                        .build());
            }
        }
        return result;
    }

    private List<ServiceConfig> parseServices(Element server) {
        List<ServiceConfig> result = new ArrayList<>();
        NodeList nodes = server.getElementsByTagName("Service");
        for (int i = 0; i < nodes.getLength(); i++) {
            Element el = (Element) nodes.item(i);
            if (el.getParentNode() == server) {
                result.add(parseService(el));
            }
        }
        return result;
    }

    private ServiceConfig parseService(Element service) {
        return ServiceConfig.builder()
                .name(attr(service, "name", null))
                .executors(parseExecutors(service))
                .connectors(parseConnectors(service))
                .engine(parseEngine(service))
                .build();
    }

    private List<ExecutorConfig> parseExecutors(Element service) {
        List<ExecutorConfig> result = new ArrayList<>();
        NodeList nodes = service.getElementsByTagName("Executor");
        for (int i = 0; i < nodes.getLength(); i++) {
            Element el = (Element) nodes.item(i);
            if (el.getParentNode() == service) {
                result.add(parseExecutor(el));
            }
        }
        return result;
    }

    private ExecutorConfig parseExecutor(Element el) {
        ExecutorConfig.Builder b = ExecutorConfig.builder()
                .name(attr(el, "name", null))
                .namePrefix(attr(el, "namePrefix", null));

        String maxThreads = el.getAttribute("maxThreads");
        if (!maxThreads.isEmpty())
            b.maxThreads(ConfigValue.explicit(Integer.parseInt(maxThreads)));

        String minSpare = el.getAttribute("minSpareThreads");
        if (!minSpare.isEmpty())
            b.minSpareThreads(ConfigValue.explicit(Integer.parseInt(minSpare)));

        TomcatDefaults.applyExecutorDefaults(b);
        return b.build();
    }

    private List<ConnectorConfig> parseConnectors(Element service) {
        List<ConnectorConfig> result = new ArrayList<>();
        NodeList nodes = service.getElementsByTagName("Connector");
        for (int i = 0; i < nodes.getLength(); i++) {
            Element el = (Element) nodes.item(i);
            if (el.getParentNode() == service) {
                result.add(parseConnector(el));
            }
        }
        return result;
    }

    private ConnectorConfig parseConnector(Element el) {
        ConnectorConfig.Builder b = ConnectorConfig.builder()
                .port(intAttr(el, "port", 8080));

        String protocol = attr(el, "protocol", null);
        if (protocol != null)
            b.protocol(ConfigValue.explicit(protocol));

        String ssl = attr(el, "SSLEnabled", null);
        if (ssl != null)
            b.sslEnabled(ConfigValue.explicit(Boolean.parseBoolean(ssl)));

        String maxThreads = el.getAttribute("maxThreads");
        if (!maxThreads.isEmpty())
            b.maxThreads(ConfigValue.explicit(Integer.parseInt(maxThreads)));

        String connTimeout = el.getAttribute("connectionTimeout");
        if (!connTimeout.isEmpty())
            b.connectionTimeout(ConfigValue.explicit(Integer.parseInt(connTimeout)));

        String maxConn = el.getAttribute("maxConnections");
        if (!maxConn.isEmpty())
            b.maxConnections(ConfigValue.explicit(Integer.parseInt(maxConn)));

        String compression = attr(el, "compression", null);
        if (compression != null)
            b.compression(ConfigValue.explicit(compression));

        String secretRequired = attr(el, "secretRequired", null);
        if (secretRequired != null)
            b.secretRequired(ConfigValue.explicit(Boolean.parseBoolean(secretRequired)));

        String executorRef = attr(el, "executor", null);
        if (executorRef != null)
            b.executorRef(executorRef);

        String proxyName = attr(el, "proxyName", null);
        if (proxyName != null)
            b.proxyName(proxyName);

        String proxyPort = el.getAttribute("proxyPort");
        if (!proxyPort.isEmpty())
            b.proxyPort(Integer.parseInt(proxyPort));

        List<SslHostConfig> sslConfigs = parseSslHostConfigs(el);
        if (!sslConfigs.isEmpty())
            b.ssl(sslConfigs.get(0));

        TomcatDefaults.applyConnectorDefaults(b);
        return b.build();
    }

    private List<SslHostConfig> parseSslHostConfigs(Element connector) {
        List<SslHostConfig> result = new ArrayList<>();
        NodeList nodes = connector.getElementsByTagName("SSLHostConfig");
        for (int i = 0; i < nodes.getLength(); i++) {
            Element el = (Element) nodes.item(i);
            if (el.getParentNode() == connector) {
                result.add(parseSslHostConfig(el));
            }
        }
        return result;
    }

    private SslHostConfig parseSslHostConfig(Element el) {
        List<CertificateConfig> certs = parseCertificates(el);
        return SslHostConfig.builder()
                .hostName(attr(el, "hostName", null))
                .protocols(attr(el, "protocols", null))
                .ciphers(attr(el, "ciphers", null))
                .certificateVerification(attr(el, "certificateVerification", null))
                .certificates(certs.isEmpty() ? null : certs)
                .build();
    }

    private List<CertificateConfig> parseCertificates(Element sslHostConfig) {
        List<CertificateConfig> result = new ArrayList<>();
        NodeList nodes = sslHostConfig.getElementsByTagName("Certificate");
        for (int i = 0; i < nodes.getLength(); i++) {
            Element el = (Element) nodes.item(i);
            if (el.getParentNode() == sslHostConfig) {
                result.add(parseCertificate(el));
            }
        }
        return result;
    }

    private CertificateConfig parseCertificate(Element el) {
        CertificateConfig.Builder b = CertificateConfig.builder()
                .keystoreFile(attr(el, "certificateKeystoreFile", null))
                .type(attr(el, "type", null));

        String keystorePass = attr(el, "certificateKeystorePassword", null);
        if (keystorePass != null)
            b.keystorePass(keystorePass);

        String keystoreType = attr(el, "certificateKeystoreType", null);
        if (keystoreType != null)
            b.keystoreType(ConfigValue.explicit(keystoreType));

        TomcatDefaults.applyCertificateDefaults(b);
        return b.build();
    }

    private EngineConfig parseEngine(Element service) {
        NodeList nodes = service.getElementsByTagName("Engine");
        for (int i = 0; i < nodes.getLength(); i++) {
            Element el = (Element) nodes.item(i);
            if (el.getParentNode() == service) {
                return EngineConfig.builder()
                        .name(attr(el, "name", null))
                        .defaultHost(attr(el, "defaultHost", null))
                        .hosts(parseHosts(el))
                        .realm(parseRealm(el))
                        .build();
            }
        }
        return null;
    }

    private List<HostConfig> parseHosts(Element engine) {
        List<HostConfig> result = new ArrayList<>();
        NodeList nodes = engine.getElementsByTagName("Host");
        for (int i = 0; i < nodes.getLength(); i++) {
            Element el = (Element) nodes.item(i);
            if (el.getParentNode() == engine) {
                result.add(parseHost(el));
            }
        }
        return result;
    }

    private HostConfig parseHost(Element el) {
        HostConfig.Builder b = HostConfig.builder()
                .name(attr(el, "name", null));

        String appBase = attr(el, "appBase", null);
        if (appBase != null)
            b.appBase(ConfigValue.explicit(appBase));

        String autoDeploy = attr(el, "autoDeploy", null);
        if (autoDeploy != null)
            b.autoDeploy(ConfigValue.explicit(Boolean.parseBoolean(autoDeploy)));

        String unpackWARs = attr(el, "unpackWARs", null);
        if (unpackWARs != null)
            b.unpackWARs(ConfigValue.explicit(Boolean.parseBoolean(unpackWARs)));

        List<ValveConfig> valves = parseValves(el);
        if (!valves.isEmpty())
            b.valves(valves);

        b.realm(parseRealm(el));

        TomcatDefaults.applyHostDefaults(b);
        return b.build();
    }

    private RealmConfig parseRealm(Element parent) {
        NodeList nodes = parent.getElementsByTagName("Realm");
        for (int i = 0; i < nodes.getLength(); i++) {
            Element el = (Element) nodes.item(i);
            if (el.getParentNode() == parent) {
                return parseRealmElement(el);
            }
        }
        return null;
    }

    private RealmConfig parseRealmElement(Element el) {
        List<RealmConfig> nested = new ArrayList<>();
        NodeList children = el.getElementsByTagName("Realm");
        for (int i = 0; i < children.getLength(); i++) {
            Element child = (Element) children.item(i);
            if (child.getParentNode() == el) {
                nested.add(parseRealmElement(child));
            }
        }
        return RealmConfig.builder()
                .className(attr(el, "className", null))
                .nestedRealms(nested.isEmpty() ? null : nested)
                .build();
    }

    private List<ValveConfig> parseValves(Element parent) {
        List<ValveConfig> result = new ArrayList<>();
        NodeList nodes = parent.getElementsByTagName("Valve");
        for (int i = 0; i < nodes.getLength(); i++) {
            Element el = (Element) nodes.item(i);
            if (el.getParentNode() == parent) {
                result.add(parseValve(el));
            }
        }
        return result;
    }

    private ValveConfig parseValve(Element el) {
        Map<String, String> attrs = new HashMap<>();
        for (int i = 0; i < el.getAttributes().getLength(); i++) {
            org.w3c.dom.Attr a = (org.w3c.dom.Attr) el.getAttributes().item(i);
            if (!"className".equals(a.getName())) {
                attrs.put(a.getName(), RedactionFilter.redact(a.getName(), resolver.resolve(a.getValue())));
            }
        }
        return ValveConfig.builder()
                .className(attr(el, "className", null))
                .attributes(attrs)
                .build();
    }

    private String attr(Element el, String name, String defaultValue) {
        String raw = el.getAttribute(name);
        if (raw == null || raw.isEmpty()) return defaultValue;
        return RedactionFilter.redact(name, resolver.resolve(raw));
    }

    private int intAttr(Element el, String name, int defaultValue) {
        String raw = el.getAttribute(name);
        if (raw == null || raw.isEmpty()) return defaultValue;
        try {
            return Integer.parseInt(resolver.resolve(raw));
        } catch (NumberFormatException ignored) {
            return defaultValue;
        }
    }
}
