package org.jboss.jws.diag.config.parser;

import org.jboss.jws.diag.config.model.CertificateConfig;
import org.jboss.jws.diag.config.model.ConfigValue;
import org.jboss.jws.diag.config.model.ConnectorConfig;
import org.jboss.jws.diag.config.model.ExecutorConfig;
import org.jboss.jws.diag.config.model.HostConfig;

/**
 * Tomcat 10.1.x compiled-in defaults for connector, host, and executor attributes.
 *
 * <p>Applied by {@link ServerXmlParser} when an attribute is absent from {@code server.xml}.
 */
public final class TomcatDefaults {

    public static final int     CONNECTOR_MAX_THREADS        = 200;
    public static final int     CONNECTOR_CONNECTION_TIMEOUT = 60000;
    public static final int     CONNECTOR_MAX_CONNECTIONS    = 8192;
    public static final String  CONNECTOR_PROTOCOL              = "HTTP/1.1";
    public static final boolean CONNECTOR_SSL_ENABLED           = false;
    public static final String  CONNECTOR_COMPRESSION           = "off";
    public static final boolean CONNECTOR_AJP_SECRET_REQUIRED   = true;

    public static final String  HOST_APP_BASE    = "webapps";
    public static final boolean HOST_AUTO_DEPLOY = true;
    public static final boolean HOST_UNPACK_WARS = true;

    public static final int    EXECUTOR_MAX_THREADS        = 200;
    public static final int    EXECUTOR_MIN_SPARE_THREADS  = 10;

    public static final String CERTIFICATE_KEYSTORE_TYPE   = "JKS";

    private TomcatDefaults() {}

    /** Fills null fields on {@code b} with Tomcat defaults. */
    public static void applyConnectorDefaults(ConnectorConfig.Builder b) {
        if (b.getProtocol() == null)
            b.protocol(ConfigValue.defaulted(CONNECTOR_PROTOCOL));
        if (b.getSslEnabled() == null)
            b.sslEnabled(ConfigValue.defaulted(CONNECTOR_SSL_ENABLED));
        if (b.getMaxThreads() == null)
            b.maxThreads(ConfigValue.defaulted(CONNECTOR_MAX_THREADS));
        if (b.getConnectionTimeout() == null)
            b.connectionTimeout(ConfigValue.defaulted(CONNECTOR_CONNECTION_TIMEOUT));
        if (b.getMaxConnections() == null)
            b.maxConnections(ConfigValue.defaulted(CONNECTOR_MAX_CONNECTIONS));
        if (b.getCompression() == null)
            b.compression(ConfigValue.defaulted(CONNECTOR_COMPRESSION));
        if (b.getSecretRequired() == null && isAjp(b))
            b.secretRequired(ConfigValue.defaulted(CONNECTOR_AJP_SECRET_REQUIRED));
    }

    private static boolean isAjp(ConnectorConfig.Builder b) {
        ConfigValue<String> proto = b.getProtocol();
        return proto != null && proto.getValue() != null
                && proto.getValue().toUpperCase(java.util.Locale.ROOT).contains("AJP");
    }

    public static void applyHostDefaults(HostConfig.Builder b) {
        if (b.getAppBase() == null)
            b.appBase(ConfigValue.defaulted(HOST_APP_BASE));
        if (b.getAutoDeploy() == null)
            b.autoDeploy(ConfigValue.defaulted(HOST_AUTO_DEPLOY));
        if (b.getUnpackWARs() == null)
            b.unpackWARs(ConfigValue.defaulted(HOST_UNPACK_WARS));
    }

    public static void applyExecutorDefaults(ExecutorConfig.Builder b) {
        if (b.getMaxThreads() == null)
            b.maxThreads(ConfigValue.defaulted(EXECUTOR_MAX_THREADS));
        if (b.getMinSpareThreads() == null)
            b.minSpareThreads(ConfigValue.defaulted(EXECUTOR_MIN_SPARE_THREADS));
    }

    public static void applyCertificateDefaults(CertificateConfig.Builder b) {
        if (b.getKeystoreType() == null)
            b.keystoreType(ConfigValue.defaulted(CERTIFICATE_KEYSTORE_TYPE));
    }
}
