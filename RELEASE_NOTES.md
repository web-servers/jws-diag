# jws-diag v0.1.0

A read-only diagnostic and configuration analysis CLI for JBoss Web Server and Apache Tomcat. It helps SREs and support engineers inspect an installation and report configuration findings without changing the installation.

## Download and verify

Download `jws-diag-v0.1.0.jar` and `jws-diag-v0.1.0.jar.sha256` from the release assets. Verify the JAR before running it.

**Linux**

```sh
sha256sum -c jws-diag-v0.1.0.jar.sha256
```

**Windows PowerShell**

```powershell
$expected = (Get-Content .\jws-diag-v0.1.0.jar.sha256).Split(' ')[0]
$actual = (Get-FileHash .\jws-diag-v0.1.0.jar -Algorithm SHA256).Hash
if ($actual -ne $expected) { throw 'Checksum mismatch' }
'Checksum OK'
```

## Quick start

Requires JDK 11 or newer:

```sh
java -jar jws-diag-v0.1.0.jar --help
java -jar jws-diag-v0.1.0.jar summary
```

Use `--catalina-home` or `--catalina-base` to select a specific installation when needed.

## Commands

| Command | Purpose |
|---|---|
| `summary` | Show product versions, JVM, OS and container details, and native library status |
| `config` | Parse and display effective connector, TLS, proxy, and executor configuration |
| `validate` | Run diagnostic rules and report INFO, WARN, and ERROR findings |
| `bundle` | Create a redacted `.tar.gz` support bundle |
| `logs` | Scan Tomcat logs for known error patterns |
| `instances` | Detect running Tomcat/JWS instances through Linux `/proc` |
| `modcluster` | Show mod_cluster or mod_proxy_cluster configuration |
| `diff` | Compare `server.xml` configurations from two `CATALINA_BASE` directories |

All commands support human-readable output and JSON output with `--format`.

## Platform support

- **Linux:** `/proc`-based process and instance discovery is Linux-specific.
- **Windows 11:** `--help` and `summary` have been verified; other commands have not.
- JBoss Web Server and Apache Tomcat version coverage is listed below; the entries marked “Not verified” have not been tested for this release.

## Exit codes

| Code | Meaning |
|---|---|
| 0 | Completed successfully; no noteworthy findings |
| 1 | Completed with warnings, a non-empty diff, or a partial result |
| 2 | Completed with one or more error findings |
| 3 | The tool could not complete the requested check |

Known issue: `instances` currently returns `0` when `/proc` cannot be scanned; it should return `3`. See #26.

## Support matrix

| Product | Version | Status |
|---|---:|---|
| JBoss Web Server | 6 | Not verified |
| JBoss Web Server | 7 | Not verified |
| Apache Tomcat | 10.1 | Not verified |
| Apache Tomcat | 11 | Not verified; the bundled defaults are based on Tomcat 10.1.x |

## Known limitations

- Tomcat 11 compatibility has not been verified.
- The JSON `schemaVersion` field is duplicated across models and its meaning is not defined. Avoid relying on it in tooling. See #39.
- The project name may change before the first stable release. See #41.
