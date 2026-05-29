package org.jboss.jws.diag.validate;

import java.nio.file.Path;

public final class RuleContext {
    private final Path catalinaBase;

    public RuleContext(Path catalinaBase) {
        this.catalinaBase = catalinaBase;
    }

    public Path getCatalinaBase() {
        return catalinaBase;
    }
}
