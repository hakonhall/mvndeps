package no.ion.mvndeps.maven;

import no.ion.mvndeps.PomParent;
import org.apache.maven.model.Model;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

public class Pom {
    private final Path path;
    private final Model model;
    private final Optional<PomParent> pomParent;

    static Pom fromModel(Path pomPath, Model model) {
        Optional<PomParent> pomParent = Optional
                .ofNullable(model.getParent())
                .map(parent -> PomParent.from(pomPath, parent.getGroupId(), parent.getArtifactId(),
                        parent.getVersion(), pomPath.getFileSystem().getPath(parent.getRelativePath())));
        return new Pom(pomPath, model, pomParent);
    }

    private Pom(Path path, Model model, Optional<PomParent> pomParent) {
        this.path = path;
        this.model = model;
        this.pomParent = pomParent;
    }

    Path path() { return path; }
    Optional<String> groupId() { return Optional.ofNullable(model.getGroupId()); }
    Optional<String> artifactId() { return Optional.ofNullable(model.getArtifactId()); }
    Optional<String> packaging() { return Optional.ofNullable(model.getPackaging()); }
    Optional<String> version() { return Optional.ofNullable(model.getVersion()); }

    Model model() { return model; }

    public Path directory() { return path.getParent(); }
    public Path modulePath(Path projectDirectory) {
        Path relative = projectDirectory.relativize(directory());
        return relative.toString().isEmpty() ? relative.resolve(".") : relative;
    }
    public List<String> getModules() { return List.copyOf(model.getModules()); }

    public Optional<PomParent> parent() { return pomParent; }
}
