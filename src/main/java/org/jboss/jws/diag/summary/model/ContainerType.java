package org.jboss.jws.diag.summary.model;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Container runtime in which the Tomcat process is running.
 */
public enum ContainerType {

    DOCKER("docker"),
    PODMAN("podman"),
    KUBERNETES("kubernetes"),
    BARE_METAL("bare_metal");

    private final String value;

    ContainerType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }
}
