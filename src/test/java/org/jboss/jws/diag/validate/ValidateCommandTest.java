package org.jboss.jws.diag.validate;

import org.jboss.jws.diag.common.ExitCodes;
import org.jboss.jws.diag.common.RuleId;
import org.jboss.jws.diag.common.Severity;

import org.jboss.jws.diag.validate.model.Finding;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class ValidateCommandTest {

    private final ValidateCommand validateCommand = new ValidateCommand();

    @Test
    void shouldReturnOkWhenNoFindingsArePresent() {
        int result = validateCommand.determineExitCode(Collections.emptyList());
        assertThat(result).isEqualTo(ExitCodes.OK);
    }

    @Test
    void shouldReturnWarningWhenFindingsContainOnlyWarnings() {
        List<Finding> findings = List.of(
                Finding.builder()
                        .ruleId(RuleId.SEC_004)
                        .category("Security")
                        .severity(Severity.WARN)
                        .summary("Version Banner Exposure Check")
                        .detail("Checks if <Connector> elements expose server metadata, or if an <ErrorInfoValve> is missing inside the <Host> or <Engine> blocks to suppress versions on error pages")
                        .file("server.xml")
                        .fix("Configure an <ErrorInfoValve> with showReport=\"false\" and showServerInfo=\"false\" inside your Host block")
                        .build()
        );

        int result = validateCommand.determineExitCode(findings);
        assertThat(result).isEqualTo(ExitCodes.WARNINGS);
    }

    @Test
    void shouldReturnErrorWhenFindingsContainsOnlyErrors() {
        List<Finding> findings = List.of(
                Finding.builder()
                        .ruleId(RuleId.SEC_001)
                        .category("Security")
                        .severity(Severity.ERROR)
                        .summary("Root User Check")
                        .detail("Checks if the Tomcat process is running as root (UID 0)")
                        .file("Process State")
                        .fix("Run Tomcat as a dedicated, non-root system user")
                        .build()
        );

        int result = validateCommand.determineExitCode(findings);
        assertThat(result).isEqualTo(ExitCodes.ERRORS);
    }

    @Test
    void shouldReturnErrorWhenFindingsContainWarnAndError() {
        List<Finding> findings = List.of(
                Finding.builder()
                        .ruleId(RuleId.SEC_004)
                        .category("Security")
                        .severity(Severity.WARN)
                        .summary("Version Banner Exposure Check")
                        .detail("Checks if <Connector> elements expose server metadata, or if an <ErrorInfoValve> is missing inside the <Host> or <Engine> blocks to suppress versions on error pages")
                        .file("server.xml")
                        .fix("Configure an <ErrorInfoValve> with showReport=\"false\" and showServerInfo=\"false\" inside your Host block")
                        .build(),
                Finding.builder()
                        .ruleId(RuleId.SEC_001)
                        .category("Security")
                        .severity(Severity.ERROR)
                        .summary("Root User Check")
                        .detail("Checks if the Tomcat process is running as root (UID 0)")
                        .file("Process State")
                        .fix("Run Tomcat as a dedicated, non-root system user")
                        .build()
        );

        int result = validateCommand.determineExitCode(findings);
        assertThat(result).isEqualTo(ExitCodes.ERRORS);
    }

    @Test
    void shouldReturnErrorWhenFindingsContainErrorAndWarn() {
        List<Finding> findings = List.of(
                Finding.builder()
                        .ruleId(RuleId.SEC_001)
                        .category("Security")
                        .severity(Severity.ERROR)
                        .summary("Root User Check")
                        .detail("Checks if the Tomcat process is running as root (UID 0)")
                        .file("Process State")
                        .fix("Run Tomcat as a dedicated, non-root system user")
                        .build(),
                Finding.builder()
                        .ruleId(RuleId.SEC_004)
                        .category("Security")
                        .severity(Severity.WARN)
                        .summary("Version Banner Exposure Check")
                        .detail("Checks if <Connector> elements expose server metadata, or if an <ErrorInfoValve> is missing inside the <Host> or <Engine> blocks to suppress versions on error pages")
                        .file("server.xml")
                        .fix("Configure an <ErrorInfoValve> with showReport=\"false\" and showServerInfo=\"false\" inside your Host block")
                        .build()
        );

        int result = validateCommand.determineExitCode(findings);
        assertThat(result).isEqualTo(ExitCodes.ERRORS);
    }

    @Test
    void shouldReturnOkWhenFindingsContainOnlyInfo() {
        List<Finding> findings = List.of(
                Finding.builder()
                        .ruleId(RuleId.CONN_004)
                        .category("Connector")
                        .severity(Severity.INFO)
                        .summary("Missing Redirect Port")
                        .detail("Inspects whether standard HTTP connectors omit the redirectPort attribute.")
                        .file("server.xml")
                        .fix("Add redirectPort=\"8443\" to allow automatic HTTPS redirection fields.")
                        .build()
        );

        int result = validateCommand.determineExitCode(findings);
        assertThat(result).isEqualTo(ExitCodes.OK);
    }

    @Test
    void shouldReturnWarningWhenFindingsContainInfoAndWarn() {
        List<Finding> findings = List.of(
                Finding.builder()
                        .ruleId(RuleId.CONN_004)
                        .category("Connector")
                        .severity(Severity.INFO)
                        .summary("Missing Redirect Port")
                        .detail("Inspects whether standard HTTP connectors omit the redirectPort attribute.")
                        .file("server.xml")
                        .fix("Add redirectPort=\"8443\" to allow automatic HTTPS redirection fields.")
                        .build(),
                Finding.builder()
                        .ruleId(RuleId.SEC_004)
                        .category("Security")
                        .severity(Severity.WARN)
                        .summary("Version Banner Exposure Check")
                        .detail("Checks if <Connector> elements expose server metadata, or if an <ErrorInfoValve> is missing inside the <Host> or <Engine> blocks to suppress versions on error pages")
                        .file("server.xml")
                        .fix("Configure an <ErrorInfoValve> with showReport=\"false\" and showServerInfo=\"false\" inside your Host block")
                        .build()
        );

        int result = validateCommand.determineExitCode(findings);
        assertThat(result).isEqualTo(ExitCodes.WARNINGS);
    }

    @Test
    void shouldReturnWarningWhenFindingsContainWarnAndInfo() {
        List<Finding> findings = List.of(
                Finding.builder()
                        .ruleId(RuleId.SEC_004)
                        .category("Security")
                        .severity(Severity.WARN)
                        .summary("Version Banner Exposure Check")
                        .detail("Checks if <Connector> elements expose server metadata, or if an <ErrorInfoValve> is missing inside the <Host> or <Engine> blocks to suppress versions on error pages")
                        .file("server.xml")
                        .fix("Configure an <ErrorInfoValve> with showReport=\"false\" and showServerInfo=\"false\" inside your Host block")
                        .build(),
                Finding.builder()
                        .ruleId(RuleId.CONN_004)
                        .category("Connector")
                        .severity(Severity.INFO)
                        .summary("Missing Redirect Port")
                        .detail("Inspects whether standard HTTP connectors omit the redirectPort attribute.")
                        .file("server.xml")
                        .fix("Add redirectPort=\"8443\" to allow automatic HTTPS redirection fields.")
                        .build()
        );

        int result = validateCommand.determineExitCode(findings);
        assertThat(result).isEqualTo(ExitCodes.WARNINGS);
    }

    @Test
    void shouldReturnErrorWhenFindingsContainInfoAndError() {
        List<Finding> findings = List.of(
                Finding.builder()
                        .ruleId(RuleId.CONN_004)
                        .category("Connector")
                        .severity(Severity.INFO)
                        .summary("Missing Redirect Port")
                        .detail("Inspects whether standard HTTP connectors omit the redirectPort attribute.")
                        .file("server.xml")
                        .fix("Add redirectPort=\"8443\" to allow automatic HTTPS redirection fields.")
                        .build(),
                Finding.builder()
                        .ruleId(RuleId.CONN_004)
                        .category("Security")
                        .severity(Severity.ERROR)
                        .summary("Root User Check")
                        .detail("Checks if the Tomcat process is running as root (UID 0)")
                        .file("Process State")
                        .fix("Run Tomcat as a dedicated, non-root system user")
                        .build()
        );

        int result = validateCommand.determineExitCode(findings);
        assertThat(result).isEqualTo(ExitCodes.ERRORS);
    }

    @Test
    void shouldReturnErrorWhenFindingsContainErrorAndInfo() {
        List<Finding> findings = List.of(
                Finding.builder()
                        .ruleId(RuleId.SEC_004)
                        .category("Security")
                        .severity(Severity.ERROR)
                        .summary("Root User Check")
                        .detail("Checks if the Tomcat process is running as root (UID 0)")
                        .file("Process State")
                        .fix("Run Tomcat as a dedicated, non-root system user")
                        .build(),
                Finding.builder()
                        .ruleId(RuleId.CONN_004)
                        .category("Connector")
                        .severity(Severity.INFO)
                        .summary("Missing Redirect Port")
                        .detail("Inspects whether standard HTTP connectors omit the redirectPort attribute.")
                        .file("server.xml")
                        .fix("Add redirectPort=\"8443\" to allow automatic HTTPS redirection fields.")
                        .build()
        );

        int result = validateCommand.determineExitCode(findings);
        assertThat(result).isEqualTo(ExitCodes.ERRORS);
    }
}
