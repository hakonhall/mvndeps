package no.ion.mvndeps.build;

import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static no.ion.mvndeps.Exceptions.uncheckIO;

public class DotCommand {
    private final FileSystem fileSystem;
    private final Path inputPath;
    private final Path outputPath;
    private final Path projectRoot;

    DotCommand(Path inputPath, Path outputPath, Path projectRoot) {
        this.fileSystem = inputPath.getFileSystem();
        this.inputPath = inputPath;
        this.outputPath = outputPath;
        this.projectRoot = projectRoot;
    }

    void go() {
        Build build = Build.read(projectRoot);
        var buildResults = BuildResultsReader.read(inputPath);
        new BuildGraph(outputPath, buildResults, build).go();
    }
}
