package org.jboss.jws.diag.validate.model;

import org.jboss.jws.diag.common.Severity;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public final class Finding {

    private final String ruleId;
    private final String category;
    private final Severity severity;
    private final String summary;
    private final String detail;
    private final String file;
    private final String fix;

    private Finding(Builder builder) {
        this.ruleId = builder.ruleId;
        this.category = builder.category;
        this.severity = builder.severity;
        this.summary = builder.summary;
        this.detail = builder.detail;
        this.file = builder.file;
        this.fix = builder.fix;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private String ruleId;
        private String category;
        private Severity severity;
        private String summary;
        private String detail;
        private String file;
        private String fix;

        public Builder ruleId(String ruleId) {
            this.ruleId = ruleId;
            return this;
        }
        public Builder category(String category) {
            this.category = category;
            return this;
        }
        public Builder severity(Severity severity) {
            this.severity = severity;
            return this;
        }
        public Builder summary(String summary) {
            this.summary = summary;
            return this;
        }
        public Builder detail(String detail) {
            this.detail = detail;
            return this;
        }
        public Builder file(String file) {
            this.file = file;
            return this;
        }
        public Builder fix(String fix) {
            this.fix = fix;
            return this;
        }

        public Finding build() {
            return new Finding(this);
        }
    }

    public String getRuleId() {
        return ruleId;
    }
    public String getCategory() {
        return category;
    }
    public Severity getSeverity() {
        return severity;
    }
    public String getSummary() {
        return summary;
    }
    public String getDetail() {
        return detail;
    }
    public String getFile() {
        return file;
    }
    public String getFix() {
        return fix;
    }

    @Override
    public String toString() {
        return "Finding{" +
                "ruleId='" + ruleId + '\'' +
                ", category='" + category + '\'' +
                ", severity='" + severity + '\'' +
                ", summary='"  + summary  + '\'' +
                ", detail='"   + detail   + '\'' +
                ", file='"     + file     + '\'' +
                ", fix='"      + fix      + '\'' +
                '}';
    }
}