package org.jboss.jws.diag.validate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jboss.jws.diag.common.ExitCodes;
import org.jboss.jws.diag.instances.model.TomcatInstance;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ValidateAllCommandTest {

    private static final String SERVER_XML =
            "<Server port=\"-1\" shutdown=\"SHUTDOWN\"><Service name=\"Catalina\"/></Server>";

    private final ByteArrayOutputStream stdout = new ByteArrayOutputStream();
    private PrintStream originalOut;

    @TempDir
    Path root;

    @BeforeEach
    void captureStdout() {
        originalOut = System.out;
        System.setOut(new PrintStream(stdout, true, StandardCharsets.UTF_8));
    }

    @AfterEach
    void restoreStdout() {
        System.setOut(originalOut);
    }

    @Test
    void allCombinedWithCatalinaBase_isToolFailure() {
        ValidateCommand command = command(List.of(), "--all", "--catalina-base", root.toString());

        assertThat(command.execute()).isEqualTo(ExitCodes.TOOL_FAILURE);
    }

    @Test
    void noRunningInstances_isToolFailure() {
        assertThat(command(List.of(), "--all").execute()).isEqualTo(ExitCodes.TOOL_FAILURE);
    }

    @Test
    void everyInstanceValidated_reportsEachInJson() throws IOException {
        Path a = base("a");
        Path b = base("b");

        int exitCode = command(List.of(instance(100, a), instance(200, b)), "--all", "--format", "JSON").execute();

        JsonNode json = new ObjectMapper().readTree(stdout.toString(StandardCharsets.UTF_8));
        assertThat(exitCode).isIn(ExitCodes.OK, ExitCodes.WARNINGS, ExitCodes.ERRORS);
        assertThat(json.get("instanceCount").asInt()).isEqualTo(2);
        assertThat(json.get("instances")).hasSize(2);
        assertThat(json.get("instances").get(0).get("pid").asInt()).isEqualTo(100);
        assertThat(json.get("instances").get(1).get("pid").asInt()).isEqualTo(200);
        assertThat(json.get("exitCode").asInt()).isEqualTo(exitCode);
    }

    @Test
    void instanceWithoutServerXml_isSkippedAndRunIsAtLeastWarning() throws IOException {
        Path valid = base("valid");
        Path empty = Files.createDirectories(root.resolve("empty"));

        int exitCode = command(List.of(instance(100, valid), instance(200, empty)),
                "--all", "--format", "JSON").execute();

        JsonNode json = new ObjectMapper().readTree(stdout.toString(StandardCharsets.UTF_8));
        assertThat(json.get("instanceCount").asInt()).isEqualTo(2);
        assertThat(json.get("instances")).hasSize(1);
        assertThat(exitCode).isGreaterThanOrEqualTo(ExitCodes.WARNINGS).isNotEqualTo(ExitCodes.TOOL_FAILURE);
    }

    @Test
    void everyInstanceSkipped_isToolFailure() throws IOException {
        Path empty = Files.createDirectories(root.resolve("empty"));

        assertThat(command(List.of(instance(100, empty)), "--all").execute())
                .isEqualTo(ExitCodes.TOOL_FAILURE);
    }

    @Test
    void humanOutput_showsEachInstanceAndOverallLine() throws IOException {
        Path a = base("a");

        command(List.of(instance(100, a)), "--all").execute();

        String out = stdout.toString(StandardCharsets.UTF_8);
        assertThat(out).contains("Validated 1 of 1 instance(s).");
        assertThat(out).contains("Instance PID 100");
        assertThat(out).contains(a.toString().replace('\\', '/'));
        assertThat(out).contains("Overall:");
    }

    private ValidateCommand command(List<TomcatInstance> instances, String... args) {
        ValidateCommand command = new ValidateCommand();
        new CommandLine(command).parseArgs(args);
        command.setInstanceSource(() -> instances);
        return command;
    }

    private Path base(String name) throws IOException {
        Path conf = Files.createDirectories(root.resolve(name).resolve("conf"));
        Files.writeString(conf.resolve("server.xml"), SERVER_XML, StandardCharsets.UTF_8);
        return root.resolve(name);
    }

    private static TomcatInstance instance(int pid, Path base) {
        return new TomcatInstance(pid, base, base);
    }
}
