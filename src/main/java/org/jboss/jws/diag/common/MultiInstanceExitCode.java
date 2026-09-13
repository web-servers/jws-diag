package org.jboss.jws.diag.common;

/**
 * Folds skipped instances into the exit code of an {@code --all} run, so every
 * multi-instance command treats partial and total failure the same way.
 *
 * <ul>
 *   <li>No instance could be processed: {@link ExitCodes#TOOL_FAILURE}. Nothing was
 *       examined, so reporting a clean result would be false.</li>
 *   <li>Some instances were skipped: at least {@link ExitCodes#WARNINGS}, because the
 *       result is incomplete. A higher code from the processed instances still wins.</li>
 *   <li>Every instance was processed: the command's own result, unchanged.</li>
 * </ul>
 */
public final class MultiInstanceExitCode {

    private MultiInstanceExitCode() {
    }

    /**
     * @param resultCode exit code derived from the instances that were processed
     * @param processed  number of instances that produced a result
     * @param expected   number of instances the command tried to process
     */
    public static int combine(int resultCode, int processed, int expected) {
        if (processed == 0) {
            return ExitCodes.TOOL_FAILURE;
        }
        if (processed < expected && resultCode < ExitCodes.WARNINGS) {
            return ExitCodes.WARNINGS;
        }
        return resultCode;
    }
}
