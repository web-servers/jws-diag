package org.jboss.jws.diag.validate.rules.security;

import org.jboss.jws.diag.validate.Rule;
import org.jboss.jws.diag.validate.RuleContext;
import org.jboss.jws.diag.validate.model.Finding;
import org.jboss.jws.diag.common.RuleId;
import org.jboss.jws.diag.common.Severity;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import java.util.List;
import java.util.Set;

public class UserDefaultCredentialsRule implements Rule {

    // Known default username/password pairs to check against
    private static final Set<String> DEFAULT_CREDENTIALS = Set.of(
            "tomcat:tomcat",
            "admin:admin",
            "admin:password",
            "admin:tomcat",
            "both:both"
    );

    @Override
    public List<Finding> evaluate(RuleContext ctx) {
        Document doc = ctx.getTomcatUsersXml();

        if (doc == null) {
            return List.of(Finding.builder()
                    .ruleId(RuleId.SEC_002)
                    .category("Security")
                    .severity(Severity.ERROR)
                    .summary("Default Credentials Detected")
                    .detail("Checks for known default username/password pairs (like tomcat/tomcat, admin/admin)")
                    .file("tomcat-users.xml")
                    .fix("Change the default passwords or remove the default accounts entirely")
                    .build());
        }

        NodeList users = doc.getElementsByTagName("user");

        for (int i = 0; i < users.getLength(); i++) {
            String username = users.item(i).getAttributes()
                    .getNamedItem("username") != null
                    ? users.item(i).getAttributes().getNamedItem("username").getNodeValue() : "";

            String password = users.item(i).getAttributes()
                    .getNamedItem("password") != null
                    ? users.item(i).getAttributes().getNamedItem("password").getNodeValue() : "";

            String pair = username + ":" + password;

            if (DEFAULT_CREDENTIALS.contains(pair)) {
                return List.of(Finding.builder()
                        .ruleId(RuleId.SEC_002)
                        .category("Security")
                        .severity(Severity.ERROR)
                        .summary("Default Credentials Detected")
                        .detail("Checks for known default username/password pairs (like tomcat/tomcat, admin/admin)")
                        .file("tomcat-users.xml")
                        .fix("Change the default passwords or remove the default accounts entirely")
                        .build());
            }
        }

        return List.of();
    }
}