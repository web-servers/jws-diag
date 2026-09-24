package org.jboss.jws.diag.validate.rules.tls;

import org.jboss.jws.diag.validate.RuleContext;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The locator has to find the keystore Tomcat would use, in each of the shapes Tomcat
 * accepts. See #69.
 */
class KeystoreLocatorTest {

    private static final Path BASE = Path.of("/opt/jws/standalone");

    @Test
    void readsCertificateAttributes() throws Exception {
        List<KeystoreLocator.Keystore> found = locate(
                "<Connector port=\"8443\"><SSLHostConfig><Certificate"
                        + " certificateKeystoreFile=\"conf/ks.p12\""
                        + " certificateKeystorePassword=\"secret\""
                        + " certificateKeystoreType=\"pkcs12\"/></SSLHostConfig></Connector>");

        assertThat(found).hasSize(1);
        assertThat(found.get(0).declaredFile).isEqualTo("conf/ks.p12");
        assertThat(found.get(0).password).isEqualTo("secret");
        assertThat(found.get(0).type).isEqualTo("PKCS12");
        assertThat(found.get(0).path).isEqualTo(BASE.resolve("conf/ks.p12"));
    }

    @Test
    void inheritsAttributesFromTheEnclosingSslHostConfig() throws Exception {
        List<KeystoreLocator.Keystore> found = locate(
                "<Connector port=\"8443\"><SSLHostConfig certificateKeystoreFile=\"conf/ks.jks\""
                        + " certificateKeystorePassword=\"secret\">"
                        + "<Certificate type=\"RSA\"/></SSLHostConfig></Connector>");

        assertThat(found).hasSize(1);
        assertThat(found.get(0).declaredFile).isEqualTo("conf/ks.jks");
        assertThat(found.get(0).password).isEqualTo("secret");
    }

    @Test
    void readsSslHostConfigWithoutANestedCertificate() throws Exception {
        List<KeystoreLocator.Keystore> found = locate(
                "<Connector port=\"8443\"><SSLHostConfig certificateKeystoreFile=\"conf/ks.jks\"/>"
                        + "</Connector>");

        assertThat(found).hasSize(1);
        assertThat(found.get(0).declaredFile).isEqualTo("conf/ks.jks");
    }

    @Test
    void readsTheOlderConnectorAttributes() throws Exception {
        List<KeystoreLocator.Keystore> found = locate(
                "<Connector port=\"8443\" keystoreFile=\"conf/ks.jks\" keystorePass=\"secret\"/>");

        assertThat(found).hasSize(1);
        assertThat(found.get(0).declaredFile).isEqualTo("conf/ks.jks");
        assertThat(found.get(0).password).isEqualTo("secret");
        assertThat(found.get(0).type).isEqualTo("JKS");
    }

    @Test
    void prefersSslHostConfigOverTheOlderConnectorAttributes() throws Exception {
        List<KeystoreLocator.Keystore> found = locate(
                "<Connector port=\"8443\" keystoreFile=\"conf/legacy.jks\">"
                        + "<SSLHostConfig><Certificate certificateKeystoreFile=\"conf/new.p12\"/>"
                        + "</SSLHostConfig></Connector>");

        assertThat(found).hasSize(1);
        assertThat(found.get(0).declaredFile).isEqualTo("conf/new.p12");
    }

    @Test
    void usesTheOlderConnectorAttributesWhenTheSslHostConfigNamesNoKeystore() throws Exception {
        // Tomcat folds the connector attributes into its default SSLHostConfig, so an
        // SSLHostConfig that only tunes protocols leaves the keystore where it was.
        List<KeystoreLocator.Keystore> found = locate(
                "<Connector port=\"8443\" keystoreFile=\"conf/ks.jks\" keystorePass=\"secret\">"
                        + "<SSLHostConfig protocols=\"TLSv1.2\"/></Connector>");

        assertThat(found).hasSize(1);
        assertThat(found.get(0).declaredFile).isEqualTo("conf/ks.jks");
        assertThat(found.get(0).password).isEqualTo("secret");
    }

    @Test
    void defaultsThePasswordToTomcatsOwnDefault() throws Exception {
        List<KeystoreLocator.Keystore> found = locate(
                "<Connector port=\"8443\"><SSLHostConfig>"
                        + "<Certificate certificateKeystoreFile=\"conf/ks.jks\"/></SSLHostConfig></Connector>");

        assertThat(found.get(0).password).isEqualTo("changeit");
    }

    @Test
    void infersTypeFromTheFileExtension() throws Exception {
        assertThat(locate("<Connector port=\"8443\" keystoreFile=\"conf/ks.pfx\"/>").get(0).type)
                .isEqualTo("PKCS12");
        assertThat(locate("<Connector port=\"8443\" keystoreFile=\"conf/ks.keystore\"/>").get(0).type)
                .isEqualTo("JKS");
    }

    @Test
    void expandsCatalinaBase() throws Exception {
        List<KeystoreLocator.Keystore> found = locate(
                "<Connector port=\"8443\" keystoreFile=\"${catalina.base}/conf/ks.jks\"/>");

        assertThat(found.get(0).isResolved()).isTrue();
        assertThat(found.get(0).path).isEqualTo(BASE.resolve("conf/ks.jks"));
    }

    @Test
    void keepsAnAbsolutePathAsIs() throws Exception {
        List<KeystoreLocator.Keystore> found = locate(
                "<Connector port=\"8443\" keystoreFile=\"/etc/pki/ks.jks\"/>");

        assertThat(found.get(0).path).isEqualTo(Path.of("/etc/pki/ks.jks"));
    }

    @Test
    void reportsAnUnresolvablePlaceholderAsUnresolved() throws Exception {
        List<KeystoreLocator.Keystore> found = locate(
                "<Connector port=\"8443\" keystoreFile=\"${catalina.home}/conf/ks.jks\"/>");

        assertThat(found).hasSize(1);
        assertThat(found.get(0).isResolved()).isFalse();
    }

    @Test
    void findsEveryConnectorsKeystore() throws Exception {
        List<KeystoreLocator.Keystore> found = locate(
                "<Connector port=\"8443\" keystoreFile=\"conf/a.jks\"/>"
                        + "<Connector port=\"9443\" keystoreFile=\"conf/b.jks\"/>");

        assertThat(found).extracting(k -> k.declaredFile).containsExactly("conf/a.jks", "conf/b.jks");
    }

    @Test
    void reportsTheSameKeystoreOnce() throws Exception {
        List<KeystoreLocator.Keystore> found = locate(
                "<Connector port=\"8443\"><SSLHostConfig hostName=\"a\">"
                        + "<Certificate certificateKeystoreFile=\"conf/ks.jks\" certificateKeystorePassword=\"p\"/>"
                        + "</SSLHostConfig><SSLHostConfig hostName=\"b\">"
                        + "<Certificate certificateKeystoreFile=\"conf/ks.jks\" certificateKeystorePassword=\"p\"/>"
                        + "</SSLHostConfig></Connector>");

        assertThat(found).hasSize(1);
    }

    @Test
    void findsNothingWithoutAKeystoreAttribute() throws Exception {
        assertThat(locate("<Connector port=\"8080\"/>")).isEmpty();
    }

    @Test
    void findsNothingWithoutServerXml() {
        assertThat(KeystoreLocator.locate(new RuleContext(BASE, null, null, "testuser"))).isEmpty();
    }

    private static List<KeystoreLocator.Keystore> locate(String connectorsXml) throws Exception {
        String xml = "<Server port=\"-1\" shutdown=\"SHUTDOWN\"><Service name=\"Catalina\">"
                + connectorsXml + "</Service></Server>";
        Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                .parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        return KeystoreLocator.locate(new RuleContext(BASE, doc, null, "testuser"));
    }
}
