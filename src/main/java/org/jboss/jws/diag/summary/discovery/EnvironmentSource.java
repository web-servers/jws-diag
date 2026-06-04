package org.jboss.jws.diag.summary.discovery;

/**
 * Abstraction over {@link System#getenv(String)} to allow injection in tests.
 */
@FunctionalInterface
interface EnvironmentSource {

    String getenv(String name);
}
