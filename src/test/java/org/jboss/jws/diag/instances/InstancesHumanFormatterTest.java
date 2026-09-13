package org.jboss.jws.diag.instances;

import org.jboss.jws.diag.instances.formatter.InstancesHumanFormatter;
import org.jboss.jws.diag.instances.model.TomcatInstance;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class InstancesHumanFormatterTest {

    private final InstancesHumanFormatter formatter = new InstancesHumanFormatter();

    @Test
    void emptyList_showsZeroAndNoProcessesMessage() {
        String out = formatter.format(Collections.emptyList());
        assertThat(out).contains("0");
        assertThat(out).contains("No running Tomcat");
    }

    @Test
    void singleInstance_showsPidAndPaths() {
        Path home = Path.of("/opt/tomcat");
        TomcatInstance inst = new TomcatInstance(12345, home, Path.of("/opt/tomcat/base"));
        String out = formatter.format(List.of(inst));

        assertThat(out).contains("12345");
        assertThat(out).contains(home.toString().replace('\\', '/'));
        assertThat(out).contains("CATALINA_HOME");
        assertThat(out).contains("CATALINA_BASE");
    }

    @Test
    void multipleInstances_allShown() {
        List<TomcatInstance> instances = List.of(
                new TomcatInstance(100, Path.of("/a"), Path.of("/a")),
                new TomcatInstance(200, Path.of("/b"), Path.of("/b"))
        );
        String out = formatter.format(instances);

        assertThat(out).contains("100");
        assertThat(out).contains("200");
        assertThat(out).contains("2");
    }

    @Test
    void nullPaths_showUnknown() {
        TomcatInstance inst = new TomcatInstance(9999, null, null);
        String out = formatter.format(List.of(inst));

        assertThat(out).contains("(unknown)");
    }
}
