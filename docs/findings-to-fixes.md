# Findings-to-Fixes Guide

## Overview

The `jws-diag validate` command analyzes a JBoss Web Server (JWS) or Apache Tomcat installation and reports configuration findings categorized as `INFO`, `WARN`, or `ERROR`.

The `jws-diag bundle` command includes these validation results in the generated support bundle as `validation-results.json`, making the findings available for offline analysis and support investigations.

This guide maps each validation rule to its purpose, affected configuration, and recommended resolution. After applying the suggested changes, rerun the validation command to verify that the finding has been resolved.

## Running Validation

Run validation against the current Tomcat installation. By default, validation findings are displayed in a human-readable format.

```bash
jws-diag validate
```

Specify an explicit `CATALINA_BASE`:

```bash
jws-diag validate --catalina-base /path/to/tomcat
```

Generate JSON output for automation, CI pipelines, or integration with external tools.

```bash
jws-diag validate --format JSON
```

## Example Validation Output

The following examples demonstrate the human-readable and JSON output formats produced by the `jws-diag validate` command.

### Human-readable output

```text
--- ERROR ------------------------------------------------------------------------------------------

  [TLS-002] TLS Certificate Expiry
     Detail : Could not load keystore conf/localhost-rsa.jks: catalina-base\conf\localhost-rsa.jks
     File   : conf/localhost-rsa.jks
     Fix    : Verify the keystore file exists, the path is correct, and the password is valid

--- WARN -------------------------------------------------------------------------------------------

  [CONN-003] Connector Proxy Mismatch
     Detail : proxyName (api.company.internal) is defined but proxyPort is missing
     File   : server.xml
     Fix    : Define the proxyPort for the created proxyName

Summary: 2 error(s), 2 warning(s), 0 info(s)
```

The human-readable output groups findings by severity and displays the rule ID, summary, detail, affected file, and recommended fix. The final summary provides the count of findings for each severity level.

### JSON output

```json
{
  "findings": [
    {
      "ruleId": "TLS-002",
      "category": "TLS",
      "severity": "ERROR",
      "summary": "Certificate Expiry",
      "detail": "Could not load keystore conf/localhost-rsa.jks: catalina-base\\conf\\localhost-rsa.jks",
      "file": "conf/localhost-rsa.jks",
      "fix": "Verify the keystore file exists, the path is correct, and the password is valid"
    }
  ],
  "summary": {
    "errors": 2,
    "warnings": 2,
    "info": 0
  },
  "exitCode": 2
}
```

The JSON output contains the validation findings, a severity summary, and the resulting exit code.

- **`findings`** - Contains the individual validation findings. Each finding identifies the rule that was triggered and provides its category, severity, summary, detailed explanation, affected file, and recommended fix.
- **`summary`** - Provides the total number of findings for each severity level: `ERROR`, `WARN`, and `INFO`.
- **`exitCode`** - Indicates the overall validation result based on the highest severity found.

## Severity Levels

Validation findings are reported using one of three severity levels. These levels indicate the relative importance of the finding and help prioritize remediation efforts.

| Severity | Description                                                                                                  | Recommended Action                                                                   |
|----------|--------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------|
| `INFO`   | Informational observation about the current configuration that does not indicate a problem.                  | Review if applicable. Changes are optional depending on the deployment requirements. |
| `WARN`   | Configuration may not follow recommended best practices or could lead to potential issues.                   | Review and update the configuration where appropriate.                               |
| `ERROR`  | Configuration issue requiring immediate attention that may impact security, stability, or correct operation. | Resolve the issue and rerun `jws-diag validate` before deploying to production.      |

## Exit Codes

The `jws-diag validate` command uses the following exit codes:

| Exit Code | Meaning  | Condition                                                                    |
|:---------:|:--------:|------------------------------------------------------------------------------|
|    `0`    |    OK    | No `ERROR` or `WARN` findings. `INFO` findings do not affect the exit code.  |
|    `1`    | Warnings | One or more `WARN` findings are present and no `ERROR` findings are present. |
|    `2`    |  Errors  | One or more `ERROR` findings are present.                                    |

When multiple severity levels are present, the exit code reflects the highest severity found. For example, if the validation result contains both warnings and errors, the command returns exit code `2`.

These exit codes can be used by scripts and CI pipelines to determine whether validation completed without errors, produced warnings, or detected errors.

## Validation Rules

The validation engine uses a systematic naming convention to uniquely identify validation rules across command-line output, JSON reports, documentation, and unit tests.

The following rule categories are currently implemented:

- **SEC** – Security validation rules
- **TLS** – Transport Layer Security (TLS) validation rules
- **CONN** – Connector configuration validation rules

The table below summarizes each implemented validation rule, its purpose, affected configuration, and the recommended resolution.

| Rule ID  | Category  | Severity |                  Summary                   | Detail                                                                                                                                                                          |        Source File         | Recommended Resolution                                                                                                                |
|:--------:|:---------:|:--------:|:------------------------------------------:|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|:--------------------------:|---------------------------------------------------------------------------------------------------------------------------------------|
| SEC-001  | Security  |  ERROR   |              Root User Check               | Checks whether the running Tomcat process for this installation has effective UID 0, read from `/proc/<pid>/status`. The user running jws-diag does not matter. Skipped when this installation is not running or `/proc` is unavailable. |       Process State        | Run Tomcat as a dedicated, non-root system user.                                                                                      |
| SEC-002  | Security  |  ERROR   |        Default Credentials Detected        | Checks for known default username/password pairs (like `tomcat/tomcat`, `admin/admin`).                                                                                         |     `tomcat-users.xml`     | Change the default passwords or remove the default accounts entirely.                                                                 |
| SEC-003  | Security  |  ERROR   |     Shutdown Port Configuration Check      | Inspects the `<Server>` element's `port` attribute to ensure it is set to `"-1"`. It flags an issue if it is missing or set to a standard network port (like `8005`).           |        `server.xml`        | Set the `port` attribute of the `<Server>` element to `-1` to disable network-based shutdown.                                         |
| SEC-004  | Security  |   WARN   |       Version Banner Exposure Check        | Checks if `<Connector>` elements expose server metadata, or if an `<ErrorReportValve>` is missing inside the `<Host>` or `<Engine>` blocks to suppress versions on error pages. |        `server.xml`        | Configure an `<ErrorReportValve>` with `showReport="false"` and `showServerInfo="false"` inside your `Host` block.                    |
| SEC-005  | Security  |   WARN   |             HTTP TRACE Enabled             | Checks if the HTTP TRACE method is allowed, which can leave it open to tracing attacks.                                                                                         |        `server.xml`        | Set `allowTrace="false"` on your active connectors.                                                                                   |
| SEC-006  | Security  |   INFO   |             Localhost Binding              | Checks if the connector `address` attribute is restricted to localhost (`127.0.0.1`).                                                                                           |        `server.xml`        | If you want the server accessible publicly, change the `address` to 0.0.0.0 .                                                         |
| SEC-007  | Security  |   INFO   | Stuck Thread Detection Valve Configuration | Checks whether a `StuckThreadDetectionValve` is configured and validates its relevant configuration including `threshold` and `interruptThreadThreshold`.                       |        `server.xml`        | Set `threshold` higher than the Container's `backgroundProcessorDelay`, and set `interruptThreadThreshold` to `-1` or >= `threshold`. |
| SEC-008  | Security  |   WARN   |       Access Log Valve Configuration       | Checks whether an `AccessLogValve` is configured and validates its relevant logging configuration.                                                                              |        `server.xml`        | Configure the `AccessLogValve` with appropriate logging, rotation, retention, and output settings.                                    |
| SEC-009  | Security  |   WARN   |     Garbage Collection Log Diagnostics     | Scans active GC logs for basic operational warning indicators.                                                                                                                  | GC log files under `logs/` | Investigate repeated Full GC, allocation failures, excessive GC pauses, or memory exhaustion indicators.                              |
| TLS-001  |    TLS    |   WARN   |            Deprecated Protocols            | Checks if obsolete TLS versions (`SSLv2`, `SSLv3`, `TLSv1.0`, `TLSv1.1`) are enabled.                                                                                           |        `server.xml`        | Update the configuration to allow only modern TLS versions such as TLS 1.2 or TLS 1.3.                                                |
| TLS-002  |    TLS    |  ERROR   |             Certificate Expiry             | Checks if the configured SSL/TLS certificate has expired based on the system date.                                                                                              |       Keystore File        | Renew and install a valid SSL/TLS certificate immediately.                                                                            |
| TLS-003  |    TLS    |   WARN   |            Missing Secure Flag             | Checks if a connector has `SSLEnabled="true"` but is missing the `secure="true"` flag.                                                                                          |        `server.xml`        | Add `secure="true"` to the connector configuration.                                                                                   |
| TLS-004  |    TLS    |   WARN   |           Missing SSLHostConfig            | Checks if an HTTPS connector is missing a modern nested `<SSLHostConfig>` block.                                                                                                |        `server.xml`        | Move inline SSL configuration attributes into a defined `<SSLHostConfig>` block.                                                      |
| TLS-005  |    TLS    |  ERROR   |             Bad Keystore Path              | Checks if the path to the SSL keystore file actually exists on the system layout.                                                                                               |        `server.xml`        | Correct the keystore file path attribute to point to a valid file.                                                                    |
| TLS-006  |    TLS    |   WARN   |             Weak Cipher Suites             | Checks for weak or obsolete encryption ciphers within the connector configurations.                                                                                             |        `server.xml`        | Configure the connector to use strong cipher suites such as AES-GCM.                                                                  |
| TLS-007  |    TLS    |   WARN   |       Certificate Chain Verification       | Checks whether the configured keystore contains the required certificate chain for the server certificate.                                                                      |       Keystore File        | Bundle the missing intermediate CA certificate(s) into the keystore alongside the end-entity certificate.                             |
| CONN-001 | Connector |   WARN   |             Low Threads Check              | Compares `maxThreads` against available CPU cores rather than using a rigid static number.                                                                                      |        `server.xml`        | Adjust `maxThreads` upward to match your host hardware specifications.                                                                |
| CONN-002 | Connector |  ERROR   |               Port Conflict                | Verifies whether multiple distinct connectors are trying to bind to the exact same port number.                                                                                 |        `server.xml`        | Assign unique, non-overlapping port numbers to each connector block.                                                                  |
| CONN-003 | Connector |   WARN   |               Proxy Mismatch               | Checks if `proxyName` is defined but `proxyPort` is missing from the configuration (or vice versa).                                                                             |        `server.xml`        | Configure both `proxyName` and `proxyPort` together when using a reverse proxy or load balancer.                                      |
| CONN-004 | Connector |   INFO   |           Missing Redirect Port            | Inspects whether standard HTTP connectors omit the `redirectPort` attribute.                                                                                                    |        `server.xml`        | Add `redirectPort="8443"` to allow automatic HTTPS redirection.                                                                       |
| CONN-005 | Connector |   WARN   |           Obsolete APR Connector           | Checks for APR connector settings when running on modern Tomcat 10.1+ servers.                                                                                                  |        `server.xml`        | Remove legacy APR tags and transition to the recommended NIO or NIO2 connector implementation.                                        |
| CONN-006 | Connector |   WARN   |          Port Availability Check           | Checks if a configured port is already used by another process.                                                                                                                 |       Process State        | Change the Tomcat port in `server.xml` or stop the process using the port.                                                            |

## Verifying the Resolution

After applying the recommended configuration changes, rerun the validation command to confirm that the reported findings have been resolved.

```bash
jws-diag validate
```

If the issue has been successfully remediated, the corresponding validation rule will no longer appear in the output. If findings are still reported, review the associated configuration and ensure the recommended resolution has been fully applied.

## Support Bundles

When additional troubleshooting or support is required, generate a **redacted support bundle** using:

```bash
jws-diag bundle
```

The generated support bundle automatically includes the validation results as `validation-results.json`, along with the collected and redacted configuration files. This enables the validation findings to be shared with Red Hat Support or other administrators without rerunning the validation command.

## Notes

- Validation rules are read-only and never modify the Tomcat installation.
- Some findings depend on the current runtime environment (for example, process state or certificate validity).
- Multiple findings may be reported for the same configuration file.