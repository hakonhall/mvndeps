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
        var buildResults = new BuildResults();

        List<String> lines = uncheckIO(() -> Files.readAllLines(inputPath, StandardCharsets.UTF_8));
        int lineno = 0;
        for (var line : lines) {
            ++lineno;

            if (line.isEmpty() || line.startsWith("#"))
                continue;

            String[] tokens = line.split(" ", -1);
            if (tokens.length != 3)
                throw new IllegalStateException("Wrong number of tokens on line + " + lineno + ": " + line);

            buildResults.add(BuildResult.from(fileSystem, tokens[0], tokens[1], tokens[2]));
        }

        Build build = Build.read(projectRoot);

        new BuildGraph(outputPath, buildResults, build).go();
    }
}
