package org.jboss.jws.diag.summary;

import org.jboss.jws.diag.common.ExitCodes;
import org.jboss.jws.diag.common.OutputFormat;
import org.jboss.jws.diag.common.OutputFormatMixin;
import org.jboss.jws.diag.summary.discovery.DiscoveryModule;
import org.jboss.jws.diag.summary.formatter.SummaryHumanFormatter;
import org.jboss.jws.diag.summary.formatter.SummaryJsonFormatter;
import org.jboss.jws.diag.summary.model.JwsInstallation;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

import java.nio.file.Path;

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

    @Override
    public void run() {
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
}
