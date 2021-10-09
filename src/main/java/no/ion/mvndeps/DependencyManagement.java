package no.ion.mvndeps;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class DependencyManagement {
    private final Map<String, Map<String, String>> versions = new HashMap<>();

    static DependencyManagement of(Model model) {
        org.apache.maven.model.DependencyManagement dependencyManagement = model.getDependencyManagement();
        if (dependencyManagement == null) {
            return new DependencyManagement();
        }

        List<Dependency> dependencies = dependencyManagement.getDependencies();
        if (dependencies == null) {
            return new DependencyManagement();
        }

        var management = new DependencyManagement();
        for (var dependency : dependencyManagement.getDependencies()) {
            management.addDependency(dependency);
        }

        return management;
    }

    private DependencyManagement() {}

    /** Returns the version specified for the given groupId and artifactId, if any. */
    Optional<String> versionOf(String groupId, String artifactId) {
        return Optional.ofNullable(versions.getOrDefault(groupId, Map.of())).map(m -> m.get(artifactId));
    }

    private void addDependency(Dependency dependency) {
        String groupId = Objects.requireNonNull(dependency.getGroupId());
        String artifactId = Objects.requireNonNull(dependency.getArtifactId());
        String version = Objects.requireNonNull(dependency.getVersion());

        versions.computeIfAbsent(groupId, ignored -> new HashMap<>()).put(artifactId, version);
    }

}
