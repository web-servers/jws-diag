package org.jboss.jws.diag.common;

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

    CONN_001("CONN-001"),
    CONN_002("CONN-002"),
    CONN_003("CONN-003"),
    CONN_004("CONN-004"),
    CONN_005("CONN-005");

    private final String id;

    RuleId(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    @Override
    public String toString() {
        return id;
    }
}
