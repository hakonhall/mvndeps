package no.ion.mvndeps;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class MavenModule {
    private final MavenCoordinate coordinate;
    private final DependencyManagement dependencyManagement;
    private final Optional<MavenModule> parent;
    private final Path modulePath;
    private final Model model;

    static MavenModule of(Path modulePath, Model model, Optional<MavenModule> parentModule) {
        String groupId = Optional
                .ofNullable(model.getGroupId())
                .orElseGet(() -> parentModule
                        .map(parent -> parent.coordinate.groupId())
                        .orElseThrow(() -> new BadPomException(modulePath, "missing groupId")));

        String artifactId = Optional
                .ofNullable(model.getArtifactId())
                .orElseThrow(() -> new BadPomException(modulePath, "missing artifactId"));

        String version = Optional
                .ofNullable(model.getVersion())
                .orElseGet(() -> parentModule
                        .map(parent -> parent.coordinate().version())
                        .orElseThrow(() -> new BadPomException(modulePath, "missing version")));

        MavenCoordinate coordinate = new MavenCoordinate(groupId, artifactId, version);

        DependencyManagement dependencyManagement = DependencyManagement.of(model);

        return new MavenModule(modulePath, model, parentModule, coordinate, dependencyManagement);
    }

    private MavenModule(Path modulePath, Model model, Optional<MavenModule> parent, MavenCoordinate coordinate,
                DependencyManagement dependencyManagement) {
        this.modulePath = modulePath;
        this.model = model;
        this.parent = parent;
        this.coordinate = coordinate;
        this.dependencyManagement = dependencyManagement;
    }

    public Path modulePath() { return modulePath; }
    public MavenCoordinate coordinate() { return coordinate; }
    public String packaging() { return Optional.ofNullable(model.getPackaging()).orElse("jar"); }
    public String to4CoordinateString() {
        return coordinate.groupId() + ":" + coordinate.artifactId() + ":" + packaging() + ":" + coordinate.version();
    }
    public Optional<MavenModule> parent() { return parent; }

    String replaceProperties(String string) {
        final String prefix = "${";
        final char suffix = '}';

        int startIndex = string.indexOf(prefix);
        if (startIndex == -1) {
            return string;
        }

        StringBuilder builder = new StringBuilder();
        int appendFromIndex = 0;
        do {
            builder.append(string, appendFromIndex, startIndex);

            int endIndex = string.indexOf(suffix, startIndex + prefix.length());
            if (endIndex == -1) {
                builder.append(string, startIndex, string.length());
                return builder.toString();
            }

            String key = string.substring(startIndex + prefix.length(), endIndex);
            String replacement = getProperty(key)
                    .orElseThrow(() -> new BadPomException(modulePath, "failed to resolve property '" + key + "'"));
            builder.append(replacement);

            appendFromIndex = endIndex + 1 /* = suffix.length() */;
            startIndex = string.indexOf(prefix, appendFromIndex);
            if (startIndex == -1) {
                builder.append(string, appendFromIndex, string.length());
                return builder.toString();
            }
        } while (true);
    }

    List<MavenDependency> getDependencies() {
        List<Dependency> dependencies = model.getDependencies();
        if (dependencies == null) {
            return List.of();
        }

        return dependencies.stream()
                .map(dep -> MavenDependency.fromDependency(this, dep))
                .collect(Collectors.toList());
    }

    public boolean hasDirectDependencyOrParent(MavenModule thatModule) {
        if (parent.isPresent() && thatModule.coordinate.equals(parent.get().coordinate)) {
            return true;
        }

        if (getDependencies().stream()
                .anyMatch(dependency ->
                    dependency.groupId().equals(thatModule.coordinate.groupId()) &&
                dependency.artifactId().equals(thatModule.coordinate.artifactId()) &&
                dependency.resolvedVersion().equals(thatModule.coordinate.version()))) {
            return true;
        }

        // TODO: Parse plugins. See also in BuildQueue
        var documentgenTestCoordinate = new MavenCoordinate("com.yahoo.vespa", "documentgen-test", "7-SNAPSHOT");
        if (coordinate.equals(documentgenTestCoordinate)) {
            var vespaDocumentgenPluginCoordinate = new MavenCoordinate("com.yahoo.vespa", "vespa-documentgen-plugin", "7-SNAPSHOT");
            if (thatModule.coordinate.equals(vespaDocumentgenPluginCoordinate)) {
                return true;
            }
        }

        return false;
    }

    /** Returns project dependencies in dependencies, parent, and plugins sections in pom.xml.
     * @param project*/
    public List<MavenCoordinate> getAllProjectDependencies(MavenProject project) {
        var dependencies = new ArrayList<MavenCoordinate>();

        parent.filter(module -> project.artifactIsInProject(module.coordinate.getGroupArtifact()))
              .ifPresent(module -> dependencies.add(module.coordinate));

        getDependencies().stream()
                         .filter(dependency -> project.artifactIsInProject(dependency.getArtifact()))
                         .map(MavenDependency::resolvedCoordinate)
                         .forEach(dependencies::add);

        // TODO: Parse plugins
        if (coordinate.getGroupArtifact().equals(new ArtifactId("com.yahoo.vespa", "documentgen-test"))) {
            var pluginCoordinate = new MavenCoordinate("com.yahoo.vespa", "vespa-documentgen-plugin", coordinate.version());
            ArtifactId pluginGroupArtifact = pluginCoordinate.getGroupArtifact();
            if (!project.artifactIsInProject(pluginGroupArtifact)) {
                throw new IllegalStateException("No longer in project: source needs to be updated: " + pluginGroupArtifact);
            }
            dependencies.add(pluginCoordinate);
        }

        return dependencies;
    }

    List<MavenDependency> getPreTestDependencies() {
        List<Dependency> dependencies = model.getDependencies();
        if (dependencies == null) {
            return List.of();
        }

        return dependencies.stream()
                .filter(dep -> !Objects.equals(dep.getScope(), "test"))
                .map(dep -> MavenDependency.fromDependency(this, dep))
                .collect(Collectors.toList());
    }

    @Override
    public String toString() {
        return coordinate.groupId() + ":" + coordinate.artifactId() + ":" + packaging() + ":" + coordinate.version();
    }

    private Optional<String> getProperty(String key) {
        switch (key) {
            case "project.version": return Optional.of(coordinate.version());
        }

        String property = model.getProperties().getProperty(key);
        if (property != null) {
            return Optional.of(replaceProperties(property));
        }

        if (parent.isPresent()) {
            return parent.get().getProperty(key);
        }

        return Optional.empty();
    }
}
