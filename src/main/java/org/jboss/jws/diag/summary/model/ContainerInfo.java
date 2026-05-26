package org.jboss.jws.diag.summary.model;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Container runtime detected for the running Tomcat process.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class ContainerInfo {

    private final ContainerType type;
    private final String detectionMethod;

    private ContainerInfo(Builder builder) {
        this.type = builder.type;
        this.detectionMethod = builder.detectionMethod;
    }

    public ContainerType getType() {
        return type;
    }

    public String getDetectionMethod() {
        return detectionMethod;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private ContainerType type;
        private String detectionMethod;

        public Builder type(ContainerType type) {
            this.type = type;
            return this;
        }

        public Builder detectionMethod(String detectionMethod) {
            this.detectionMethod = detectionMethod;
            return this;
        }

        public ContainerInfo build() {
            return new ContainerInfo(this);
        }
    }

    @Override
    public String toString() {
        return "ContainerInfo{type=" + type + ", detectionMethod='" + detectionMethod + "'}";
    }
}
