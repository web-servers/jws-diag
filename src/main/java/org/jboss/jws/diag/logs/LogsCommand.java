package org.jboss.jws.diag.logs;

import org.jboss.jws.diag.common.ExitCodes;
import org.jboss.jws.diag.common.OutputFormat;
import org.jboss.jws.diag.common.OutputFormatMixin;
import org.jboss.jws.diag.instances.InstanceScanner;
import org.jboss.jws.diag.instances.model.TomcatInstance;
import org.jboss.jws.diag.logs.formatter.LogsHumanFormatter;
import org.jboss.jws.diag.logs.formatter.LogsJsonFormatter;
import org.jboss.jws.diag.logs.formatter.MultiLogsHumanFormatter;
import org.jboss.jws.diag.logs.formatter.MultiLogsJsonFormatter;
import org.jboss.jws.diag.logs.model.InstanceLogResult;
import org.jboss.jws.diag.logs.model.LogScanResult;
import org.jboss.jws.diag.logs.model.MultiLogReport;
import org.jboss.jws.diag.summary.discovery.CatalinaDiscovery;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Command(name = "logs",
        description = "Scan Tomcat log files for known error patterns (OOM, BindException, StuckThread, etc.)",
        mixinStandardHelpOptions = true)
public class LogsCommand implements Runnable {

    @Option(names = "--log-file",
            description = "Path to log file to scan (default: CATALINA_BASE/logs/catalina.out)")
    private Path logFile;

    @Option(names = "--catalina-home", description = "Path to CATALINA_HOME (overrides auto-detection)")
    private Path catalinaHome;

    @Option(names = "--catalina-base", description = "Path to CATALINA_BASE (defaults to CATALINA_HOME)")
    private Path catalinaBase;

    @Option(names = "--all",
            description = "Auto-discover all running JWS instances and scan each instance's log file")
    private boolean all;

    @Mixin
    private OutputFormatMixin outputFormat;

    @Override
    public void run() {
        if (all && logFile != null) {
            System.err.println("ERROR: --log-file cannot be combined with --all. "
                    + "Each instance's log file is resolved from its own CATALINA_BASE.");
            System.exit(ExitCodes.TOOL_FAILURE);
            return;
        }
        if (all) {
            runMulti();
        } else {
            runSingle();
        }
    }

    private void runSingle() {
        Path target = resolveLogFile();
        if (target == null) {
            System.err.println("ERROR: Could not determine log file path. "
                    + "Use --log-file or --catalina-home/--catalina-base.");
            System.exit(ExitCodes.TOOL_FAILURE);
            return;
        }
        if (!Files.exists(target)) {
            System.err.println("ERROR: Log file not found: " + target);
            System.exit(ExitCodes.TOOL_FAILURE);
            return;
        }

        LogScanResult result;
        try {
            result = new LogScanner().scan(target);
        } catch (IOException e) {
            System.err.println("ERROR: Failed to scan log file: " + e.getMessage());
            System.exit(ExitCodes.TOOL_FAILURE);
            return;
        }

        switch (outputFormat.getFormat()) {
            case HUMAN:
                System.out.println(new LogsHumanFormatter().format(result));
                break;
            case JSON:
                System.out.println(new LogsJsonFormatter().format(result));
                break;
        }

        System.exit(LogsExitCodeCalculator.determineExitCode(result));
    }

    private void runMulti() {
        List<TomcatInstance> instances = new InstanceScanner().scan();
        if (instances.isEmpty()) {
            System.err.println("ERROR: No running JWS instances found via /proc.");
            System.exit(ExitCodes.TOOL_FAILURE);
            return;
        }

        LogScanner scanner = new LogScanner();
        List<InstanceLogResult> results = new ArrayList<>();

        for (TomcatInstance inst : instances) {
            Path base = inst.getCatalinaBase() != null ? inst.getCatalinaBase() : inst.getCatalinaHome();
            if (base == null) {
                System.err.println("WARN: Skipping PID " + inst.getPid() + ": no CATALINA_BASE");
                continue;
            }
            LogFileResolver.Resolution resolution = LogFileResolver.resolve(base, inst.getCatalinaOut());
            if (!resolution.isFound()) {
                System.err.println("WARN: Skipping PID " + inst.getPid() + ": " + resolution.getSkipReason());
                continue;
            }
            Path logPath = resolution.getLogFile();
            try {
                LogScanResult scanResult = scanner.scan(logPath);
                results.add(new InstanceLogResult(inst.getPid(), logPath, scanResult));
            } catch (IOException e) {
                System.err.println("WARN: Skipping PID " + inst.getPid()
                        + ": failed to scan log: " + e.getMessage());
            }
        }

        MultiLogReport report = new MultiLogReport(instances.size(), results);

        String output;
        if (outputFormat.getFormat() == OutputFormat.JSON) {
            output = new MultiLogsJsonFormatter().format(report);
        } else {
            output = new MultiLogsHumanFormatter().format(report);
        }
        System.out.println(output);

        if (results.isEmpty()) {
            System.err.println("ERROR: No instance logs could be scanned ("
                    + instances.size() + " instance(s) discovered, all skipped).");
        }

        System.exit(LogsExitCodeCalculator.determineMultiExitCode(results, instances.size()));
    }

    private Path resolveLogFile() {
        if (logFile != null) {
            return logFile;
        }
        Path base = resolveBase();
        return base != null ? base.resolve("logs/catalina.out") : null;
    }

    private Path resolveBase() {
        if (catalinaBase != null) return catalinaBase;
        if (catalinaHome != null) return catalinaHome;
        CatalinaDiscovery.Result result = CatalinaDiscovery.create(null, null).discover();
        return result.getCatalinaBase() != null ? result.getCatalinaBase() : result.getCatalinaHome();
    }

}
