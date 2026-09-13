package org.jboss.jws.diag.logs;

import org.jboss.jws.diag.common.ExitCodes;
import org.jboss.jws.diag.logs.model.InstanceLogResult;
import org.jboss.jws.diag.logs.model.LogPattern;
import org.jboss.jws.diag.logs.model.LogScanResult;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class LogsExitCodeCalculatorTest {

    private static final Path LOG = Path.of("/opt/jws-6.0/standalone/logs/catalina.out");

    @Test
    void singleScan_withNoMatches_returnsOk() {
        int code = LogsExitCodeCalculator.determineExitCode(scan());

        assertThat(code).isEqualTo(ExitCodes.OK);
    }

    @Test
    void singleScan_withWarnMatch_returnsWarnings() {
        int code = LogsExitCodeCalculator.determineExitCode(scan(LogPattern.STUCK_THREAD));

        assertThat(code).isEqualTo(ExitCodes.WARNINGS);
    }

    @Test
    void singleScan_withErrorMatch_returnsErrors() {
        int code = LogsExitCodeCalculator.determineExitCode(scan(LogPattern.OOM));

        assertThat(code).isEqualTo(ExitCodes.ERRORS);
    }

    @Test
    void singleScan_withWarnAndErrorMatches_returnsErrors() {
        int code = LogsExitCodeCalculator.determineExitCode(
                scan(LogPattern.STUCK_THREAD, LogPattern.OOM));

        assertThat(code).isEqualTo(ExitCodes.ERRORS);
    }

    @Test
    void singleScan_withInfoMatchOnly_returnsOk() {
        int code = LogsExitCodeCalculator.determineExitCode(scan(LogPattern.CLASS_NOT_FOUND));

        assertThat(code).isEqualTo(ExitCodes.OK);
    }

    @Test
    void multiScan_whenEveryInstanceWasSkipped_returnsToolFailure() {
        int code = LogsExitCodeCalculator.determineMultiExitCode(List.of(), 3);

        assertThat(code).isEqualTo(ExitCodes.TOOL_FAILURE);
    }

    @Test
    void multiScan_whenSomeInstancesWereSkipped_returnsWarnings() {
        List<InstanceLogResult> results = List.of(instance(1234, scan()));

        int code = LogsExitCodeCalculator.determineMultiExitCode(results, 3);

        assertThat(code).isEqualTo(ExitCodes.WARNINGS);
    }

    @Test
    void multiScan_whenAllInstancesScannedCleanly_returnsOk() {
        List<InstanceLogResult> results = List.of(
                instance(1234, scan()),
                instance(5678, scan()));

        int code = LogsExitCodeCalculator.determineMultiExitCode(results, 2);

        assertThat(code).isEqualTo(ExitCodes.OK);
    }

    @Test
    void multiScan_withWarnMatchInOneInstance_returnsWarnings() {
        List<InstanceLogResult> results = List.of(
                instance(1234, scan()),
                instance(5678, scan(LogPattern.STUCK_THREAD)));

        int code = LogsExitCodeCalculator.determineMultiExitCode(results, 2);

        assertThat(code).isEqualTo(ExitCodes.WARNINGS);
    }

    @Test
    void multiScan_withErrorMatchInOneInstance_returnsErrors() {
        List<InstanceLogResult> results = List.of(
                instance(1234, scan(LogPattern.STUCK_THREAD)),
                instance(5678, scan(LogPattern.BIND_EXCEPTION)));

        int code = LogsExitCodeCalculator.determineMultiExitCode(results, 2);

        assertThat(code).isEqualTo(ExitCodes.ERRORS);
    }

    @Test
    void multiScan_errorMatchOutranksSkippedInstances() {
        List<InstanceLogResult> results = List.of(instance(1234, scan(LogPattern.OOM)));

        int code = LogsExitCodeCalculator.determineMultiExitCode(results, 4);

        assertThat(code).isEqualTo(ExitCodes.ERRORS);
    }

    private static InstanceLogResult instance(int pid, LogScanResult result) {
        return new InstanceLogResult(pid, LOG, result);
    }

    private static LogScanResult scan(LogPattern... matched) {
        Map<LogPattern, Integer> counts = new EnumMap<>(LogPattern.class);
        for (LogPattern pattern : matched) {
            counts.put(pattern, 1);
        }
        return new LogScanResult(LOG, 100L, counts, new EnumMap<>(LogPattern.class));
    }
}
