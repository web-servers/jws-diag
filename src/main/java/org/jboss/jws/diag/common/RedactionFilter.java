package org.jboss.jws.diag.common;

import java.util.Locale;

/**
 * Redacts sensitive attribute values to prevent credential leakage in diagnostic output.
 *
 * <p>{@code ${VAULT::...}} tokens are preserved as-is — they are already opaque and
 * do not expose the underlying secret.
 */
public final class RedactionFilter {

    public static final String REDACTED = "***REDACTED***";

    private RedactionFilter() {}

    /**
     * Returns {@link #REDACTED} if the attribute name indicates a sensitive value,
     * or the original {@code value} otherwise. {@code ${VAULT::...}} tokens are
     * always returned unchanged regardless of attribute name.
     */
    public static String redact(String attributeName, String value) {
        if (value == null) return null;
        if (value.startsWith("${VAULT::")) return value;
        if (isSensitive(attributeName)) return REDACTED;
        return value;
    }

    private static boolean isSensitive(String attrName) {
        String lower = attrName.toLowerCase(Locale.ROOT);
        return lower.contains("password")
                || lower.contains("secret")
                || lower.contains("credential")
                || lower.endsWith("pass");
    }
}
