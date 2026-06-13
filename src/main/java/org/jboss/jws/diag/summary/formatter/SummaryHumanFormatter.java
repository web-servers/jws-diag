package org.jboss.jws.diag.summary.formatter;

import org.jboss.jws.diag.summary.model.ContainerInfo;
import org.jboss.jws.diag.summary.model.ContainerType;
import org.jboss.jws.diag.summary.model.JvmInfo;
import org.jboss.jws.diag.summary.model.JwsInstallation;
import org.jboss.jws.diag.summary.model.NativeInfo;
import org.jboss.jws.diag.summary.model.OsInfo;

/**
 * Formats a {@link JwsInstallation} as a human-readable summary intended for
 * SREs triaging production issues: key facts first, no scrolling required.
 *
 * <pre>
 * Tomcat 10.1.49 | JWS 6.1.0
 * CATALINA_HOME: /opt/rh/jws6/root/usr/share/tomcat
 * CATALINA_BASE: /opt/rh/jws6/root/etc/tomcat
 * JVM: 17.0.10 (Red Hat, Inc.) | OS: RHEL 9.3 (x86_64)
 * Container: Podman | Native: APR 1.7.2, OpenSSL 3.0.9 ✓
 * PID: 12345
 * </pre>
 */
public class SummaryHumanFormatter {

    public String format(JwsInstallation installation) {
        StringBuilder sb = new StringBuilder();

        String tomcat = installation.getTomcatVersion() != null
                ? "Tomcat " + installation.getTomcatVersion() : "Tomcat N/A";
        String jws = installation.getJwsVersion() != null
                ? " | JWS " + installation.getJwsVersion() : "";
        sb.append(tomcat).append(jws).append('\n');

        sb.append("CATALINA_HOME: ")
                .append(installation.getCatalinaHome() != null ? installation.getCatalinaHome() : "N/A")
                .append('\n');
        sb.append("CATALINA_BASE: ")
                .append(installation.getCatalinaBase() != null ? installation.getCatalinaBase() : "N/A")
                .append('\n');

        sb.append("JVM: ").append(formatJvm(installation.getJvmInfo()))
                .append(" | OS: ").append(formatOs(installation.getOsInfo()))
                .append('\n');

        sb.append("Container: ").append(formatContainer(installation.getContainerInfo()));
        String nat = formatNative(installation.getNativeInfo());
        if (nat != null) {
            sb.append(" | Native: ").append(nat);
        }
        sb.append('\n');

        if (installation.getPid() != null) {
            sb.append("PID: ").append(installation.getPid()).append('\n');
        }

        return sb.toString().stripTrailing();
    }

    private static String formatJvm(JvmInfo jvm) {
        if (jvm == null) {
            return "N/A";
        }
        String version = jvm.getVersion() != null ? jvm.getVersion() : "N/A";
        String vendor = jvm.getVendor() != null ? " (" + jvm.getVendor() + ")" : "";
        return version + vendor;
    }

    private static String formatOs(OsInfo os) {
        if (os == null) {
            return "N/A";
        }
        String name = os.getName() != null ? os.getName() : "N/A";
        String version = os.getVersion() != null ? " " + os.getVersion() : "";
        String arch = os.getArch() != null ? " (" + os.getArch() + ")" : "";
        return name + version + arch;
    }

    private static String formatContainer(ContainerInfo container) {
        if (container == null || container.getType() == null) {
            return "N/A";
        }
        ContainerType type = container.getType();
        if (type == ContainerType.DOCKER) return "Docker";
        if (type == ContainerType.PODMAN) return "Podman";
        if (type == ContainerType.KUBERNETES) return "Kubernetes";
        if (type == ContainerType.CONTAINERD) return "containerd";
        return "None";
    }

    private static String formatNative(NativeInfo nativeInfo) {
        if (nativeInfo == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        if (nativeInfo.getAprVersion() != null) {
            sb.append("APR ").append(nativeInfo.getAprVersion());
        }
        if (nativeInfo.getOpensslVersion() != null) {
            if (sb.length() > 0) sb.append(", ");
            sb.append("OpenSSL ").append(nativeInfo.getOpensslVersion());
        }
        if (Boolean.TRUE.equals(nativeInfo.isLoaded())) {
            sb.append(" ✓");
        }
        return sb.length() > 0 ? sb.toString() : null;
    }
}
