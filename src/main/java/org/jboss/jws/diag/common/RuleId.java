package org.jboss.jws.diag.common;

import com.fasterxml.jackson.annotation.JsonValue;

public enum RuleId {
    SEC_001("SEC-001"),
    SEC_002("SEC-002"),
    SEC_003("SEC-003"),
    SEC_004("SEC-004"),
    SEC_005("SEC-005"),
    SEC_006("SEC-006"),

    TLS_001("TLS-001"),
    TLS_002("TLS-002"),
    TLS_003("TLS-003"),
    TLS_004("TLS-004"),
    TLS_005("TLS-005"),
    TLS_006("TLS-006");

    private final String id;

    RuleId(String id) {
        this.id = id;
    }

    @JsonValue
    public String getId() {
        return id;
    }

    @Override
    public String toString() {
        return id;
    }
}
