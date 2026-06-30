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
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ConfigHumanFormatterTest {

    private final ConfigHumanFormatter formatter = new ConfigHumanFormatter();

    private static ServerConfig minimalServer(ConnectorConfig... connectors) {
        return ServerConfig.builder()
                .shutdownPort(8005)
                .shutdownCommand("SHUTDOWN")
                .listeners(Collections.emptyList())
                .services(List.of(ServiceConfig.builder()
                        .name("Catalina")
                        .connectors(List.of(connectors))
                        .executors(Collections.emptyList())
                        .engine(EngineConfig.builder()
                                .name("Catalina")
                                .defaultHost("localhost")
                                .hosts(List.of(HostConfig.builder()
                                        .name("localhost")
                                        .appBase(ConfigValue.defaulted("webapps"))
                                        .autoDeploy(ConfigValue.defaulted(true))
                                        .unpackWARs(ConfigValue.defaulted(true))
                                        .build()))
                                .build())
                        .build()))
                .build();
    }

    @Test
    void httpConnectorShowsPortAndProtocol() {
        ConnectorConfig conn = ConnectorConfig.builder()
                .port(8080)
                .protocol(ConfigValue.explicit("HTTP/1.1"))
                .sslEnabled(ConfigValue.defaulted(false))
                .maxThreads(ConfigValue.defaulted(200))
                .connectionTimeout(ConfigValue.explicit(20000))
                .maxConnections(ConfigValue.defaulted(8192))
                .compression(ConfigValue.defaulted("off"))
                .build();
        String output = formatter.format(minimalServer(conn));

        assertThat(output).contains("Connector :8080 [HTTP/1.1]");
        assertThat(output).doesNotContain("★ SSL");
        assertThat(output).contains("maxThreads: 200 (default)");
        assertThat(output).contains("connectionTimeout: 20000 (explicit)");
        assertThat(output).contains("compression: off (default)");
    }

    @Test
    void sslConnectorShowsStarMarker() {
        ConnectorConfig conn = ConnectorConfig.builder()
                .port(8443)
                .protocol(ConfigValue.explicit("HTTP/1.1"))
                .sslEnabled(ConfigValue.explicit(true))
                .maxThreads(ConfigValue.defaulted(200))
                .connectionTimeout(ConfigValue.defaulted(60000))
                .maxConnections(ConfigValue.defaulted(8192))
                .compression(ConfigValue.defaulted("off"))
                .build();
        String output = formatter.format(minimalServer(conn));

        assertThat(output).contains("Connector :8443 [HTTP/1.1] ★ SSL");
    }

    @Test
    void sslConnectorShowsSslHostConfig() {
        SslHostConfig ssl = SslHostConfig.builder()
                .protocols("TLSv1.2+TLSv1.3")
                .certificates(List.of(CertificateConfig.builder()
                        .keystoreFile("conf/server.jks")
                        .keystoreType(ConfigValue.defaulted("JKS"))
                        .keystorePass("***REDACTED***")
                        .build()))
                .build();
        ConnectorConfig conn = ConnectorConfig.builder()
                .port(8443)
                .protocol(ConfigValue.explicit("HTTP/1.1"))
                .sslEnabled(ConfigValue.explicit(true))
                .maxThreads(ConfigValue.defaulted(200))
                .connectionTimeout(ConfigValue.defaulted(60000))
                .maxConnections(ConfigValue.defaulted(8192))
                .compression(ConfigValue.defaulted("off"))
                .sslHostConfigs(List.of(ssl))
                .build();
        String output = formatter.format(minimalServer(conn));

        assertThat(output).contains("SSLHostConfig:");
        assertThat(output).contains("protocols: TLSv1.2+TLSv1.3");
        assertThat(output).contains("Certificate:");
        assertThat(output).contains("keystoreFile: conf/server.jks");
        assertThat(output).contains("keystoreType: JKS (default)");
        assertThat(output).contains("keystorePass: ***REDACTED***");
    }

    @Test
    void executorShownBeforeConnectors() {
        ExecutorConfig exec = ExecutorConfig.builder()
                .name("tomcatThreadPool")
                .namePrefix("catalina-exec-")
                .maxThreads(ConfigValue.explicit(150))
                .minSpareThreads(ConfigValue.explicit(4))
                .build();
        ServerConfig config = ServerConfig.builder()
                .shutdownPort(8005)
                .shutdownCommand("SHUTDOWN")
                .listeners(Collections.emptyList())
                .services(List.of(ServiceConfig.builder()
                        .name("Catalina")
                        .connectors(List.of(ConnectorConfig.builder()
                                .port(8080)
                                .protocol(ConfigValue.explicit("HTTP/1.1"))
                                .sslEnabled(ConfigValue.defaulted(false))
                                .maxThreads(ConfigValue.defaulted(200))
                                .connectionTimeout(ConfigValue.defaulted(60000))
                                .maxConnections(ConfigValue.defaulted(8192))
                                .compression(ConfigValue.defaulted("off"))
                                .executorRef("tomcatThreadPool")
                                .build()))
                        .executors(List.of(exec))
                        .engine(EngineConfig.builder()
                                .name("Catalina").defaultHost("localhost")
                                .hosts(Collections.emptyList()).build())
                        .build()))
                .build();
        String output = formatter.format(config);

        assertThat(output).contains("Executor \"tomcatThreadPool\"");
        assertThat(output).contains("maxThreads: 150 (explicit)");
        assertThat(output).contains("minSpareThreads: 4 (explicit)");
        assertThat(output).contains("namePrefix: catalina-exec-");
        assertThat(output).contains("executor: tomcatThreadPool");
        assertThat(output.indexOf("Executor")).isLessThan(output.indexOf("Connector"));
    }

    @Test
    void ajpConnectorShowsSecretRequired() {
        ConnectorConfig conn = ConnectorConfig.builder()
                .port(8009)
                .protocol(ConfigValue.explicit("AJP/1.3"))
                .sslEnabled(ConfigValue.defaulted(false))
                .maxThreads(ConfigValue.defaulted(200))
                .connectionTimeout(ConfigValue.defaulted(60000))
                .maxConnections(ConfigValue.defaulted(8192))
                .compression(ConfigValue.defaulted("off"))
                .secretRequired(ConfigValue.defaulted(true))
                .build();
        String output = formatter.format(minimalServer(conn));

        assertThat(output).contains("secretRequired: true (default)");
    }

    @Test
    void realmShownUnderHost() {
        RealmConfig realm = RealmConfig.builder().className("org.apache.catalina.realm.UserDatabaseRealm").build();
        ServerConfig config = ServerConfig.builder()
                .shutdownPort(8005)
                .shutdownCommand("SHUTDOWN")
                .listeners(Collections.emptyList())
                .services(List.of(ServiceConfig.builder()
                        .name("Catalina")
                        .connectors(Collections.emptyList())
                        .executors(Collections.emptyList())
                        .engine(EngineConfig.builder()
                                .name("Catalina").defaultHost("localhost")
                                .hosts(List.of(HostConfig.builder()
                                        .name("localhost")
                                        .appBase(ConfigValue.defaulted("webapps"))
                                        .autoDeploy(ConfigValue.defaulted(true))
                                        .unpackWARs(ConfigValue.defaulted(true))
                                        .realm(realm)
                                        .build()))
                                .build())
                        .build()))
                .build();
        String output = formatter.format(config);

        assertThat(output).contains("Realm: org.apache.catalina.realm.UserDatabaseRealm");
    }

    @Test
    void valveShownUnderHost() {
        Map<String, String> attrs = new LinkedHashMap<>();
        attrs.put("directory", "logs");
        attrs.put("pattern", "%h %t %r %s");
        ValveConfig valve = ValveConfig.builder()
                .className("org.apache.catalina.valves.AccessLogValve")
                .attributes(attrs)
                .build();
        ServerConfig config = ServerConfig.builder()
                .shutdownPort(8005)
                .shutdownCommand("SHUTDOWN")
                .listeners(Collections.emptyList())
                .services(List.of(ServiceConfig.builder()
                        .name("Catalina")
                        .connectors(Collections.emptyList())
                        .executors(Collections.emptyList())
                        .engine(EngineConfig.builder()
                                .name("Catalina").defaultHost("localhost")
                                .hosts(List.of(HostConfig.builder()
                                        .name("localhost")
                                        .appBase(ConfigValue.defaulted("webapps"))
                                        .autoDeploy(ConfigValue.defaulted(true))
                                        .unpackWARs(ConfigValue.defaulted(true))
                                        .valves(List.of(valve))
                                        .build()))
                                .build())
                        .build()))
                .build();
        String output = formatter.format(config);

        assertThat(output).contains("Valve: org.apache.catalina.valves.AccessLogValve");
        assertThat(output).contains("directory: logs");
    }

    @Test
    void multiServiceShowsServiceHeader() {
        ServiceConfig svc1 = ServiceConfig.builder()
                .name("Catalina")
                .connectors(List.of(ConnectorConfig.builder()
                        .port(8080)
                        .protocol(ConfigValue.explicit("HTTP/1.1"))
                        .sslEnabled(ConfigValue.defaulted(false))
                        .maxThreads(ConfigValue.defaulted(200))
                        .connectionTimeout(ConfigValue.defaulted(60000))
                        .maxConnections(ConfigValue.defaulted(8192))
                        .compression(ConfigValue.defaulted("off"))
                        .build()))
                .executors(Collections.emptyList())
                .engine(EngineConfig.builder().name("Catalina").defaultHost("localhost")
                        .hosts(Collections.emptyList()).build())
                .build();
        ServiceConfig svc2 = ServiceConfig.builder()
                .name("Admin")
                .connectors(List.of(ConnectorConfig.builder()
                        .port(9080)
                        .protocol(ConfigValue.defaulted("HTTP/1.1"))
                        .sslEnabled(ConfigValue.defaulted(false))
                        .maxThreads(ConfigValue.defaulted(200))
                        .connectionTimeout(ConfigValue.defaulted(60000))
                        .maxConnections(ConfigValue.defaulted(8192))
                        .compression(ConfigValue.defaulted("off"))
                        .build()))
                .executors(Collections.emptyList())
                .engine(EngineConfig.builder().name("Catalina").defaultHost("localhost")
                        .hosts(Collections.emptyList()).build())
                .build();
        ServerConfig config = ServerConfig.builder()
                .shutdownPort(8005)
                .shutdownCommand("SHUTDOWN")
                .listeners(Collections.emptyList())
                .services(List.of(svc1, svc2))
                .build();
        String output = formatter.format(config);

        assertThat(output).contains("Service: Catalina");
        assertThat(output).contains("Service: Admin");
    }

    @Test
    void proxyAttributesShown() {
        ConnectorConfig conn = ConnectorConfig.builder()
                .port(8080)
                .protocol(ConfigValue.explicit("HTTP/1.1"))
                .sslEnabled(ConfigValue.defaulted(false))
                .maxThreads(ConfigValue.defaulted(200))
                .connectionTimeout(ConfigValue.defaulted(60000))
                .maxConnections(ConfigValue.defaulted(8192))
                .compression(ConfigValue.defaulted("off"))
                .proxyName("example.com")
                .proxyPort(80)
                .build();
        String output = formatter.format(minimalServer(conn));

        assertThat(output).contains("proxyName: example.com");
        assertThat(output).contains("proxyPort: 80");
    }
}
