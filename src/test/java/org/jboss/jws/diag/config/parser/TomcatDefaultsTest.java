package org.jboss.jws.diag.config.parser;

import org.jboss.jws.diag.config.model.ConfigValue;
import org.jboss.jws.diag.config.model.ConnectorConfig;
import org.jboss.jws.diag.config.model.ExecutorConfig;
import org.jboss.jws.diag.config.model.HostConfig;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TomcatDefaultsTest {

    @Test
    void connectorDefaultsAppliedWhenAllAbsent() {
        ConnectorConfig.Builder b = ConnectorConfig.builder().port(8080);
        TomcatDefaults.applyConnectorDefaults(b);
        ConnectorConfig c = b.build();

        assertThat(c.getProtocol().getValue()).isEqualTo("HTTP/1.1");
        assertThat(c.getProtocol().isExplicit()).isFalse();

        assertThat(c.getSslEnabled().getValue()).isFalse();
        assertThat(c.getSslEnabled().isExplicit()).isFalse();

        assertThat(c.getMaxThreads().getValue()).isEqualTo(200);
        assertThat(c.getMaxThreads().isExplicit()).isFalse();

        assertThat(c.getConnectionTimeout().getValue()).isEqualTo(60000);
        assertThat(c.getConnectionTimeout().isExplicit()).isFalse();

        assertThat(c.getMaxConnections().getValue()).isEqualTo(8192);
        assertThat(c.getMaxConnections().isExplicit()).isFalse();

        assertThat(c.getCompression().getValue()).isEqualTo("off");
        assertThat(c.getCompression().isExplicit()).isFalse();

        // HTTP connector: no secretRequired default
        assertThat(c.getSecretRequired()).isNull();
    }

    @Test
    void connectorExplicitValueNotOverwritten() {
        ConnectorConfig.Builder b = ConnectorConfig.builder()
                .port(8080)
                .maxThreads(ConfigValue.explicit(500))
                .protocol(ConfigValue.explicit("org.apache.coyote.http11.Http11NioProtocol"));
        TomcatDefaults.applyConnectorDefaults(b);
        ConnectorConfig c = b.build();

        assertThat(c.getMaxThreads().getValue()).isEqualTo(500);
        assertThat(c.getMaxThreads().isExplicit()).isTrue();

        assertThat(c.getProtocol().getValue()).isEqualTo("org.apache.coyote.http11.Http11NioProtocol");
        assertThat(c.getProtocol().isExplicit()).isTrue();

        // remaining fields still get defaults
        assertThat(c.getConnectionTimeout().getValue()).isEqualTo(60000);
        assertThat(c.getConnectionTimeout().isExplicit()).isFalse();
    }

    @Test
    void hostDefaultsAppliedWhenAllAbsent() {
        HostConfig.Builder b = HostConfig.builder().name("localhost");
        TomcatDefaults.applyHostDefaults(b);
        HostConfig h = b.build();

        assertThat(h.getAppBase().getValue()).isEqualTo("webapps");
        assertThat(h.getAppBase().isExplicit()).isFalse();

        assertThat(h.getAutoDeploy().getValue()).isTrue();
        assertThat(h.getAutoDeploy().isExplicit()).isFalse();

        assertThat(h.getUnpackWARs().getValue()).isTrue();
        assertThat(h.getUnpackWARs().isExplicit()).isFalse();
    }

    @Test
    void hostExplicitValueNotOverwritten() {
        HostConfig.Builder b = HostConfig.builder()
                .name("localhost")
                .autoDeploy(ConfigValue.explicit(false));
        TomcatDefaults.applyHostDefaults(b);
        HostConfig h = b.build();

        assertThat(h.getAutoDeploy().getValue()).isFalse();
        assertThat(h.getAutoDeploy().isExplicit()).isTrue();

        // appBase and unpackWARs get defaults
        assertThat(h.getAppBase().getValue()).isEqualTo("webapps");
        assertThat(h.getUnpackWARs().getValue()).isTrue();
    }

    @Test
    void executorDefaultsAppliedWhenAllAbsent() {
        ExecutorConfig.Builder b = ExecutorConfig.builder().name("pool");
        TomcatDefaults.applyExecutorDefaults(b);
        ExecutorConfig e = b.build();

        assertThat(e.getMaxThreads().getValue()).isEqualTo(200);
        assertThat(e.getMaxThreads().isExplicit()).isFalse();

        assertThat(e.getMinSpareThreads().getValue()).isEqualTo(10);
        assertThat(e.getMinSpareThreads().isExplicit()).isFalse();
    }

    @Test
    void executorExplicitMaxThreadsNotOverwritten() {
        ExecutorConfig.Builder b = ExecutorConfig.builder()
                .name("pool")
                .maxThreads(ConfigValue.explicit(150));
        TomcatDefaults.applyExecutorDefaults(b);
        ExecutorConfig e = b.build();

        assertThat(e.getMaxThreads().getValue()).isEqualTo(150);
        assertThat(e.getMaxThreads().isExplicit()).isTrue();

        assertThat(e.getMinSpareThreads().getValue()).isEqualTo(10);
        assertThat(e.getMinSpareThreads().isExplicit()).isFalse();
    }

    @Test
    void ajpConnectorGetsSecretRequiredDefault() {
        ConnectorConfig.Builder b = ConnectorConfig.builder()
                .port(8009)
                .protocol(ConfigValue.explicit("AJP/1.3"));
        TomcatDefaults.applyConnectorDefaults(b);
        ConnectorConfig c = b.build();

        assertThat(c.getSecretRequired()).isNotNull();
        assertThat(c.getSecretRequired().getValue()).isTrue();
        assertThat(c.getSecretRequired().isExplicit()).isFalse();
    }

    @Test
    void httpConnectorDoesNotGetSecretRequired() {
        ConnectorConfig.Builder b = ConnectorConfig.builder()
                .port(8080)
                .protocol(ConfigValue.explicit("HTTP/1.1"));
        TomcatDefaults.applyConnectorDefaults(b);
        ConnectorConfig c = b.build();

        assertThat(c.getSecretRequired()).isNull();
    }

    @Test
    void explicitSecretRequiredNotOverwritten() {
        ConnectorConfig.Builder b = ConnectorConfig.builder()
                .port(8009)
                .protocol(ConfigValue.explicit("AJP/1.3"))
                .secretRequired(ConfigValue.explicit(false));
        TomcatDefaults.applyConnectorDefaults(b);
        ConnectorConfig c = b.build();

        assertThat(c.getSecretRequired().getValue()).isFalse();
        assertThat(c.getSecretRequired().isExplicit()).isTrue();
    }

    @Test
    void compressionDefaultIsStringOff() {
        ConnectorConfig.Builder b = ConnectorConfig.builder().port(8080);
        TomcatDefaults.applyConnectorDefaults(b);
        ConnectorConfig c = b.build();

        assertThat(c.getCompression().getValue()).isEqualTo("off");
        assertThat(c.getCompression().isExplicit()).isFalse();
    }
}
