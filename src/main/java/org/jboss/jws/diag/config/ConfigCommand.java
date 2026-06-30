package org.jboss.jws.diag.config;

import org.jboss.jws.diag.common.ExitCodes;
import org.jboss.jws.diag.common.OutputFormat;
import org.jboss.jws.diag.common.OutputFormatMixin;
import org.jboss.jws.diag.config.formatter.ConfigHumanFormatter;
import org.jboss.jws.diag.config.formatter.ConfigJsonFormatter;
import org.jboss.jws.diag.config.model.ServerConfig;
import org.jboss.jws.diag.config.parser.PropertyResolver;
import org.jboss.jws.diag.config.parser.ServerXmlParser;
import org.jboss.jws.diag.summary.discovery.CatalinaDiscovery;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

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

    @Override
    public void run() {
        Path base = resolveBase();
        if (base == null) {
            System.err.println("ERROR: Could not determine CATALINA_BASE. "
                    + "Use --catalina-home or --catalina-base, or ensure Tomcat is running.");
            System.exit(ExitCodes.ERRORS);
            return;
        }

        Path serverXml = base.resolve("conf/server.xml");
        if (!Files.exists(serverXml)) {
            System.err.println("ERROR: server.xml not found at: " + serverXml);
            System.exit(ExitCodes.ERRORS);
            return;
        }

        ServerConfig config;
        try {
            PropertyResolver resolver = PropertyResolver.create(base);
            config = new ServerXmlParser(resolver).parse(serverXml);
        } catch (IOException e) {
            System.err.println("ERROR: Failed to parse server.xml: " + e.getMessage());
            System.exit(ExitCodes.ERRORS);
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

    private Path resolveBase() {
        if (catalinaBase != null) return catalinaBase;
        CatalinaDiscovery.Result result = CatalinaDiscovery.create(catalinaHome, null).discover();
        return result.getCatalinaBase();
    }
}
