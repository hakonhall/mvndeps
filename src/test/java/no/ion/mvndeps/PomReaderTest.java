package no.ion.mvndeps;

import no.ion.mvndeps.maven.MavenCoordinate;
import no.ion.mvndeps.maven.MavenDependency;
import no.ion.mvndeps.maven.MavenModule;
import no.ion.mvndeps.maven.MavenProject;
import no.ion.mvndeps.maven.MavenProjectReader;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Comparator;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PomReaderTest {
    private final Path vespaPath = Path.of(".");
    private final MavenProject project = MavenProjectReader.readProjectDirectory(vespaPath);

    @Test
    void printMavenModuleCoordinates() {
        Set<String> modules = project.modules()
                                     .stream()
                                     .map(MavenModule::coordinate)
                                     .map(MavenCoordinate::toString)
                                     .collect(Collectors.toSet());
        assertEquals(Set.of("no.ion:mvndeps:1.0-SNAPSHOT"), modules);
    }

    @Test
    void printDependencies() {
        Set<String> dependencies = project.modules().stream()
                                     .flatMap(module -> module.getDependencies().stream())
                                     .map(MavenDependency::toString)
                                     .collect(Collectors.toSet());
        assertEquals(Set.of("org.apache.maven:maven-model-builder:3.8.1",
                                    "org.apache.maven:maven-resolver-provider:3.8.1",
                                    "org.junit.jupiter:junit-jupiter:5.8.1"),
                     dependencies);
    }

    @Test
    void printDependencyGraph() {
        Set<String> dependencies = project.modules().stream().
                                     sorted(Comparator.comparing(m -> m.coordinate().toString()))
                                     .map(module -> module
                                             .getDependencies()
                                             .stream()
                                             .filter(dependency -> dependency.groupId().startsWith("com.yahoo.vespa"))
                                             .map(dependency -> dependency.groupId() + ":" + dependency.artifactId())
                                             .sorted()
                                             .distinct()
                                             .collect(Collectors.joining(",", module.coordinate() + " [", "]")))
                                     .collect(Collectors.toSet());
        assertEquals(Set.of("no.ion:mvndeps:1.0-SNAPSHOT []"), dependencies);
    }
}