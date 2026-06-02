package org.jboss.jws.diag.validate;

import org.jboss.jws.diag.validate.model.Finding;

import java.util.List;

public interface Rule {

    List<Finding> evaluate(RuleContext ctx);
}
