package org.jboss.jws.diag.common;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RedactionFilterTest {

    @Test
    void passwordAttributeIsRedacted() {
        assertThat(RedactionFilter.redact("keystorePassword", "secret123"))
                .isEqualTo("***REDACTED***");
    }

    @Test
    void certificateKeystorePasswordIsRedacted() {
        assertThat(RedactionFilter.redact("certificateKeystorePassword", "changeit"))
                .isEqualTo("***REDACTED***");
    }

    @Test
    void passSuffixIsRedacted() {
        assertThat(RedactionFilter.redact("keystorePass", "changeit"))
                .isEqualTo("***REDACTED***");
    }

    @Test
    void truststorePassIsRedacted() {
        assertThat(RedactionFilter.redact("truststorePass", "changeit"))
                .isEqualTo("***REDACTED***");
    }

    @Test
    void secretAttributeIsRedacted() {
        assertThat(RedactionFilter.redact("clientSecret", "abc123"))
                .isEqualTo("***REDACTED***");
    }

    @Test
    void credentialAttributeIsRedacted() {
        assertThat(RedactionFilter.redact("connectionCredential", "xyz"))
                .isEqualTo("***REDACTED***");
    }

    @Test
    void portNotRedacted() {
        assertThat(RedactionFilter.redact("port", "8080")).isEqualTo("8080");
    }

    @Test
    void classNameNotRedacted() {
        assertThat(RedactionFilter.redact("className",
                "org.apache.catalina.realm.UserDatabaseRealm"))
                .isEqualTo("org.apache.catalina.realm.UserDatabaseRealm");
    }

    @Test
    void vaultTokenPreservedForSensitiveAttribute() {
        assertThat(RedactionFilter.redact("certificateKeystorePassword",
                "${VAULT::ssl::keystorePassword::1}"))
                .isEqualTo("${VAULT::ssl::keystorePassword::1}");
    }

    @Test
    void nullValueReturnsNull() {
        assertThat(RedactionFilter.redact("keystorePass", null)).isNull();
    }
}
