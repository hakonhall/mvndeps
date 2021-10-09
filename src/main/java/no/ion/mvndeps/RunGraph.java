package no.ion.mvndeps;

import java.io.BufferedWriter;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static no.ion.mvndeps.Exceptions.uncheck;
import static no.ion.mvndeps.Exceptions.uncheckIO;

public class RunGraph implements AutoCloseable {
    private final Path directory;
    private final MavenProject project;
    private final BuildQueue queue;
    private final Path dotPath;
    private final BufferedWriter writer;
    private final Map<MavenCoordinate, BuiltModule> builtModules = new HashMap<>();
    private static final boolean build = true;

    public static void compute(Path projectDirectory, Path dotPath) {
        MavenProject project = MavenProjectReader.readProjectDirectory(projectDirectory);
        BuildQueue queue = BuildQueue.make(project);
        var writer = uncheckIO(() -> Files.newBufferedWriter(dotPath, StandardOpenOption.CREATE,
                                                             StandardOpenOption.TRUNCATE_EXISTING));
        try (var runGraph = new RunGraph(projectDirectory, project, queue, dotPath, writer)) {
            runGraph.go();
        }
    }

    public RunGraph(Path directory, MavenProject project, BuildQueue buildQueue, Path dotPath, BufferedWriter writer) {
        this.directory = directory;
        this.project = project;
        this.queue = buildQueue;
        this.dotPath = dotPath;
        this.writer = writer;
    }

    public void go() {
        List<MavenModule> previousReadyToBuild = List.of();

        writeln("digraph Build {");
        for (int round = 1; !queue.isDone(); ++round) {
            writeln("  // build round " + round + ":");
            List<MavenModule> currentReadyToBuild = queue
                    .readyToBuild()
                    .stream()
                    .sorted(Comparator.comparing(RunGraph::toNodeId))
                    .collect(Collectors.toList());

            DotEdge[] criticalPathEdge = { null };

            // Modules in this round
            List<MavenModule> finalPreviousReadyToBuild = previousReadyToBuild;
            currentReadyToBuild.stream()
                               .sorted(Comparator.comparing(RunGraph::toNodeId))
                               .forEach(module -> {
                                   final Duration elapsedTime;
                                   if (build) {
                                       elapsedTime = build(module);
                                   } else {
                                       elapsedTime = Duration.ofSeconds(1);
                                   }
                                   Duration baseTime = Duration.ZERO;

                                   var edges = new ArrayList<DotEdge>();

                                   queue.remove(module.coordinate());
                                   for (var previousModule : finalPreviousReadyToBuild) {
                                       if (module.hasDirectDependencyOrParent(previousModule)) {
                                           var edge = new DotEdge(toNodeId(previousModule), toNodeId(module));
                                           edge.addAttribute("label", edgeLabel(elapsedTime));
                                           edge.addAttribute("penwidth", durationToPenwidth(elapsedTime));
                                           edges.add(edge);

                                           BuiltModule builtModule = builtModules.get(previousModule.coordinate());
                                           if (builtModule == null) {
                                               throw new IllegalStateException(toNodeId(module) + " depends on " +
                                                       previousModule.coordinate() + " but it has not been built");
                                           }

                                           // TODO: funker ikke
                                           if (criticalPathEdge[0] == null ||
                                                   builtModule.totalElapsedTime().compareTo(baseTime) > 0) {
                                               criticalPathEdge[0] = edge;
                                               baseTime = builtModule.totalElapsedTime();
                                           }
                                       }
                                   }

                                   if (criticalPathEdge != null) {
                                       /*criticalPathEdge.addAttribute("color", "red");
                                       criticalPathEdge.addAttribute("weight", "50");*/
                                       for (var edge : edges) {
                                           writeln("  " + edge.toString());
                                       }
                                   }

                                   var builtModule = new BuiltModule(module, elapsedTime, baseTime.plus(elapsedTime));
                                   builtModules.put(module.coordinate(), builtModule);
                                   writeln("  " + toNodeId(module) + " // " + elapsedTime + " (" +
                                                   builtModule.totalElapsedTime() + ") @" + module.modulePath());
                               });
            writeln();

            previousReadyToBuild = currentReadyToBuild;
        }

        writeln("}");
    }

    @Override
    public void close() { uncheckIO(writer::close); }

    private static Duration max(Duration left, Duration right) { return left.compareTo(right) > 0 ? left : right; }

    private static String durationToPenwidth(Duration elapsedTime) {
        double seconds = elapsedTime.toNanos() / 1_000_000_000.0;

        long x10width;
        if (seconds < 1) {
            x10width = 5;
        } else {
            // 60 seconds gives 7 points width.
            // TODO: We should instead scale s.t. the largest elapsedTime has a fixed max width
            x10width = Math.round(10 * (7 * Math.log(seconds) / Math.log(60)) );
            if (x10width > 100) {
                x10width = 100;
            }
        }

        return (x10width / 10) + "." + (x10width % 10);
    }

    /**
     * Return the logarithm with base B, such that exampleResult would be returned if input is exampleValue.  But force
     * the result to be between low and high, inclusive.  Then round off to include 'decimals' significant decimal digits.
     */
    private static double squash(double value, double exampleValue, double exampleResult, double low, double high, int decimals) {
        if (value <= 0) {
            return round(low, decimals);
        }

        double result = exampleResult * Math.log(value) / Math.log(exampleValue);
        double squeezedResult = squeeze(low, result, high);
        return round(squeezedResult, decimals);
    }

    private static double round(double value, int decimals) {
        double factor = Math.pow(10, decimals);
        return Math.round(value * factor) / factor;
    }

    private static double squeeze(double low, double value, double high) {
        if (value < low) {
            return low;
        } else if (value > high) {
            return high;
        } else {
            return value;
        }
    }

    private static String edgeLabel(Duration elapsedTime) {
        return "\"" + (elapsedTime.toNanos() + 500_000_000L) / 1_000_000_000L + "s\"";
    }

    private static String toNodeId(MavenModule module) { return toNodeId(module.coordinate().getGroupArtifact()); }
    private static String toNodeId(MavenDependency dependency) { return toNodeId(dependency.getArtifact()); }
    private static String toNodeId(ArtifactId artifactId) { return "\"" + artifactId.toString() + "\""; }

    private Duration build(MavenModule module) {
        mvn("clean", "-pl", ":" + module.coordinate().artifactId());
        return mvn("install", "-pl", ":" + module.coordinate().artifactId());
    }

    private Duration mvn(String... additionalArguments) {
        List<String> command = new ArrayList<>();
        command.add("mvn");
        // TODO: Fix
        command.add("-Dmaven.repo.local=/home/hakon/local/github/vespa-engine/vespa/.m2/repository");
        command.add("-T1");
        command.addAll(List.of(additionalArguments));
        return exec(command);
    }

    private Duration exec(List<String> command) {
        System.out.println("Executing: " + command);

        ProcessBuilder processBuilder = new ProcessBuilder(command)
                .inheritIO()
                .redirectErrorStream(true)
                .directory(directory.toFile());

        Map<String, String> environment = processBuilder.environment();
        // TODO: Fix
        environment.put("JAVA_HOME", "/home/hakon/share/jdk-11.0.10+9");

        long startNanos = System.nanoTime();
        Process process = uncheckIO(processBuilder::start);

        InputStream inputStream = process.getInputStream();
        byte [] buffer = new byte[8192];
        while (true) {
            int bytesRead = uncheckIO(() -> inputStream.read(buffer));
            if (bytesRead == -1) {
                // EOF, so wait for process exit
                Integer value = uncheck(process::waitFor);
                long endNanos = System.nanoTime();
                if (value != 0) {
                    throw new ErrorMessage("Command failed with exit code " + value + ": " + command);
                }

                return Duration.ofNanos(endNanos - startNanos);
            }

            var string = new String(buffer, 0, bytesRead, StandardCharsets.UTF_8);
            System.out.println(string);
        }
    }

    private void writeln(String line) {
        write(line) ;
        writeln();
    }

    private void writeln() {
        write('\n');
        flush();
    }

    private void write(String str) { uncheckIO(() -> writer.write(str)); }
    private void write(char c) { uncheckIO(() -> writer.write(c)); }
    private void flush() { uncheckIO(writer::flush); }
}
