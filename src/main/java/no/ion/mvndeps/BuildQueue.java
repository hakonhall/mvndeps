package no.ion.mvndeps;

import no.ion.mvndeps.maven.BadPomException;
import no.ion.mvndeps.maven.MavenDependency;
import no.ion.mvndeps.maven.MavenModule;
import no.ion.mvndeps.maven.MavenProject;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class BuildQueue {
    private final Map<MavenCoordinate, BuildDependencyNode> nodes;
    private final Set<MavenCoordinate> nodesReadyToBuild;

    static BuildQueue makeCompilation(MavenProject project) {
        Map<MavenCoordinate, BuildDependencyNode> nodes = new HashMap<>();
        Set<MavenCoordinate> nodesReadyToBuild = new HashSet<>();
        List<MavenModule> modules = project.modules();

        for (MavenModule module : modules) {
            var node = new BuildDependencyNode(module);
            MavenCoordinate coordinate = node.module().coordinate();
            nodes.put(coordinate, node);
            nodesReadyToBuild.add(coordinate);
        }

        for (MavenModule module : modules) {
            var node = nodes.get(module.coordinate());

            for (MavenDependency dependency : module.getPreTestDependencies()) {
                String groupId = dependency.groupId();
                String artifactId = dependency.artifactId();
                if (!project.artifactIsInProject(groupId, artifactId)) {
                    continue;
                }

                String version = dependency.resolvedVersion();

                MavenCoordinate dependencyCoordinate = new MavenCoordinate(groupId, artifactId, version);

                BuildDependencyNode dependencyNode = nodes.get(dependencyCoordinate);
                if (dependencyNode == null) {
                    // Situation:  There is a POM file declaring an artifact with the dependencyCoordinate's
                    // groupId and artifactId, however the version in <dependency>  in 'module' does not match that.
                    throw new BadPomException(module.modulePath(), "dependency not found: " + dependencyCoordinate);
                }

                // 'node' depends on 'dependencyNode'

                node.addBuildDependency(dependencyNode);
                dependencyNode.isABuildDependencyOf(node);
                nodesReadyToBuild.remove(node.module().coordinate());
            }
        }

        return new BuildQueue(nodes, nodesReadyToBuild);
    }

    public static BuildQueue make(MavenProject project) {
        var nodes = new HashMap<MavenCoordinate, BuildDependencyNode>();
        var nodesReadyToBuild = new HashSet<MavenCoordinate>();

        List<MavenModule> modules = project.modules();

        for (MavenModule module : modules) {
            var node = new BuildDependencyNode(module);
            MavenCoordinate coordinate = node.module().coordinate();
            nodes.put(coordinate, node);
            nodesReadyToBuild.add(coordinate);
        }

        for (MavenModule module : modules) {
            var node = nodes.get(module.coordinate());
            module.parent().ifPresent(parentModule -> {
                addDependency(parentModule.coordinate(), nodes, node, module, nodesReadyToBuild);
            });

            for (MavenDependency dependency : module.getDependencies()) {
                addDependency(dependency, project, nodes, node, module, nodesReadyToBuild);
            }

            // TODO: Parse plugins
            if (module.coordinate().equals(new MavenCoordinate("com.yahoo.vespa", "documentgen-test", "7-SNAPSHOT"))) {
                var coordinate = new MavenCoordinate("com.yahoo.vespa", "vespa-documentgen-plugin", "7-SNAPSHOT");
                addDependency(coordinate, nodes, node, module, nodesReadyToBuild);
            }
        }

        return new BuildQueue(nodes, nodesReadyToBuild);
    }

    private static void addDependency(MavenDependency dependency, MavenProject project,
                                      HashMap<MavenCoordinate, BuildDependencyNode> nodes, BuildDependencyNode node,
                                      MavenModule module, HashSet<MavenCoordinate> nodesReadyToBuild) {
        String groupId = dependency.groupId();
        String artifactId = dependency.artifactId();
        if (!project.artifactIsInProject(groupId, artifactId)) {
            return;
        }

        String version = dependency.resolvedVersion();

        var dependencyCoordinate = new MavenCoordinate(groupId, artifactId, version);

        addDependency(dependencyCoordinate, nodes, node, module, nodesReadyToBuild);
    }

    private static void addDependency(MavenCoordinate dependencyCoordinate,
                                      HashMap<MavenCoordinate, BuildDependencyNode> nodes, BuildDependencyNode node,
                                      MavenModule module, HashSet<MavenCoordinate> nodesReadyToBuild) {
        BuildDependencyNode dependencyNode = nodes.get(dependencyCoordinate);
        if (dependencyNode == null) {
            // Situation:  There is a POM file declaring an artifact with the dependencyCoordinate's
            // groupId and artifactId, however the version in <dependency>  in 'module' does not match that.
            throw new BadPomException(module.modulePath(), "dependency not found: " + dependencyCoordinate);
        }

        // 'node' depends on 'dependencyNode'

        node.addBuildDependency(dependencyNode);
        dependencyNode.isABuildDependencyOf(node);
        nodesReadyToBuild.remove(node.module().coordinate());
    }

    private BuildQueue(Map<MavenCoordinate, BuildDependencyNode> nodes, Set<MavenCoordinate> nodesReadyToBuild) {
        this.nodes = nodes;
        this.nodesReadyToBuild = nodesReadyToBuild;
    }

    public boolean isDone() { return nodesReadyToBuild.isEmpty(); }

    public List<MavenModule> readyToBuild() {
        return nodesReadyToBuild.stream()
                .map(nodes::get)
                .sorted(Comparator.comparing((BuildDependencyNode node) -> node.buildsDependingOnThis().size()).reversed())
                .map(node -> node.module())
                .collect(Collectors.toList());
    }

    public List<BuildDependencyNode> readyToBuild2() {
        return nodesReadyToBuild.stream()
                                .map(nodes::get)
                                .sorted(Comparator.comparing((BuildDependencyNode node) -> node.buildsDependingOnThis().size()).reversed())
                                .collect(Collectors.toList());
    }

    /** Remove module from the build queue. */
    public void remove(MavenCoordinate coordinate) {
        BuildDependencyNode node = nodes.get(coordinate);
        if (node == null) {
            throw new IllegalArgumentException("there is no such artifact in the build queue: " + coordinate);
        }

        List<BuildDependencyNode> dependers = node.buildsDependingOnThis();
        for (var dependerNode : dependers) {
            boolean isNowReadyToBeBuilt = dependerNode.notifyOfBuildCompletion(coordinate);
            if (isNowReadyToBeBuilt) {
                nodesReadyToBuild.add(dependerNode.module().coordinate());
            }
        }

        nodesReadyToBuild.remove(coordinate);
    }
}
