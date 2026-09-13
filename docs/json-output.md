# JSON Output and Schema Versioning

Every command accepts `--format json`. This page describes what a consumer of that output can rely on.

## The `schemaVersion` field

Every JSON payload carries a top-level `schemaVersion`:

```json
{
  "schemaVersion": "1.0",
  ...
}
```

Each command versions its output independently. The payloads are unrelated, so a field added to `logs` does not move the version `summary` reports. A command's version covers both its single-instance output and its `--all` output.

| Command | Current version |
|---------|-----------------|
| `summary`, `summary --all` | `1.0` |
| `config`, `config --all` | `1.0` |
| `validate`, `validate --all` | `1.1` (`--all` output added in 1.1) |
| `diff`, `diff --all` | `1.0` |
| `logs`, `logs --all` | `1.0` |
| `modcluster` | `1.0` |
| `instances` | `1.0` |

The values live in one place in the source: `org.jboss.jws.diag.common.SchemaVersions`.

## What a version change means

Versions are `MAJOR.MINOR`.

**Minor bump** (`1.0` to `1.1`): fields were added. Existing fields keep their names, types and meaning. A consumer written against `1.0` keeps working.

**Major bump** (`1.x` to `2.0`): at least one existing field was removed, renamed, changed type, or changed meaning. A consumer written against `1.x` may break and should check the release notes.

## Rules for consumers

- **Ignore fields you do not recognise.** Minor versions add fields. A parser that rejects unknown fields will break on changes that are promised to be compatible.
- **Check the major version.** Reject or warn on a major version you were not written for.
- **Do not depend on field order.** Order is kept stable for readability but is not part of the contract.
- **Treat absent optional fields as unknown.** Many fields are omitted when discovery could not determine them, rather than emitted as `null`.
- **Paths always use forward slashes**, on every platform.

## Rules for contributors

`JsonSchemaContractTest` records the field paths each command produces in `src/test/resources/schema/`. If you change what a command emits, that test fails.

1. Decide the bump: major for removed, renamed or retyped fields, minor for added ones.
2. Update the constant in `SchemaVersions`.
3. Regenerate the fingerprints:

   ```bash
   mvn test -Dtest=JsonSchemaContractTest -Djws.schema.update=true
   ```

4. Commit the updated fingerprint with the change, so reviewers see the shape change in the diff.

Regeneration refuses to record a changed shape under an unchanged version, so the bump cannot be skipped by regenerating alone.

If you add an optional field to a model, also populate it in the test's fixtures. The fingerprint only sees fields the fixtures fill in.
