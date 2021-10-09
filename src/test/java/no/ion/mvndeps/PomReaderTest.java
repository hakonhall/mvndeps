package no.ion.mvndeps;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Collectors;

class PomReaderTest {
    private final Path vespaPath = Path.of("/home/hakon/local/github/vespa-engine/vespa");
    private final MavenProject project = MavenProjectReader.readProjectDirectory(vespaPath);

    @Test
    void printMavenModuleCoordinates() {
        project.modules().stream()
                .map(module -> module.to4CoordinateString())
                .sorted()
                .forEach(System.out::println);
    }

    @Test
    void printDependencies() {
        project.modules().stream()
                .flatMap(module -> module.getDependencies().stream())
                .map(MavenDependency::toString)
                .sorted()
                .distinct()
                .forEach(System.out::println);
    }

    @Test
    void printVespaDependencyGraph() {
        project.modules().stream().
                sorted(Comparator.comparing(module -> module.to4CoordinateString()))
                .forEach(module -> {
                    System.out.println(module.to4CoordinateString() + ": " +
                    module.getDependencies().stream()
                            .filter(dependency -> dependency.groupId().startsWith("com.yahoo.vespa"))
                            .map(dependency -> dependency.groupId() + ":" + dependency.artifactId())
                            .sorted()
                            .distinct()
                            .collect(Collectors.toList()));
        });
    }
}