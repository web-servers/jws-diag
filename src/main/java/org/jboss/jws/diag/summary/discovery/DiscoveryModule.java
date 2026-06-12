package org.jboss.jws.diag.summary.discovery;

import org.jboss.jws.diag.summary.model.ContainerInfo;
import org.jboss.jws.diag.summary.model.JvmInfo;
import org.jboss.jws.diag.summary.model.JwsInstallation;
import org.jboss.jws.diag.summary.model.OsInfo;

import java.nio.file.Path;

/**
 * Orchestrates all detectors to build a complete {@link JwsInstallation}.
 *
 * <p>Each detector runs independently; a failure in one (e.g. container detection)
 * does not prevent the others from contributing to the result.
 */
public class DiscoveryModule {

    private final CatalinaDiscovery catalinaDiscovery;
    private final OsDetector osDetector;
    private final ContainerDetector containerDetector;
    private final JvmDetector.PropertySource jvmProperties;
    private final Path procRoot;

    /** Production factory — uses real filesystem and environment. */
    public static DiscoveryModule create(Path cliHome, Path cliBase) {
        CatalinaDiscovery catalina = CatalinaDiscovery.create(cliHome, cliBase);
        return new DiscoveryModule(
                catalina,
                OsDetector.create(),
                ContainerDetector.create(),
                System::getProperty,
                Path.of("/proc")
        );
    }

    DiscoveryModule(CatalinaDiscovery catalinaDiscovery,
                    OsDetector osDetector,
                    ContainerDetector containerDetector,
                    JvmDetector.PropertySource jvmProperties,
                    Path procRoot) {
        this.catalinaDiscovery = catalinaDiscovery;
        this.osDetector = osDetector;
        this.containerDetector = containerDetector;
        this.jvmProperties = jvmProperties;
        this.procRoot = procRoot;
    }

    public JwsInstallation discover() {
        CatalinaDiscovery.Result catalina = catalinaDiscovery.discover();
        Path catalinaHome = catalina.getCatalinaHome();

        OsInfo os = osDetector.detect();
        ContainerInfo container = containerDetector.detect();

        JvmInfo jvm = new JvmDetector(jvmProperties, procRoot, catalina.getPid()).detect();

        String tomcatVersion = new TomcatVersionDetector(catalinaHome).detect();
        String jwsVersion    = new JwsVersionDetector(catalinaHome).detect();

        return JwsInstallation.builder()
                .catalinaHome(catalinaHome)
                .catalinaBase(catalina.getCatalinaBase())
                .tomcatVersion(tomcatVersion)
                .jwsVersion(jwsVersion)
                .jvmInfo(jvm)
                .osInfo(os)
                .containerInfo(container)
                .pid(catalina.getPid())
                .build();
    }
}
