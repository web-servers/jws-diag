package org.jboss.jws.diag.common;

/**
 * JSON schema version for each command's {@code --format json} output.
 *
 * <p>Each command versions independently, because the payloads are unrelated: a field
 * added to {@code logs} says nothing about {@code summary}. A command's version covers
 * both its single-instance output and its {@code --all} output.
 *
 * <p>Bump the major number when a field is removed, renamed, or changes type or meaning.
 * Bump the minor number when a field is added. Consumers must ignore fields they do not
 * recognise. See {@code docs/json-output.md}.
 *
 * <p>{@code JsonSchemaContractTest} fails when a command's JSON field set changes, so a
 * shape change cannot land without someone deciding whether the version moves.
 */
public final class SchemaVersions {

    public static final String SUMMARY = "1.0";
    public static final String CONFIG = "1.0";
    // 1.1: added validate --all output.
    public static final String VALIDATE = "1.1";
    public static final String DIFF = "1.0";
    public static final String LOGS = "1.0";
    public static final String MODCLUSTER = "1.0";
    public static final String INSTANCES = "1.0";

    private SchemaVersions() {
    }
}
