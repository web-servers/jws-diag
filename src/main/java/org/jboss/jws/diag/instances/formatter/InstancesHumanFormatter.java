package org.jboss.jws.diag.instances.formatter;

import org.jboss.jws.diag.instances.model.TomcatInstance;

import java.util.List;

/**
 * Renders discovered Tomcat instances as human-readable text.
 *
 * <p>Example:
 * <pre>
 * Tomcat instances on this host: 2
 *
 * PID 12345
 *   CATALINA_HOME: /opt/rh/jws6/root/usr/share/tomcat
 *   CATALINA_BASE: /opt/rh/jws6/root/etc/tomcat
 *
 * PID 67890
 *   CATALINA_HOME: /opt/tomcat
 *   CATALINA_BASE: /opt/tomcat
 * </pre>
 */
public final class InstancesHumanFormatter {

    public String format(List<TomcatInstance> instances) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Tomcat instances on this host: %d%n", instances.size()));

        if (instances.isEmpty()) {
            sb.append("\nNo running Tomcat processes detected.\n");
            sb.append("Tip: Run as root or with ptrace permissions to scan all /proc entries.\n");
            return sb.toString();
        }

        for (TomcatInstance inst : instances) {
            sb.append('\n');
            sb.append(String.format("PID %d%n", inst.getPid()));
            sb.append(String.format("  CATALINA_HOME: %s%n",
                    inst.getCatalinaHome() != null ? inst.getCatalinaHome().toString().replace('\\', '/') : "(unknown)"));
            sb.append(String.format("  CATALINA_BASE: %s%n",
                    inst.getCatalinaBase() != null ? inst.getCatalinaBase().toString().replace('\\', '/') : "(unknown)"));
        }
        return sb.toString();
    }
}
