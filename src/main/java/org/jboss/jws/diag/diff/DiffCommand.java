package org.jboss.jws.diag.diff;

import org.jboss.jws.diag.common.ExitCodes;
import org.jboss.jws.diag.common.MultiInstanceExitCode;
import org.jboss.jws.diag.common.OutputFormat;
import org.jboss.jws.diag.common.OutputFormatMixin;
import org.jboss.jws.diag.config.model.ServerConfig;
import org.jboss.jws.diag.config.parser.PropertyResolver;
import org.jboss.jws.diag.config.parser.ServerXmlParser;
import org.jboss.jws.diag.diff.formatter.DiffHumanFormatter;
import org.jboss.jws.diag.diff.formatter.DiffJsonFormatter;
import org.jboss.jws.diag.diff.formatter.MultiDiffHumanFormatter;
import org.jboss.jws.diag.diff.formatter.MultiDiffJsonFormatter;
import org.jboss.jws.diag.diff.model.DiffReport;
import org.jboss.jws.diag.diff.model.InstanceDiffResult;
import org.jboss.jws.diag.diff.model.MultiDiffReport;
import org.jboss.jws.diag.instances.InstanceScanner;
import org.jboss.jws.diag.instances.model.TomcatInstance;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Command(name = "diff",
        description = "Compare effective server.xml configuration between two CATALINA_BASE directories",
        mixinStandardHelpOptions = true)
public class DiffCommand implements Runnable {

    @Option(names = "--left",
            description = "Path to the first CATALINA_BASE directory (or its server.xml)")
    private Path left;

    @Option(names = "--right",
            description = "Path to the second CATALINA_BASE directory (or its server.xml)")
    private Path right;

    @Option(names = "--all",
            description = "Scan /proc for all running JWS instances and diff each against the lowest-PID instance")
    private boolean all;

    @Mixin
    private OutputFormatMixin outputFormat;

    @Override
    public void run() {
        if (all) {
            runMultiDiff();
        } else {
            if (left == null || right == null) {
                System.err.println("ERROR: --left and --right are required when --all is not specified");
                System.exit(ExitCodes.TOOL_FAILURE);
                return;
            }
            runSingleDiff();
        }
    }

    private void runSingleDiff() {
        Path leftXml = resolveServerXml("--left", left);
        Path rightXml = resolveServerXml("--right", right);
        if (leftXml == null || rightXml == null) {
            System.exit(ExitCodes.TOOL_FAILURE);
            return;
        }

        ServerConfig leftConfig = parse(leftXml, left);
        ServerConfig rightConfig = parse(rightXml, right);
        if (leftConfig == null || rightConfig == null) {
            System.exit(ExitCodes.TOOL_FAILURE);
            return;
        }

        DiffReport report = new ConfigDiffer().diff(left, right, leftConfig, rightConfig);

        String output;
        if (outputFormat.getFormat() == OutputFormat.JSON) {
            output = new DiffJsonFormatter().format(report);
        } else {
            output = new DiffHumanFormatter().format(report);
        }

        System.out.println(output);
        System.exit(report.hasDifferences() ? ExitCodes.WARNINGS : ExitCodes.OK);
    }

    private void runMultiDiff() {
        List<TomcatInstance> instances = new InstanceScanner().scan();

        if (instances.size() < 2) {
            System.err.println("ERROR: --all requires at least 2 running JWS instances; found "
                    + instances.size());
            System.exit(ExitCodes.TOOL_FAILURE);
            return;
        }

        TomcatInstance reference = instances.get(0);
        Path refBase = reference.getCatalinaBase();
        Path refXml = resolveServerXml("reference (PID " + reference.getPid() + ")", refBase);
        if (refXml == null) {
            System.exit(ExitCodes.TOOL_FAILURE);
            return;
        }
        ServerConfig refConfig = parse(refXml, refBase);
        if (refConfig == null) {
            System.exit(ExitCodes.TOOL_FAILURE);
            return;
        }

        ConfigDiffer differ = new ConfigDiffer();
        List<InstanceDiffResult> comparisons = new ArrayList<>();

        for (int i = 1; i < instances.size(); i++) {
            TomcatInstance inst = instances.get(i);
            Path instBase = inst.getCatalinaBase();
            Path instXml = resolveServerXml("PID " + inst.getPid(), instBase);
            if (instXml == null) {
                System.err.println("WARN: Skipping PID " + inst.getPid()
                        + ": server.xml not found under " + instBase);
                continue;
            }
            ServerConfig instConfig = parse(instXml, instBase);
            if (instConfig == null) {
                System.err.println("WARN: Skipping PID " + inst.getPid()
                        + ": failed to parse server.xml");
                continue;
            }

            DiffReport diff = differ.diff(refBase, instBase, refConfig, instConfig);
            comparisons.add(new InstanceDiffResult(inst.getPid(), instBase, diff));
        }

        MultiDiffReport report = new MultiDiffReport(
                reference.getPid(), refBase, instances.size(), comparisons);

        String output;
        if (outputFormat.getFormat() == OutputFormat.JSON) {
            output = new MultiDiffJsonFormatter().format(report);
        } else {
            output = new MultiDiffHumanFormatter().format(report);
        }

        System.out.println(output);

        int expected = instances.size() - 1;
        if (comparisons.isEmpty()) {
            System.err.println("ERROR: No instance could be compared against the reference ("
                    + expected + " candidate(s), all skipped).");
        }
        int resultCode = report.hasDifferences() ? ExitCodes.WARNINGS : ExitCodes.OK;
        System.exit(MultiInstanceExitCode.combine(resultCode, comparisons.size(), expected));
    }

    private Path resolveServerXml(String flag, Path path) {
        if (Files.isRegularFile(path)) {
            return path;
        }
        if (Files.isDirectory(path)) {
            Path xml = path.resolve("conf/server.xml");
            if (Files.exists(xml)) return xml;
            System.err.println("ERROR: server.xml not found under " + flag + ": " + path);
            return null;
        }
        System.err.println("ERROR: " + flag + " is not a directory or server.xml file: " + path);
        return null;
    }

    private ServerConfig parse(Path serverXml, Path base) {
        try {
            Path resolverBase;
            if (Files.isDirectory(base)) {
                resolverBase = base;
            } else {
                Path parent = base.getParent();
                resolverBase = (parent != null) ? parent.getParent() : null;
            }
            PropertyResolver resolver = PropertyResolver.create(resolverBase);
            return new ServerXmlParser(resolver).parse(serverXml);
        } catch (IOException e) {
            System.err.println("ERROR: Failed to parse " + serverXml + ": " + e.getMessage());
            return null;
        }
    }
}
