package no.ion.mvndeps.build;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static no.ion.mvndeps.misc.Exceptions.uncheckIO;

public class BuildResultsReader {
    public static BuildResults read(Path buildResultPath) {
        var buildResults = new BuildResults();

        List<String> lines = uncheckIO(() -> Files.readAllLines(buildResultPath, StandardCharsets.UTF_8));
        int lineno = 0;
        for (var line : lines) {
            ++lineno;

            if (line.isEmpty() || line.startsWith("#"))
                continue;

            String[] tokens = line.split(" ", -1);
            if (tokens.length != 3)
                throw new IllegalStateException("Wrong number of tokens on line + " + lineno + ": " + line);

            buildResults.add(BuildResult.from(buildResultPath.getFileSystem(), tokens[0], tokens[1], tokens[2]));
        }

        return buildResults;
    }
}
