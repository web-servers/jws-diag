package org.jboss.jws.diag.summary;

import org.jboss.jws.diag.common.ExitCodes;
import org.jboss.jws.diag.common.MultiInstanceExitCode;
import org.jboss.jws.diag.common.OutputFormat;
import org.jboss.jws.diag.common.OutputFormatMixin;
import org.jboss.jws.diag.instances.InstanceScanner;
import org.jboss.jws.diag.instances.model.TomcatInstance;
import org.jboss.jws.diag.summary.discovery.DiscoveryModule;
import org.jboss.jws.diag.summary.formatter.MultiSummaryHumanFormatter;
import org.jboss.jws.diag.summary.formatter.MultiSummaryJsonFormatter;
import org.jboss.jws.diag.summary.formatter.SummaryHumanFormatter;
import org.jboss.jws.diag.summary.formatter.SummaryJsonFormatter;
import org.jboss.jws.diag.summary.model.JwsInstallation;
import org.jboss.jws.diag.summary.model.MultiSummaryReport;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Command(name = "summary",
        description = "Show installed versions, JVM info, OS/container detection, and native library status",
        mixinStandardHelpOptions = true)
public class SummaryCommand implements Runnable {

    @Mixin
    private OutputFormatMixin outputFormat;

    @Option(names = "--catalina-home", description = "Path to CATALINA_HOME (overrides auto-detection)")
    private Path catalinaHome;

    @Option(names = "--catalina-base", description = "Path to CATALINA_BASE (defaults to CATALINA_HOME)")
    private Path catalinaBase;

    @Option(names = "--all",
            description = "Scan /proc for all running JWS instances and show summary for each")
    private boolean all;

    @Override
    public void run() {
        if (all) {
            runMultiSummary();
        } else {
            runSingleSummary();
        }
    }

    private void runSingleSummary() {
        JwsInstallation installation = DiscoveryModule.create(catalinaHome, catalinaBase).discover();

        String output;
        if (outputFormat.getFormat() == OutputFormat.JSON) {
            output = new SummaryJsonFormatter().format(installation);
        } else {
            output = new SummaryHumanFormatter().format(installation);
        }

        System.out.println(output);
        System.exit(ExitCodes.OK);
    }

    private void runMultiSummary() {
        List<TomcatInstance> instances = new InstanceScanner().scan();

        if (instances.isEmpty()) {
            System.err.println("ERROR: --all found no running JWS instances");
            System.exit(ExitCodes.TOOL_FAILURE);
            return;
        }

        List<JwsInstallation> installations = new ArrayList<>();
        for (TomcatInstance inst : instances) {
            try {
                JwsInstallation installation = DiscoveryModule
                        .create(inst.getCatalinaHome(), inst.getCatalinaBase())
                        .discover();
                installations.add(installation);
            } catch (Exception e) {
                System.err.println("WARN: Skipping PID " + inst.getPid()
                        + ": discovery failed: " + e.getMessage());
            }
        }

        MultiSummaryReport report = new MultiSummaryReport(instances.size(), installations);

        String output;
        if (outputFormat.getFormat() == OutputFormat.JSON) {
            output = new MultiSummaryJsonFormatter().format(report);
        } else {
            output = new MultiSummaryHumanFormatter().format(report);
        }

        System.out.println(output);

        if (installations.isEmpty()) {
            System.err.println("ERROR: No instance could be summarized ("
                    + instances.size() + " discovered, all skipped).");
        }
        System.exit(MultiInstanceExitCode.combine(ExitCodes.OK, installations.size(), instances.size()));
    }
}
