package org.jboss.jws.diag.validate.rules.security;

import org.jboss.jws.diag.common.RuleId;
import org.jboss.jws.diag.common.Severity;
import org.jboss.jws.diag.validate.RuleContext;
import org.jboss.jws.diag.validate.model.Finding;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The rule looks at the Tomcat process through a fixture /proc tree, never at the user
 * running the test.
 */
public class RootUserCheckTest {

    @TempDir
    Path proc;

    @TempDir
    Path catalinaBase;

    @Test
    void tomcatRunningAsRoot_isError() throws IOException {
        runningTomcat(4242, catalinaBase, "Uid:\t0\t0\t0\t0");

        List<Finding> findings = evaluate("tomcat");

        assertThat(findings).hasSize(1);
        assertThat(findings.get(0).getRuleId()).isEqualTo(RuleId.SEC_001);
        assertThat(findings.get(0).getSeverity()).isEqualTo(Severity.ERROR);
        assertThat(findings.get(0).getDetail()).contains("PID 4242").contains("root (UID 0)");
    }

    @Test
    void tomcatRunningAsServiceUser_passes() throws IOException {
        runningTomcat(4242, catalinaBase, "Uid:\t91\t91\t91\t91");

        assertThat(evaluate("tomcat")).isEmpty();
    }

    @Test
    void jwsDiagRunAsRoot_doesNotFlagANonRootTomcat() throws IOException {
        // Regression for #66: the rule used to report the user running jws-diag.
        runningTomcat(4242, catalinaBase, "Uid:\t91\t91\t91\t91");

        assertThat(evaluate("root")).isEmpty();
    }

    @Test
    void jwsDiagRunAsRoot_withNoTomcatRunning_passes() {
        assertThat(evaluate("root")).isEmpty();
    }

    @Test
    void startedAsRootButDroppedPrivileges_passes() throws IOException {
        // Real UID 0, effective UID 91: the privilege drop jsvc performs.
        runningTomcat(4242, catalinaBase, "Uid:\t0\t91\t91\t91");

        assertThat(evaluate("tomcat")).isEmpty();
    }

    @Test
    void rootTomcatForAnotherInstallation_isIgnored(@TempDir Path otherBase) throws IOException {
        runningTomcat(4242, otherBase, "Uid:\t0\t0\t0\t0");

        assertThat(evaluate("tomcat")).isEmpty();
    }

    @Test
    void unreadableStatus_passes() throws IOException {
        runningTomcat(4242, catalinaBase, null);

        assertThat(evaluate("tomcat")).isEmpty();
    }

    private List<Finding> evaluate(String userRunningJwsDiag) {
        return new RootUserCheckRule(proc).evaluate(new RuleContext(catalinaBase, null, null, userRunningJwsDiag));
    }

    private void runningTomcat(int pid, Path base, String uidLine) throws IOException {
        Path pidDir = Files.createDirectories(proc.resolve(Integer.toString(pid)));
        String cmdline = String.join("\0", "/usr/bin/java",
                "-Dcatalina.home=" + base, "-Dcatalina.base=" + base,
                "org.apache.catalina.startup.Bootstrap", "start") + "\0";
        Files.write(pidDir.resolve("cmdline"), cmdline.getBytes(StandardCharsets.UTF_8));
        if (uidLine != null) {
            Files.writeString(pidDir.resolve("status"),
                    "Name:\tjava\nState:\tS (sleeping)\n" + uidLine + "\nGid:\t91\t91\t91\t91\n",
                    StandardCharsets.UTF_8);
        }
    }
}
