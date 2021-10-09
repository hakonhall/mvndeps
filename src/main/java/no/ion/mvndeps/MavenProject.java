package no.ion.mvndeps;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MavenProject {
    private final Path projectDirectory;
    private final Map<ArtifactId, Map<String, MavenModule>> modules;

    MavenProject(Path projectDirectory, List<MavenModule> modules) {
        this.projectDirectory = projectDirectory;
        this.modules = modules.stream()
                .collect(Collectors.groupingBy(
                        module -> module.coordinate().getGroupArtifact(),
                        Collectors.toMap(
                                module -> module.packaging(),
                                module -> module)));
    }

    public List<MavenModule> modules() {
        return modules.values().stream().flatMap(m -> m.values().stream()).collect(Collectors.toList());
    }

    boolean artifactIsInProject(ArtifactId artifactId) { return modules.containsKey(artifactId); }
    boolean artifactIsInProject(String groupId, String artifactId) {
        return modules.containsKey(new ArtifactId(groupId, artifactId));
    }

    public MavenModule getMavenModule(MavenCoordinate coordinate) {
        MavenModule module = modules.getOrDefault(coordinate.getGroupArtifact(), Map.of()).get(coordinate.version());
        if (module == null) {
            throw new IllegalStateException("Unknown module: " + coordinate.toString());
        }

        return module;
    }
}
