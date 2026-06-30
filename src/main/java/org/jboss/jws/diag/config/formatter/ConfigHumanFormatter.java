package org.jboss.jws.diag.config.formatter;

import org.jboss.jws.diag.config.model.CertificateConfig;
import org.jboss.jws.diag.config.model.ConfigValue;
import org.jboss.jws.diag.config.model.ConnectorConfig;
import org.jboss.jws.diag.config.model.EngineConfig;
import org.jboss.jws.diag.config.model.ExecutorConfig;
import org.jboss.jws.diag.config.model.HostConfig;
import org.jboss.jws.diag.config.model.RealmConfig;
import org.jboss.jws.diag.config.model.ServerConfig;
import org.jboss.jws.diag.config.model.ServiceConfig;
import org.jboss.jws.diag.config.model.SslHostConfig;
import org.jboss.jws.diag.config.model.ValveConfig;

import java.util.List;
import java.util.Map;

/**
 * Formats a {@link ServerConfig} as human-readable text intended for SREs
 * triaging production issues.
 *
 * <pre>
 * Connector :8080 [HTTP/1.1]
 *   maxThreads: 200 (default)
 *   connectionTimeout: 20000 (explicit)
 * Connector :8443 [HTTP/1.1] ★ SSL
 *   keystoreFile: conf/localhost-rsa.jks (explicit)
 *   keystoreType: JKS (default)
 * Executor "tomcatThreadPool"
 *   maxThreads: 150 (explicit)
 *   minSpareThreads: 4 (default)
 * </pre>
 */
public class ConfigHumanFormatter {

    private static final String INDENT = "  ";

    public String format(ServerConfig config) {
        StringBuilder sb = new StringBuilder();
        List<ServiceConfig> services = config.getServices();
        boolean multiService = services.size() > 1;

        for (ServiceConfig svc : services) {
            if (multiService) {
                sb.append("Service: ").append(svc.getName()).append('\n');
            }

            for (ExecutorConfig exec : svc.getExecutors()) {
                appendExecutor(sb, exec, multiService ? INDENT : "");
            }

            for (ConnectorConfig conn : svc.getConnectors()) {
                appendConnector(sb, conn, multiService ? INDENT : "");
            }

            if (svc.getEngine() != null) {
                appendEngine(sb, svc.getEngine(), multiService ? INDENT : "");
            }
        }

        return sb.toString().stripTrailing();
    }

    private void appendConnector(StringBuilder sb, ConnectorConfig conn, String prefix) {
        String protocol = conn.getProtocol() != null ? conn.getProtocol().getValue() : "HTTP/1.1";
        boolean ssl = conn.getSslEnabled() != null && Boolean.TRUE.equals(conn.getSslEnabled().getValue());
        sb.append(prefix)
          .append("Connector :").append(conn.getPort())
          .append(" [").append(protocol).append(']');
        if (ssl) sb.append(" ★ SSL");
        sb.append('\n');

        String ind = prefix + INDENT;
        appendConfigValue(sb, ind, "protocol", conn.getProtocol());
        appendConfigValue(sb, ind, "sslEnabled", conn.getSslEnabled());
        appendConfigValue(sb, ind, "maxThreads", conn.getMaxThreads());
        appendConfigValue(sb, ind, "connectionTimeout", conn.getConnectionTimeout());
        appendConfigValue(sb, ind, "maxConnections", conn.getMaxConnections());
        appendConfigValue(sb, ind, "compression", conn.getCompression());
        appendConfigValue(sb, ind, "secretRequired", conn.getSecretRequired());
        if (conn.getExecutorRef() != null)
            sb.append(ind).append("executor: ").append(conn.getExecutorRef()).append('\n');
        if (conn.getProxyName() != null)
            sb.append(ind).append("proxyName: ").append(conn.getProxyName()).append('\n');
        if (conn.getProxyPort() != null)
            sb.append(ind).append("proxyPort: ").append(conn.getProxyPort()).append('\n');
        if (conn.getSslHostConfigs() != null) {
            for (SslHostConfig sslCfg : conn.getSslHostConfigs()) {
                appendSsl(sb, sslCfg, ind);
            }
        }
    }

    private void appendSsl(StringBuilder sb, SslHostConfig ssl, String prefix) {
        sb.append(prefix).append("SSLHostConfig:\n");
        String ind = prefix + INDENT;
        if (ssl.getProtocols() != null)
            sb.append(ind).append("protocols: ").append(ssl.getProtocols()).append('\n');
        if (ssl.getSslEnabledProtocols() != null)
            sb.append(ind).append("sslEnabledProtocols: ").append(ssl.getSslEnabledProtocols()).append('\n');
        if (ssl.getCiphers() != null)
            sb.append(ind).append("ciphers: ").append(ssl.getCiphers()).append('\n');
        if (ssl.getCertificateVerification() != null)
            sb.append(ind).append("certificateVerification: ").append(ssl.getCertificateVerification()).append('\n');
        if (ssl.getCertificates() != null) {
            for (CertificateConfig cert : ssl.getCertificates()) {
                appendCertificate(sb, cert, ind);
            }
        }
    }

    private void appendCertificate(StringBuilder sb, CertificateConfig cert, String prefix) {
        sb.append(prefix).append("Certificate:\n");
        String ind = prefix + INDENT;
        if (cert.getKeystoreFile() != null)
            sb.append(ind).append("keystoreFile: ").append(cert.getKeystoreFile()).append('\n');
        appendConfigValue(sb, ind, "keystoreType", cert.getKeystoreType());
        if (cert.getKeystorePass() != null)
            sb.append(ind).append("keystorePass: ").append(cert.getKeystorePass()).append('\n');
        if (cert.getType() != null)
            sb.append(ind).append("type: ").append(cert.getType()).append('\n');
    }

    private void appendExecutor(StringBuilder sb, ExecutorConfig exec, String prefix) {
        sb.append(prefix).append("Executor \"").append(exec.getName()).append('"').append('\n');
        String ind = prefix + INDENT;
        appendConfigValue(sb, ind, "maxThreads", exec.getMaxThreads());
        appendConfigValue(sb, ind, "minSpareThreads", exec.getMinSpareThreads());
        if (exec.getNamePrefix() != null)
            sb.append(ind).append("namePrefix: ").append(exec.getNamePrefix()).append('\n');
    }

    private void appendEngine(StringBuilder sb, EngineConfig engine, String prefix) {
        if (engine.getRealm() != null) {
            sb.append(prefix).append("Realm: ").append(engine.getRealm().getClassName()).append('\n');
            appendNestedRealms(sb, engine.getRealm(), prefix + INDENT);
        }
        for (HostConfig host : engine.getHosts()) {
            appendHost(sb, host, prefix);
        }
    }

    private void appendHost(StringBuilder sb, HostConfig host, String prefix) {
        sb.append(prefix).append("Host: ").append(host.getName()).append('\n');
        String ind = prefix + INDENT;
        appendConfigValue(sb, ind, "appBase", host.getAppBase());
        appendConfigValue(sb, ind, "autoDeploy", host.getAutoDeploy());
        appendConfigValue(sb, ind, "unpackWARs", host.getUnpackWARs());
        if (host.getRealm() != null) {
            sb.append(ind).append("Realm: ").append(host.getRealm().getClassName()).append('\n');
            appendNestedRealms(sb, host.getRealm(), ind + INDENT);
        }
        for (ValveConfig valve : host.getValves()) {
            appendValve(sb, valve, ind);
        }
    }

    private void appendNestedRealms(StringBuilder sb, RealmConfig realm, String prefix) {
        if (realm.getNestedRealms() == null) return;
        for (RealmConfig nested : realm.getNestedRealms()) {
            sb.append(prefix).append("Realm: ").append(nested.getClassName()).append('\n');
            appendNestedRealms(sb, nested, prefix + INDENT);
        }
    }

    private void appendValve(StringBuilder sb, ValveConfig valve, String prefix) {
        sb.append(prefix).append("Valve: ").append(valve.getClassName()).append('\n');
        Map<String, String> attrs = valve.getAttributes();
        if (attrs != null && !attrs.isEmpty()) {
            String ind = prefix + INDENT;
            for (Map.Entry<String, String> e : attrs.entrySet()) {
                sb.append(ind).append(e.getKey()).append(": ").append(e.getValue()).append('\n');
            }
        }
    }

    private static <T> void appendConfigValue(StringBuilder sb, String prefix,
                                               String name, ConfigValue<T> cv) {
        if (cv == null) return;
        sb.append(prefix).append(name).append(": ").append(cv.getValue())
          .append(" (").append(cv.isExplicit() ? "explicit" : "default").append(")\n");
    }
}
