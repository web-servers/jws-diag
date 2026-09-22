package org.jboss.jws.diag.validate.rules.security;

import org.jboss.jws.diag.instances.InstanceScanner;
import org.jboss.jws.diag.instances.model.TomcatInstance;
import org.jboss.jws.diag.validate.Rule;
import org.jboss.jws.diag.validate.RuleContext;
import org.jboss.jws.diag.validate.model.Finding;
import org.jboss.jws.diag.common.RuleId;
import org.jboss.jws.diag.common.Severity;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * SEC-001: the Tomcat process for this installation runs as root.
 *
 * <p>Checks the running Tomcat process, found by its CATALINA_BASE, and reads its
 * effective UID from {@code /proc/<pid>/status}. It deliberately ignores the user running
 * jws-diag: an operator commonly runs {@code sudo jws-diag validate}, and that says nothing
 * about how Tomcat runs. When this installation is not running, or there is no
 * {@code /proc}, the user cannot be known and the check is skipped.
 */
public class RootUserCheckRule implements Rule {

    private static final Path DEFAULT_PROC = Path.of("/proc");

    private final Path procRoot;

    public RootUserCheckRule() {
        this(DEFAULT_PROC);
    }

    RootUserCheckRule(Path procRoot) {
        this.procRoot = procRoot;
    }

    @Override
    public List<Finding> evaluate(RuleContext ctx) {
        TomcatInstance tomcat = new InstanceScanner(procRoot).findByCatalinaBase(ctx.getCatalinaBase());
        if (tomcat == null) {
            return List.of();
        }
        Integer uid = effectiveUid(tomcat.getPid());
        if (uid == null || uid != 0) {
            return List.of();
        }
        return List.of(Finding.builder()
                .ruleId(RuleId.SEC_001)
                .category("Security")
                .severity(Severity.ERROR)
                .summary("Root User Check")
                .detail("The Tomcat process for this installation (PID " + tomcat.getPid()
                        + ") runs as root (UID 0).")
                .file("Process State")
                .fix("Run Tomcat as a dedicated, non-root system user")
                .build());
    }

    // The "Uid:" line lists real, effective, saved and filesystem UIDs. The effective UID is
    // what the kernel checks, so a process started as root that dropped privileges passes.
    private Integer effectiveUid(int pid) {
        Path status = procRoot.resolve(Integer.toString(pid)).resolve("status");
        try {
            for (String line : Files.readAllLines(status, StandardCharsets.UTF_8)) {
                if (line.startsWith("Uid:")) {
                    String[] fields = line.substring("Uid:".length()).trim().split("\\s+");
                    return fields.length >= 2 ? Integer.valueOf(fields[1]) : null;
                }
            }
        } catch (IOException | NumberFormatException e) {
            return null;
        }
        return null;
    }
}
