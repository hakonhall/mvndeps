package no.ion.mvndeps.build;

import no.ion.mvndeps.io.BuildInfos;

import java.nio.file.FileSystem;
import java.nio.file.Path;

import static no.ion.mvndeps.misc.Exceptions.uncheckIO;

public class DotCommand {
    private final FileSystem fileSystem;
    private final Path inputPath;
    private final Path outputPath;
    private final Path projectRoot;

    public DotCommand(Path inputPath, Path outputPath, Path projectRoot) {
        this.fileSystem = inputPath.getFileSystem();
        this.inputPath = inputPath;
        this.outputPath = outputPath;
        this.projectRoot = projectRoot;
    }

    public void go() {
        Build build = Build.read(projectRoot);
        var buildInfos = BuildInfos.readFromPath(inputPath);
        new BuildGraph(outputPath, buildInfos, build).go();
    }
}
