package org.jboss.jws.diag.validate;

import org.jboss.jws.diag.common.ExitCodes;
import org.jboss.jws.diag.common.MultiInstanceExitCode;
import org.jboss.jws.diag.common.OutputFormatMixin;
import org.jboss.jws.diag.instances.InstanceScanner;
import org.jboss.jws.diag.instances.model.TomcatInstance;
import org.jboss.jws.diag.validate.model.Finding;
import org.jboss.jws.diag.validate.model.InstanceValidationResult;
import org.jboss.jws.diag.validate.output.HumanReadableOutput;
import org.jboss.jws.diag.validate.output.JsonOutput;
import org.jboss.jws.diag.validate.output.MultiHumanReadableOutput;
import org.jboss.jws.diag.validate.output.MultiJsonOutput;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

@Command(name = "validate",
        description = "Run diagnostic rules against configuration and report findings (INFO/WARN/ERROR)",
        mixinStandardHelpOptions = true)
public class ValidateCommand implements Runnable {

    @CommandLine.Option(names = "--catalina-base", description = "Path to CATALINA_BASE (defaults to $CATALINA_BASE env var)")
    private Path catalinaBase;

    @CommandLine.Option(names = "--all",
            description = "Scan /proc for all running JWS instances and validate each one")
    private boolean all;

    @Mixin
    private OutputFormatMixin outputFormat;

    private Supplier<List<TomcatInstance>> instanceSource = () -> new InstanceScanner().scan();

    @Override
    public void run() {
        System.exit(execute());
    }

    public int execute() {
        if (all && catalinaBase != null) {
            System.err.println("[ERROR] --catalina-base cannot be combined with --all. "
                    + "Each instance is validated against its own CATALINA_BASE.");
            return ExitCodes.TOOL_FAILURE;
        }
        return all ? executeAll() : executeSingle();
    }

    private int executeSingle() {
        Path resolvedCatalinaBase;
        try {
            resolvedCatalinaBase = resolveCatalinaBase();
        } catch (IllegalStateException e) {
            System.err.println("[ERROR] " + e.getMessage());
            return ExitCodes.TOOL_FAILURE;
        }

        ValidationEngine validationEngine = new ValidationEngine();
        List<Finding> findings = validationEngine.validate(resolvedCatalinaBase);
        int exitCode = ExitCodeCalculator.determineExitCode(findings);

        switch (outputFormat.getFormat()) {
            case HUMAN: new HumanReadableOutput().print(findings); break;
            case JSON: new JsonOutput().print(findings, exitCode); break;
        }

        return exitCode;
    }

    private int executeAll() {
        List<TomcatInstance> instances = instanceSource.get();
        if (instances.isEmpty()) {
            System.err.println("[ERROR] --all found no running JWS instances");
            return ExitCodes.TOOL_FAILURE;
        }

        ValidationEngine engine = new ValidationEngine();
        List<InstanceValidationResult> results = new ArrayList<>();
        List<Finding> allFindings = new ArrayList<>();

        for (TomcatInstance inst : instances) {
            Path base = inst.getCatalinaBase() != null ? inst.getCatalinaBase() : inst.getCatalinaHome();
            if (base == null) {
                System.err.println("[WARN] Skipping PID " + inst.getPid() + ": no CATALINA_BASE");
                continue;
            }
            // Rules treat a missing server.xml as nothing to check. Reporting that as a
            // clean instance would be false, so it is a skip instead.
            Path serverXml = base.resolve("conf/server.xml");
            if (!Files.isRegularFile(serverXml) || !Files.isReadable(serverXml)) {
                System.err.println("[WARN] Skipping PID " + inst.getPid()
                        + ": server.xml not readable at " + serverXml);
                continue;
            }
            try {
                List<Finding> findings = engine.validate(base);
                results.add(new InstanceValidationResult(inst.getPid(), base, findings));
                allFindings.addAll(findings);
            } catch (RuntimeException e) {
                System.err.println("[WARN] Skipping PID " + inst.getPid()
                        + ": validation failed: " + e.getMessage());
            }
        }

        int exitCode = MultiInstanceExitCode.combine(
                ExitCodeCalculator.determineExitCode(allFindings), results.size(), instances.size());

        switch (outputFormat.getFormat()) {
            case HUMAN: new MultiHumanReadableOutput().print(instances.size(), results); break;
            case JSON: new MultiJsonOutput().print(instances.size(), results, exitCode); break;
        }

        if (results.isEmpty()) {
            System.err.println("[ERROR] No instance could be validated ("
                    + instances.size() + " discovered, all skipped).");
        }
        return exitCode;
    }

    // Visible for tests, so --all can run without scanning the real /proc.
    void setInstanceSource(Supplier<List<TomcatInstance>> instanceSource) {
        this.instanceSource = instanceSource;
    }

    private Path resolveCatalinaBase() {
        if (catalinaBase != null) {
            return catalinaBase;
        }

        String envValue = System.getenv("CATALINA_BASE");
        if (envValue == null || envValue.isBlank()) {
            throw new IllegalStateException(
                    "Could not determine CATALINA_BASE. "
                            + "Use --catalina-base, or set the CATALINA_BASE environment variable.");
        }

        return Paths.get(envValue);
    }
}
