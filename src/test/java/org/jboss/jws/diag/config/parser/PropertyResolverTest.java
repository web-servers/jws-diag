package org.jboss.jws.diag.config.parser;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PropertyResolverTest {

    private PropertyResolver resolver(Map<String, String> sys,
                                      Map<String, String> cat,
                                      Map<String, String> env) {
        return new PropertyResolver(sys, cat, env);
    }

    @Test
    void nullInputReturnsNull() {
        PropertyResolver r = resolver(Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap());
        assertThat(r.resolve(null)).isNull();
    }

    @Test
    void plainValuePassedThrough() {
        PropertyResolver r = resolver(Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap());
        assertThat(r.resolve("HTTP/1.1")).isEqualTo("HTTP/1.1");
    }

    @Test
    void systemPropertyResolved() {
        Map<String, String> sys = new HashMap<>();
        sys.put("http.port", "8080");
        PropertyResolver r = resolver(sys, Collections.emptyMap(), Collections.emptyMap());
        assertThat(r.resolve("${http.port}")).isEqualTo("8080");
    }

    @Test
    void catalinaPropertyResolved() {
        Map<String, String> cat = new HashMap<>();
        cat.put("app.base", "webapps");
        PropertyResolver r = resolver(Collections.emptyMap(), cat, Collections.emptyMap());
        assertThat(r.resolve("${app.base}")).isEqualTo("webapps");
    }

    @Test
    void systemPropertyTakesPrecedenceOverCatalina() {
        Map<String, String> sys = new HashMap<>();
        sys.put("shared.key", "from-system");
        Map<String, String> cat = new HashMap<>();
        cat.put("shared.key", "from-catalina");
        PropertyResolver r = resolver(sys, cat, Collections.emptyMap());
        assertThat(r.resolve("${shared.key}")).isEqualTo("from-system");
    }

    @Test
    void envPrefixResolved() {
        Map<String, String> env = new HashMap<>();
        env.put("CATALINA_HOME", "/opt/tomcat");
        PropertyResolver r = resolver(Collections.emptyMap(), Collections.emptyMap(), env);
        assertThat(r.resolve("${env.CATALINA_HOME}")).isEqualTo("/opt/tomcat");
    }

    @Test
    void vaultTokenPreservedAsIs() {
        PropertyResolver r = resolver(Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap());
        assertThat(r.resolve("${VAULT::block::attr::}")).isEqualTo("${VAULT::block::attr::}");
    }

    @Test
    void unresolvedPlaceholderKeptAsOriginal() {
        PropertyResolver r = resolver(Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap());
        assertThat(r.resolve("${unknown.key}")).isEqualTo("${unknown.key}");
    }

    @Test
    void multiplePlaceholdersInSingleValue() {
        Map<String, String> sys = new HashMap<>();
        sys.put("host", "localhost");
        sys.put("port", "8080");
        PropertyResolver r = resolver(sys, Collections.emptyMap(), Collections.emptyMap());
        assertThat(r.resolve("${host}:${port}")).isEqualTo("localhost:8080");
    }

    @Test
    void noPlaceholderValueUnchanged() {
        PropertyResolver r = resolver(Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap());
        assertThat(r.resolve("static-value-123")).isEqualTo("static-value-123");
    }
}
