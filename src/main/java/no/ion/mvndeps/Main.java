package no.ion.mvndeps;

import no.ion.mvndeps.build.Build;
import no.ion.mvndeps.build.BuildEdge;
import no.ion.mvndeps.build.DotCommand;
import no.ion.mvndeps.build.ModuleBuild;
import no.ion.mvndeps.graph.Vertex;
import no.ion.mvndeps.io.BuildInfo;
import no.ion.mvndeps.io.BuildInfos;
import no.ion.mvndeps.maven.MavenCoordinate;
import no.ion.mvndeps.misc.FileWriter;
import no.ion.mvndeps.misc.Mvn;
import no.ion.mvndeps.misc.Option;
import no.ion.mvndeps.misc.Usage;
import no.ion.mvndeps.proto.Builds;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static no.ion.mvndeps.misc.Exceptions.uncheckIO;

public class Main {
    private static final Path JAVA11_HOME = Path.of("/home/hakon/share/jdk-11.0.10+9");

    private final ProgramArguments args;

    public static void main(String... args) {
        try {
            new Main(args).go();
        } catch (UsageError e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }
    }

    public Main(String... args) {
        this.args = new ProgramArguments(args);
    }

    public void go() {
        switch (args.getString()) {
            case "build":
                args.next();
                buildBasedOnBuildOrderFile();
                break;
            case "dot":
                args.next();
                dotCommand();
                break;
            case "timings":
                args.next();
                buildAll();
                break;
            case "build-order":
                args.next();
                buildOrder();
                break;
            default:
                throw new UsageError("Unknown command: " + args.getString());
        }
    }

    private static void failIf(boolean condition, String message) {
        if (condition)
            throw new UsageError(message);
    }

    private static void fail(String message) {
        System.err.println(message);
        System.exit(1);
    }

    private void dotCommand() {
        var usage = new Usage();
        Option<Path> inputPath = usage.addRequired("-i", Path::of);
        Option<Path> outputPath = usage.addRequired("-o", Path::of);
        Option<Path> projectDirectory = usage.addRequired("-p", Path::of);

        usage.readOptions(args);

        Path finalInputPath = inputPath.get();
        failIf(uncheckIO(() -> !Files.isRegularFile(finalInputPath)), "Input file does not exist: " + inputPath);

        Path finalOutputPath = outputPath.get();
        failIf(uncheckIO(() -> !Files.isDirectory(finalOutputPath.getParent())), "Directory of output path does not exist: " +
                finalOutputPath.getParent());

        Path finalProjectDirectory = projectDirectory.get();
        failIf(uncheckIO(() -> !Files.isDirectory(finalProjectDirectory)), "Project does not exist: " + projectDirectory);

        failIf(!args.atEnd(), "Extraneous arguments");

        new DotCommand(inputPath.get(), outputPath.get(), projectDirectory.get()).go();
    }

    private void buildOrder() {
        var usage = new Usage();
        Option<String> format = usage.addOption("-f", "textual", spec -> switch (spec) {
            case "proto" -> spec;
            default -> throw new UsageError("Invalid option value: '" + spec + "'");
        });
        Option<Optional<Path>> outputDirectory = usage.addOption("-o", Optional.empty(), s -> Optional.of(Path.of(s)));
        Option<Path> projectDirectory = usage.addRequired("-p", Path::of);

        usage.readOptions(args);

        Build build = Build.read(projectDirectory.get());

        List<Vertex<MavenCoordinate, ModuleBuild, BuildEdge>> verticesInBuildOrder = build.buildOrder();

        if (format.get().equals("proto")) {
            if (outputDirectory.get().isEmpty())
                throw new UsageError(outputDirectory.name() + " is required with " + format.get() + " format");

            var buildsBuilder = Builds.newBuilder();
            verticesInBuildOrder.forEach(vertex -> buildsBuilder.addBuilds(
                    no.ion.mvndeps.proto.Build.newBuilder()
                                              .setGroupId(vertex.id().groupId())
                                              .setArtifactId(vertex.id().artifactId())
                                              .setPath(vertex.get().module().modulePath().toString())
                                              .build()));

            Builds builds = buildsBuilder.build();

            createDirectoryUnlessItExists(outputDirectory.get().get());
            Path buildOrderFilePath = buildOrderFilePath(outputDirectory.get().get());
            try (var outputStream = Files.newOutputStream(buildOrderFilePath)) {
                builds.writeTo(outputStream);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }

        } else {
            ArrayList<BuildInfo> buildInfosList = new ArrayList<>();
            verticesInBuildOrder.forEach(vertex -> {
                var buildInfo = new BuildInfo(vertex.id().getGroupArtifact(), vertex.get().module().modulePath(), null);
                buildInfosList.add(buildInfo);
            });
            var buildInfos = new BuildInfos(buildInfosList);

            if (outputDirectory.get().isPresent()) {
                createDirectoryUnlessItExists(outputDirectory.get().get());
                Path buildOrderFilePath = buildOrderFilePath(outputDirectory.get().get());
                buildInfos.writeToPath(buildOrderFilePath);
            } else {
                buildInfos.buildInfos().forEach(buildInfo -> System.out.println(buildInfo.serialize()));
            }
        }
    }

    private void buildBasedOnBuildOrderFile() {
        var usage = new Usage();
        Option<String> format = usage.addOption("-f", "textual", spec -> switch (spec) {
            case "proto", "textual" -> spec;
            default -> throw new UsageError("Invalid option value: '" + spec + "'");
        });
        Option<Path> buildOrderPath = usage.addRequired("-i", Path::of);
        Option<Path> javaHome = usage.addOption("-j", null, Path::of);
        Option<Path> outPathOption = usage.addOption("-o", null, pathString -> {
            Path path = Path.of(pathString);
            if (!Files.isDirectory(path.getParent()))
                throw new UsageError("Parent directory of output file does not wxist: " + path);
            return path;
        });
        Option<Path> projectDirectory = usage.addRequired("-p", Path::of);
        Option<Path> mavenRepoLocal = usage.addOption("-r", defaultMavenRepoLocal(), Path::of);

        usage.readOptions(args);

        Path resolvedBuildOrderPath = Files.isDirectory(buildOrderPath.get()) ?
                buildOrderFilePath(buildOrderPath.get()) :
                buildOrderPath.get();

        Path outputPath = outPathOption.isPresent() ?
                outPathOption.get() :
                resolvedBuildOrderPath;

        if (!args.atEnd())
            throw new UsageError("Extraneous arguments");

        final BuildInfos buildInfos;

        switch (format.get()) {
            case "proto": {
                Builds builds;
                try (var inputStream = Files.newInputStream(resolvedBuildOrderPath)) {
                    builds = Builds.parseFrom(inputStream);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }

                var buildInfoList = new ArrayList<BuildInfo>();
                for (var b : builds.getBuildsList()) {
                    long buildTimeNanos = b.getBuildTimeNanos();
                    Double buildTimeSeconds = buildTimeNanos == 0L ? null : buildTimeNanos / 1e9;
                    var buildInfo = BuildInfo.fromRaw(b.getGroupId(), b.getArtifactId(), b.getPath(), buildTimeSeconds);
                    buildInfoList.add(buildInfo);
                }

                buildInfos = new BuildInfos(List.copyOf(buildInfoList));
                break;
            }
            case "textual": {
                buildInfos = BuildInfos.readFromPath(resolvedBuildOrderPath);
                break;
            }
            default:
                throw new IllegalStateException("Unknown format: " + format.get());
        }

        build(projectDirectory.get(), buildInfos, javaHome.get(), mavenRepoLocal.get(), outputPath);
    }

    private static Path defaultMavenRepoLocal() {
        String home = System.getProperty("user.home");
        if (home == null)
            throw new UsageError("user.home system property (home directory) is not set");

        return Path.of(home);
    }

    private void build(Path projectDirectory, BuildInfos buildInfos, Path javaHomeOrNull,
                       Path mavenRepoLocalOrNull, Path outPath) {
        Mvn mvn = new Mvn(javaHomeOrNull, projectDirectory, mavenRepoLocalOrNull);
        mvn.clean();

        var updatedBuildInfos = new BuildInfos(
                buildInfos.buildInfos()
                          .stream()
                          .map(buildInfo -> {
                              long startNanos = System.nanoTime();
                              mvn.install(buildInfo.modulePath());
                              var elapsedTime = Duration.ofNanos(System.nanoTime() - startNanos);
                              return new BuildInfo(buildInfo.artifactId(), buildInfo.modulePath(), elapsedTime);
                          })
                          .collect(Collectors.toList()));

        updatedBuildInfos.writeToPath(outPath);
    }

    private static void createDirectoryUnlessItExists(Path dir) {
        try {
            Files.createDirectory(dir);
        } catch (FileAlreadyExistsException e) {
            // ok
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static Path buildOrderFilePath(Path outputDirectory) {
        return outputDirectory.resolve("build-order.dat");
    }

    private void buildAll() {
        Path javaHome = Optional.ofNullable(System.getProperty("JAVA_HOME")).map(Path::of).orElse(null);
        Path outputPath = null;
        Path projectDirectory = null;

        for (; !args.atEnd(); args.next()) {
            switch (args.getString()) {
                case "-j":
                    javaHome = Path.of(args.nextAsStringOptionValue());
                    continue;
                case "-o":
                    outputPath = Path.of(args.nextAsStringOptionValue());
                    continue;
                case "-p":
                    projectDirectory = Path.of(args.nextAsStringOptionValue());
                    continue;
                default:
                    if (args.getString().startsWith("-"))
                        throw new UsageError("Unknown option: " + args.getString());
                    // fall through to break
            }
            break;
        }

        failIf(javaHome == null, "-j or JAVA_HOME must be set for mvn command");
        Path finalJavaHome = javaHome;
        failIf(uncheckIO(() -> !Files.isDirectory(finalJavaHome)), "Java home does not exist: " + javaHome);

        failIf(projectDirectory == null, "-p is required");
        failIf(outputPath == null, "-o is required");
        Path finalOutputPath = outputPath;
        failIf(uncheckIO(() -> !Files.isDirectory(finalOutputPath.getParent())), "Directory of output path does not exist: " +
                outputPath.getParent());

        failIf(!args.atEnd(), "Extraneous arguments");

        var mvn = new Mvn(JAVA11_HOME, projectDirectory, projectDirectory.resolve(".m2/repository"));
        mvn.clean();

        Build build = Build.read(projectDirectory);

        try (var writer = FileWriter.truncateForWriting(outputPath)) {
            build.buildOrder().forEach(vertex -> {
                Duration elapsedTime = mvn.install(vertex.get().module().modulePath());
                String serialized = String.format("%s %s %.3f\n",
                                                  vertex.get().module().modulePath(),
                                                  vertex.get().module().coordinate().getGroupArtifact(),
                                                  Math.round(elapsedTime.toNanos() / 1_000_000.0) / 1_000.0);
                writer.write(serialized);
            });
        }
    }
}
