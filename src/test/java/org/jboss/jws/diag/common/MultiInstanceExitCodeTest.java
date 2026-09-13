package org.jboss.jws.diag.common;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MultiInstanceExitCodeTest {

    @Test
    void nothingProcessed_isToolFailure() {
        assertThat(MultiInstanceExitCode.combine(ExitCodes.OK, 0, 3)).isEqualTo(ExitCodes.TOOL_FAILURE);
    }

    @Test
    void nothingProcessed_isToolFailureEvenIfResultLooksBad() {
        assertThat(MultiInstanceExitCode.combine(ExitCodes.ERRORS, 0, 3)).isEqualTo(ExitCodes.TOOL_FAILURE);
    }

    @Test
    void allProcessed_keepsResult() {
        assertThat(MultiInstanceExitCode.combine(ExitCodes.OK, 3, 3)).isEqualTo(ExitCodes.OK);
        assertThat(MultiInstanceExitCode.combine(ExitCodes.WARNINGS, 3, 3)).isEqualTo(ExitCodes.WARNINGS);
        assertThat(MultiInstanceExitCode.combine(ExitCodes.ERRORS, 3, 3)).isEqualTo(ExitCodes.ERRORS);
    }

    @Test
    void someSkippedWithCleanResult_isWarning() {
        assertThat(MultiInstanceExitCode.combine(ExitCodes.OK, 2, 3)).isEqualTo(ExitCodes.WARNINGS);
    }

    @Test
    void someSkippedWithErrorResult_keepsError() {
        assertThat(MultiInstanceExitCode.combine(ExitCodes.ERRORS, 2, 3)).isEqualTo(ExitCodes.ERRORS);
    }
}
