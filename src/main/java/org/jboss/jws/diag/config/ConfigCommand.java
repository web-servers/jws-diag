package org.jboss.jws.diag.config;

import org.jboss.jws.diag.common.ExitCodes;
import org.jboss.jws.diag.common.MultiInstanceExitCode;
import org.jboss.jws.diag.common.OutputFormat;
import org.jboss.jws.diag.common.OutputFormatMixin;
import org.jboss.jws.diag.config.formatter.ConfigHumanFormatter;
import org.jboss.jws.diag.config.formatter.ConfigJsonFormatter;
import org.jboss.jws.diag.config.formatter.MultiConfigHumanFormatter;
import org.jboss.jws.diag.config.formatter.MultiConfigJsonFormatter;
import org.jboss.jws.diag.config.model.InstanceConfigResult;
import org.jboss.jws.diag.config.model.MultiConfigReport;
import org.jboss.jws.diag.config.model.ServerConfig;
import org.jboss.jws.diag.config.parser.PropertyResolver;
import org.jboss.jws.diag.config.parser.ServerXmlParser;
import org.jboss.jws.diag.instances.InstanceScanner;
import org.jboss.jws.diag.instances.model.TomcatInstance;
import org.jboss.jws.diag.summary.discovery.CatalinaDiscovery;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Command(name = "config",
        description = "Parse and display effective connector, TLS, proxy, and executor configuration",
        mixinStandardHelpOptions = true)
public class ConfigCommand implements Runnable {

    @Mixin
    private OutputFormatMixin outputFormat;

    @Option(names = "--catalina-home", description = "Path to CATALINA_HOME (overrides auto-detection)")
    private Path catalinaHome;

    @Option(names = "--catalina-base", description = "Path to CATALINA_BASE (defaults to CATALINA_HOME)")
    private Path catalinaBase;

    @Option(names = "--all",
            description = "Scan /proc for all running JWS instances and show config for each")
    private boolean all;

    @Override
    public void run() {
        if (all) {
            runMultiConfig();
        } else {
            runSingleConfig();
        }
    }

    private void runSingleConfig() {
        Path base = resolveBase();
        if (base == null) {
            System.err.println("ERROR: Could not determine CATALINA_BASE. "
                    + "Use --catalina-home or --catalina-base, or ensure Tomcat is running.");
            System.exit(ExitCodes.TOOL_FAILURE);
            return;
        }

        ServerConfig config = parseConfig(base);
        if (config == null) {
            System.exit(ExitCodes.TOOL_FAILURE);
            return;
        }

        String output;
        if (outputFormat.getFormat() == OutputFormat.JSON) {
            output = new ConfigJsonFormatter().format(config);
        } else {
            output = new ConfigHumanFormatter().format(config);
        }

        System.out.println(output);
        System.exit(ExitCodes.OK);
    }

    private void runMultiConfig() {
        List<TomcatInstance> instances = new InstanceScanner().scan();

        if (instances.isEmpty()) {
            System.err.println("ERROR: --all found no running JWS instances");
            System.exit(ExitCodes.TOOL_FAILURE);
            return;
        }

        List<InstanceConfigResult> results = new ArrayList<>();
        for (TomcatInstance inst : instances) {
            Path base = inst.getCatalinaBase();
            ServerConfig config = parseConfig(base);
            if (config != null) {
                results.add(new InstanceConfigResult(inst.getPid(), base, config));
            } else {
                System.err.println("WARN: Skipping PID " + inst.getPid()
                        + ": could not parse config for " + base);
            }
        }

        MultiConfigReport report = new MultiConfigReport(instances.size(), results);

        String output;
        if (outputFormat.getFormat() == OutputFormat.JSON) {
            output = new MultiConfigJsonFormatter().format(report);
        } else {
            output = new MultiConfigHumanFormatter().format(report);
        }

        System.out.println(output);

        if (results.isEmpty()) {
            System.err.println("ERROR: No instance config could be parsed ("
                    + instances.size() + " discovered, all skipped).");
        }
        System.exit(MultiInstanceExitCode.combine(ExitCodes.OK, results.size(), instances.size()));
    }

    private ServerConfig parseConfig(Path base) {
        Path serverXml = base.resolve("conf/server.xml");
        if (!Files.exists(serverXml)) {
            System.err.println("ERROR: server.xml not found at: " + serverXml);
            return null;
        }
        try {
            PropertyResolver resolver = PropertyResolver.create(base);
            return new ServerXmlParser(resolver).parse(serverXml);
        } catch (IOException e) {
            System.err.println("ERROR: Failed to parse server.xml: " + e.getMessage());
            return null;
        }
    }

    private Path resolveBase() {
        if (catalinaBase != null) {
            if (!Files.isDirectory(catalinaBase)) {
                System.err.println("ERROR: --catalina-base is not a valid directory: " + catalinaBase);
                System.exit(ExitCodes.TOOL_FAILURE);
            }
            return catalinaBase;
        }
        if (catalinaHome != null && !Files.isDirectory(catalinaHome)) {
            System.err.println("ERROR: --catalina-home is not a valid directory: " + catalinaHome);
            System.exit(ExitCodes.TOOL_FAILURE);
        }
        CatalinaDiscovery.Result result = CatalinaDiscovery.create(catalinaHome, null).discover();
        return result.getCatalinaBase();
    }
}
