package no.ion.mvndeps.maven;

import org.apache.maven.model.Model;

import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Optional;
import java.util.TreeMap;
import java.util.TreeSet;

public class MavenProjectReader {
    private final PomReader reader = new PomReader();
    /** Paths relative to the project directory, "." for the project directory itself. */
    private final TreeMap<Path, MavenModule> modules = new TreeMap<>();
    private final MavenProjectBuilder builder;
    private final FileSystem fileSystem;
    private final Path projectDirectory;

    private boolean verbose = false;

    /**
     * Path to module directories, relative to the project directory ("." for the project directory itself)
     * whose poms have not yet been read.
     */
    private final TreeSet<Path> unreadModulePaths = new TreeSet<>();

    /** Read the project pom.xml in the project directory and all its dependencies. */
    public static MavenProject readProjectDirectory(Path projectDirectory) {
        return new MavenProjectReader(projectDirectory).read();
    }

    private MavenProjectReader(Path projectDirectory) {
        this.fileSystem = projectDirectory.getFileSystem();
        this.projectDirectory = projectDirectory;
        this.builder = new MavenProjectBuilder(projectDirectory);
    }

    public MavenProjectReader setVerbose(boolean verbose) {
        this.verbose = verbose;
        return this;
    }

    /** Read projectDirectory, and its parent, modules, and dependencies, recursively. */
    private MavenProject read() {
        unreadModulePaths.add(fileSystem.getPath("."));

        while (true) {
            Iterator<Path> iterator = unreadModulePaths.iterator();
            if (!iterator.hasNext()) {
                break;
            }

            Path modulePath = iterator.next();
            iterator.remove();

            resolvePom(modulePath);
        }

        return builder.build();
    }

    private MavenModule resolvePom(Path modulePath) {
        MavenModule mavenModule = modules.get(modulePath);
        if (mavenModule != null) {
            return mavenModule;
        }

        if (verbose)
            System.out.println("Reading " + modulePath);

        Model model = reader.readModel(projectDirectory.resolve(modulePath));

        Optional<MavenModule> parentModule = ParentElement
                .fromModel(model)
                .map(parentElement -> {
                    Path parentModulePath = modulePath.resolve(parentElement.relativeDirectory()).normalize();
                    return resolvePom(parentModulePath);
                });

        model.getModules().stream()
                .map(relativePath -> modulePath.resolve(relativePath).normalize())
                .filter(submodulePath -> !modules.containsKey(submodulePath))
                .forEach(unreadModulePaths::add);

        mavenModule = MavenModule.of(modulePath, model, parentModule);
        builder.addMavenModule(mavenModule);
        modules.put(modulePath, mavenModule);
        return mavenModule;
    }
}
