package no.ion.mvndeps.maven;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MavenProjectBuilder {
    private final List<MavenModule> modules = new ArrayList<>();
    private final Path projectDirectory;

    MavenProjectBuilder(Path projectDirectory) {
        this.projectDirectory = projectDirectory;
    }

    private void requireProjectDirectory() {
        Objects.requireNonNull(projectDirectory, "No project directory has been specified");
    }

    MavenProject build() {
        requireProjectDirectory();
        return new MavenProject(projectDirectory, modules);
    }

    public void addMavenModule(MavenModule mavenModule) {
        modules.add(mavenModule);
    }
}
