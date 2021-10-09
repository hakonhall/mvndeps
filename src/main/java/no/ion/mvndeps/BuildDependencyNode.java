package no.ion.mvndeps;

import no.ion.mvndeps.maven.MavenModule;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A Maven module may be built once all dependencies have been built.
 */
public class BuildDependencyNode {
    private final MavenModule module;
    private final Set<MavenCoordinate> dependencies = new HashSet<>();
    private final List<BuildDependencyNode> dependers = new ArrayList<>();

    private Duration totalElapsedTime = Duration.ZERO;
    private Duration elapsedTime = Duration.ZERO;

    BuildDependencyNode(MavenModule module) {
        this.module = module;
    }

    public MavenModule module() { return module; }
    public List<MavenCoordinate> dependencies() { return List.copyOf(dependencies); }

    void addBuildDependency(BuildDependencyNode dependency) { dependencies.add(dependency.module.coordinate()); }
    void isABuildDependencyOf(BuildDependencyNode depender) { dependers.add(depender); }

    boolean readyToBuild() { return dependencies.isEmpty(); }

    boolean notifyOfBuildCompletion(MavenCoordinate dependency) {
        dependencies.remove(dependency);
        return readyToBuild();
    }

    List<BuildDependencyNode> buildsDependingOnThis() { return List.copyOf(dependers); }
}
