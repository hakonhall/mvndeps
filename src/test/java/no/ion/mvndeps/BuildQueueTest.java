package no.ion.mvndeps;

import org.junit.jupiter.api.Test;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

class BuildQueueTest {
    private final Path vespaPath = Path.of("/home/hakon/local/github/vespa-engine/vespa");
    private final MavenProject project = MavenProjectReader.readProjectDirectory(vespaPath);
    private final BuildQueue queue = BuildQueue.make(project);

    @Test
    void testBuildOrder() {
        do {
            Iterator<MavenModule> iterator = queue.readyToBuild().iterator();
            if (!iterator.hasNext()) {
                break;
            }

            MavenModule module = iterator.next();
            System.out.println("Building " + module.coordinate().toString());

            queue.remove(module.coordinate());
        } while (true);
    }

    //@Test
    void testBatchBuild() {
        new Main(FileSystems.getDefault(), "-p", vespaPath.toString(), "-d", "/home/hakon/tmp/vespa-deps/build.dot").doMain();
    }

    @Test
    void testBatchCompileBuild() {
        BuildQueue queue = BuildQueue.makeCompilation(project);
        for (int round = 1; !queue.isDone(); ++round) {
            System.out.println("build round " + round + ":");
            queue.readyToBuild().stream()
                    .sorted(Comparator.comparing(module -> toNodeId(module)))
                    .forEach(module -> {
                        queue.remove(module.coordinate());
                        System.out.println("  " + toNodeId(module));
                    });
            System.out.println();
        }
    }

    @Test
    void dotDependencyGraph() {
        System.out.println("digraph \"Vespa build order\" {");

        List<MavenModule> modules = project.modules()
                .stream()
                .sorted(Comparator.comparing(module -> module.coordinate().toString()))
                .collect(Collectors.toList());

        modules.forEach(module -> System.out.println("  " + toNodeId(module)));

        BuildQueue queue = BuildQueue.make(project);
        queue.readyToBuild().stream()
                .map(BuildQueueTest::toNodeId)
                .sorted()
                .forEach(moduleString -> System.out.println("  init -> " + moduleString));

        modules.forEach(module -> {
            module.getDependencies().stream()
                    .filter(dependency -> project.artifactIsInProject(dependency.getArtifact()))
                    .map(BuildQueueTest::toNodeId)
                    .sorted()
                    .forEach(dependency -> System.out.println("  " + dependency + " -> " + toNodeId(module)));
        });

        System.out.println("}");
    }

    @Test
    void dotCompileDependencyGraph() {
        System.out.println("digraph \"Vespa compile order\" {");

        List<MavenModule> modules = project.modules()
                .stream()
                .sorted(Comparator.comparing(module -> module.coordinate().toString()))
                .collect(Collectors.toList());

        modules.forEach(module -> System.out.println("  " + toNodeId(module)));

        BuildQueue queue = BuildQueue.makeCompilation(project);
        queue.readyToBuild().stream()
                .map(BuildQueueTest::toNodeId)
                .sorted()
                .forEach(moduleString -> System.out.println("  init -> " + moduleString));

        modules.forEach(module -> {
            module.getDependencies().stream()
                    .filter(dependency -> project.artifactIsInProject(dependency.getArtifact()))
                    .map(BuildQueueTest::toNodeId)
                    .sorted()
                    .forEach(dependency -> System.out.println("  " + dependency + " -> " + toNodeId(module)));
        });

        System.out.println("}");
    }

    private static String toNodeId(MavenModule module) { return toNodeId(module.coordinate().getGroupArtifact()); }
    private static String toNodeId(MavenDependency dependency) { return toNodeId(dependency.getArtifact()); }
    private static String toNodeId(ArtifactId artifactId) { return "\"" + artifactId.toString() + "\""; }
}