package no.ion.mvndeps.maven;

import org.apache.maven.model.Dependency;

import java.util.Objects;
import java.util.Optional;

public class MavenDependency {
    private final MavenModule module;
    private final Dependency dependency;

    static MavenDependency fromDependency(MavenModule module, Dependency dependency) {
        return new MavenDependency(module, dependency);
    }

    private MavenDependency(MavenModule module, Dependency dependency) {
        this.module = module;
        this.dependency = dependency;
    }

    public ArtifactId getArtifact() { return new ArtifactId(groupId(), artifactId()); }
    public String groupId() { return Objects.requireNonNull(dependency.getGroupId()); }
    public String artifactId() { return Objects.requireNonNull(dependency.getArtifactId()); }
    public Optional<String> version() { return Optional.ofNullable(dependency.getVersion()); }

    /** Version, with value resolved from properties and any parent. */
    public String resolvedVersion() {
        return Optional
                .ofNullable(dependency.getVersion())
                .map(version -> module.replaceProperties(version))
                .orElseGet(() -> module.parent()
                        .map(parent -> parent.coordinate().version())
                        .orElseThrow(() -> new BadPomException(module.modulePath(), "missing version of dependency " +
                                toString())));
    }

    public MavenCoordinate resolvedCoordinate() {
        String version = resolvedVersion();
        return new MavenCoordinate(groupId(), artifactId(), version);
    }

    @Override
    public String toString() {
        return groupId() + ":" + artifactId() + version().map(v -> ":" + v).orElse("");
    }
}
