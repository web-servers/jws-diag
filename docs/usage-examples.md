# Usage Examples

Practical examples of `jws-diag summary` and `jws-diag config` for common
support and diagnostic scenarios.

---

## Building and installing

```bash
mvn package -q
alias jws-diag='java -jar target/jws-diag-0.1.0-SNAPSHOT.jar'
```

---

## `jws-diag summary`

### Bare-metal JWS installation (auto-discovered)

```
$ jws-diag summary
Tomcat 10.1.49 | JWS 6.1.0
CATALINA_HOME: /opt/rh/jws6/root/usr/share/tomcat
CATALINA_BASE: /opt/rh/jws6/root/etc/tomcat
JVM: 17.0.10 (Red Hat, Inc.) | OS: RHEL 9.3 (x86_64)
Container: none (bare metal / VM)
Native: APR 1.7.2 | OpenSSL 3.0.9
PID: 12345
```

### Podman container with explicit path

```
$ jws-diag summary --catalina-home /deployments/tomcat
Tomcat 10.1.35
CATALINA_HOME: /deployments/tomcat
CATALINA_BASE: /deployments/tomcat
JVM: 21.0.3 (Eclipse Adoptium) | OS: Ubuntu 22.04 (amd64)
Container: Podman (detected via /run/.containerenv)
Native: not configured
PID: 1
```

### JSON output (for CI / automation)

```bash
jws-diag summary --format json
```

```json
{
  "schemaVersion": "1.0",
  "tomcatVersion": "10.1.49",
  "jwsVersion": "6.1.0",
  "catalinaHome": "/opt/rh/jws6/root/usr/share/tomcat",
  "catalinaBase": "/opt/rh/jws6/root/etc/tomcat",
  "jvm": {
    "version": "17.0.10",
    "vendor": "Red Hat, Inc.",
    "jvmArgs": ["-Xmx512m", "-Djava.awt.headless=true"]
  },
  "os": { "name": "RHEL", "version": "9.3", "arch": "x86_64" },
  "container": { "type": "BARE_METAL" },
  "nativeLib": { "aprVersion": "1.7.2", "opensslVersion": "3.0.9" },
  "pid": 12345
}
```

---

## `jws-diag config`

### Standard HTTP + AJP setup

```
$ jws-diag config
Connector :8080 [HTTP/1.1]
  protocol: HTTP/1.1 (explicit)
  sslEnabled: false (default)
  maxThreads: 200 (default)
  connectionTimeout: 20000 (explicit)
  maxConnections: 8192 (default)
  compression: off (default)
Connector :8009 [AJP/1.3]
  protocol: AJP/1.3 (explicit)
  sslEnabled: false (default)
  maxThreads: 200 (default)
  connectionTimeout: 60000 (default)
  maxConnections: 8192 (default)
  secretRequired: true (default)
Host: localhost
  appBase: webapps (explicit)
  autoDeploy: true (explicit)
  unpackWARs: true (explicit)
```

### TLS-enabled connector with keystore

```
$ jws-diag config
Connector :8443 [HTTP/1.1] ★ SSL
  protocol: HTTP/1.1 (explicit)
  sslEnabled: true (explicit)
  maxThreads: 150 (explicit)
  connectionTimeout: 60000 (default)
  maxConnections: 8192 (default)
  SSLHostConfig:
    protocols: TLSv1.2+TLSv1.3
    Certificate:
      keystoreFile: conf/localhost-rsa.jks
      keystoreType: JKS (default)
      keystorePass: ***REDACTED***
```

Passwords are always redacted. VAULT tokens are preserved verbatim:

```
      keystorePass: ${VAULT::ssl::keystorePassword::1}
```

### Shared executor with connector reference

```
$ jws-diag config
Executor "tomcatThreadPool"
  maxThreads: 150 (explicit)
  minSpareThreads: 4 (explicit)
  threadPriority: 5 (default)
  maxIdleTime: 60000 (default)
  namePrefix: catalina-exec-
Connector :8080 [HTTP/1.1]
  ...
  executor: tomcatThreadPool
```

### Proxy-aware connector with RemoteIpValve

```
Connector :8080 [HTTP/1.1]
  ...
  proxyName: example.com
  proxyPort: 80
Host: localhost
  ...
  Valve: RemoteIP [org.apache.catalina.valves.RemoteIpValve]
    remoteIpHeader: X-Forwarded-For
    protocolHeader: X-Forwarded-Proto
  Valve: AccessLog [org.apache.catalina.valves.AccessLogValve]
    directory: logs
    pattern: %h %l %u %t "%r" %s %b
```

Known valve types (`AccessLog`, `RemoteIP`, `StuckThreadDetection`, `ErrorReport`)
are shown with a readable label. Unknown valve classes show the raw `className`.

### Multi-service configuration

```
$ jws-diag config
Service: Catalina
  Connector :8080 [HTTP/1.1]
    ...
  Host: localhost
    ...
Service: Admin
  Connector :9080 [HTTP/1.1]
    ...
  Host: admin.localhost
    ...
```

### JSON output for automation

```bash
jws-diag config --format json | jq '.services[0].connectors[0].maxThreads'
# {"value":200,"explicit":false}

jws-diag config --format json | jq '.services[0].executors[0]'
# {
#   "name": "tomcatThreadPool",
#   "maxThreads": {"value":150,"explicit":true},
#   "minSpareThreads": {"value":4,"explicit":true},
#   "threadPriority": {"value":5,"explicit":false},
#   "maxIdleTime": {"value":60000,"explicit":false}
# }
```

The `"explicit": false` flag lets you distinguish operator-set values from
Tomcat defaults in CI checks or support scripts.

### Extracting all explicit TLS settings

```bash
jws-diag config --format json \
  | jq '[.services[].connectors[] | select(.sslEnabled.value == true)]'
```

---

## Common support workflows

### Check if AJP secret is required

```bash
jws-diag config --format json \
  | jq '.services[].connectors[] | select(.protocol.value | test("AJP")) | .secretRequired'
```

If `null` is returned the connector has no `secretRequired` configured (defaults to `true` for AJP).

### Verify no passwords in output

```bash
# Should print nothing if redaction is working
jws-diag config --format json | grep -v VAULT | grep -i password
```

### Compare two installations

```bash
diff \
  <(jws-diag config --catalina-home /opt/tomcat-prod --format json) \
  <(jws-diag config --catalina-home /opt/tomcat-staging --format json)
```

### Validate every instance on a host

```bash
jws-diag validate --all
```

Each running instance found in `/proc` is validated against its own `CATALINA_BASE`.
An instance whose `conf/server.xml` cannot be read is skipped with a warning on stderr.
The exit code is the highest severity across all instances, at least `1` if any were
skipped, and `3` if none could be validated.

List only the instances with errors:

```bash
jws-diag validate --all --format json \
  | jq '.instances[] | select(.summary.errors > 0) | {pid, catalinaBase}'
```

### Check which defaults differ from Tomcat baseline

```bash
jws-diag config --format json \
  | jq '[.. | objects | select(has("explicit") and .explicit == true)]'
```

This returns every attribute that was explicitly set in `server.xml`, making it easy
to see what was changed from the Tomcat defaults.
