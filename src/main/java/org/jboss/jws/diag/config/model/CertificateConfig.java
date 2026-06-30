package org.jboss.jws.diag.config.model;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Represents a {@code <Certificate>} element nested inside {@code <SSLHostConfig>}.
 *
 * <p>{@code keystorePass} is always either {@code ***REDACTED***} (when a literal value is
 * present) or a {@code ${VAULT::...}} opaque token — never the actual secret.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class CertificateConfig {

    private final String keystoreFile;
    private final ConfigValue<String> keystoreType;
    private final String keystorePass;
    private final String type;

    private CertificateConfig(Builder b) {
        this.keystoreFile = b.keystoreFile;
        this.keystoreType = b.keystoreType;
        this.keystorePass = b.keystorePass;
        this.type = b.type;
    }

    public String getKeystoreFile() { return keystoreFile; }
    public ConfigValue<String> getKeystoreType() { return keystoreType; }
    public String getKeystorePass() { return keystorePass; }
    public String getType() { return type; }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String keystoreFile;
        private ConfigValue<String> keystoreType;
        private String keystorePass;
        private String type;

        public Builder keystoreFile(String v) { this.keystoreFile = v; return this; }
        public Builder keystoreType(ConfigValue<String> v) { this.keystoreType = v; return this; }
        public Builder keystorePass(String v) { this.keystorePass = v; return this; }
        public Builder type(String v) { this.type = v; return this; }

        public ConfigValue<String> getKeystoreType() { return keystoreType; }

        public CertificateConfig build() { return new CertificateConfig(this); }
    }

    @Override
    public String toString() {
        return "CertificateConfig{keystoreFile='" + keystoreFile
                + "', keystoreType=" + keystoreType + ", type='" + type + "'}";
    }
}
