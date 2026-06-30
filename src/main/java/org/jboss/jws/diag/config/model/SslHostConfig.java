package org.jboss.jws.diag.config.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Collections;
import java.util.List;

/**
 * Represents an {@code <SSLHostConfig>} element nested inside an SSL-enabled {@code <Connector>}.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class SslHostConfig {

    private final String hostName;
    /** APR/OpenSSL connector protocol list (e.g. "TLSv1.2+TLSv1.3"). */
    private final String protocols;
    /** JSSE connector protocol list (e.g. "TLSv1.2,TLSv1.3"). Used by Http11NioProtocol. */
    private final String sslEnabledProtocols;
    private final String ciphers;
    private final String certificateVerification;
    private final List<CertificateConfig> certificates;

    private SslHostConfig(Builder b) {
        this.hostName = b.hostName;
        this.protocols = b.protocols;
        this.sslEnabledProtocols = b.sslEnabledProtocols;
        this.ciphers = b.ciphers;
        this.certificateVerification = b.certificateVerification;
        this.certificates = b.certificates != null
                ? Collections.unmodifiableList(b.certificates) : Collections.emptyList();
    }

    public String getHostName() { return hostName; }
    public String getProtocols() { return protocols; }
    public String getSslEnabledProtocols() { return sslEnabledProtocols; }
    public String getCiphers() { return ciphers; }
    public String getCertificateVerification() { return certificateVerification; }
    public List<CertificateConfig> getCertificates() { return certificates; }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String hostName;
        private String protocols;
        private String sslEnabledProtocols;
        private String ciphers;
        private String certificateVerification;
        private List<CertificateConfig> certificates;

        public Builder hostName(String v) { this.hostName = v; return this; }
        public Builder protocols(String v) { this.protocols = v; return this; }
        public Builder sslEnabledProtocols(String v) { this.sslEnabledProtocols = v; return this; }
        public Builder ciphers(String v) { this.ciphers = v; return this; }
        public Builder certificateVerification(String v) { this.certificateVerification = v; return this; }
        public Builder certificates(List<CertificateConfig> v) { this.certificates = v; return this; }

        public SslHostConfig build() { return new SslHostConfig(this); }
    }

    @Override
    public String toString() {
        return "SslHostConfig{hostName='" + hostName + "', protocols='" + protocols
                + "', sslEnabledProtocols='" + sslEnabledProtocols + "'}";
    }
}
