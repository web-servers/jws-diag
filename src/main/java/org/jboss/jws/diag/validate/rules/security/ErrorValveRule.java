package org.jboss.jws.diag.validate.rules.security;

import org.jboss.jws.diag.common.RuleId;
import org.jboss.jws.diag.common.Severity;
import org.jboss.jws.diag.validate.Rule;
import org.jboss.jws.diag.validate.RuleContext;
import org.jboss.jws.diag.validate.model.Finding;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import java.util.List;

public class ErrorValveRule implements Rule {

    @Override
    public List<Finding> evaluate(RuleContext ctx) {
        Document doc = ctx.getServerXml();

        if (doc == null) {
            return List.of();
        }

        NodeList valves = doc.getElementsByTagName("Valve");

        for (int i = 0; i < valves.getLength(); i++) {
            String className = valves.item(i).getAttributes().getNamedItem("className") != null
                    ? valves.item(i).getAttributes().getNamedItem("className").getNodeValue() : "";

            if ("org.apache.catalina.valves.ErrorReportValve".equals(className)) {
                String showReport = valves.item(i).getAttributes().getNamedItem("showReport") != null
                        ? valves.item(i).getAttributes().getNamedItem("showReport").getNodeValue() : "true";
                String showServerInfo = valves.item(i).getAttributes().getNamedItem("showServerInfo") != null
                        ? valves.item(i).getAttributes().getNamedItem("showServerInfo").getNodeValue() : "true";

                if ("false".equals(showReport) && "false".equals(showServerInfo)) {
                    return List.of();
                }
            }
        }

        return List.of(Finding.builder()
                .ruleId(RuleId.SEC_004)
                .category("Security")
                .severity(Severity.WARN)
                .summary("Version Banner Exposure Check")
                .detail("Checks if <Connector> elements expose server metadata, or if an <ErrorReportValve> is missing inside the <Host> or <Engine> blocks to suppress versions on error pages")
                .file("server.xml")
                .fix("Configure an <ErrorReportValve> with showReport=\"false\" and showServerInfo=\"false\" inside your Host block")
                .build());
    }
}
