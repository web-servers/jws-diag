package org.jboss.jws.diag.summary.discovery;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Detects the Tomcat version by reading {@code ServerInfo.properties} from
 * {@code CATALINA_HOME/lib/catalina.jar}.
 *
 * <p>Returns {@code null} when {@code catalinaHome} is null, the jar is missing,
 * or the properties entry cannot be read.
 */
class TomcatVersionDetector {

    private final Path catalinaHome;

    TomcatVersionDetector(Path catalinaHome) {
        this.catalinaHome = catalinaHome;
    }

    String detect() {
        if (catalinaHome == null) {
            return null;
        }
        Path jar = catalinaHome.resolve("lib/catalina.jar");
        if (!Files.exists(jar)) {
            return null;
        }
        try (ZipFile zip = new ZipFile(jar.toFile())) {
            ZipEntry entry = zip.getEntry("org/apache/catalina/util/ServerInfo.properties");
            if (entry == null) {
                return null;
            }
            Properties props = new Properties();
            try (InputStream is = zip.getInputStream(entry)) {
                props.load(is);
            }
            return props.getProperty("server.number");
        } catch (IOException e) {
            return null;
        }
    }
}
