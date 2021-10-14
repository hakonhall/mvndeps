package no.ion.mvndeps;

import no.ion.mvndeps.build.Build;
import no.ion.mvndeps.build.BuildEdge;
import no.ion.mvndeps.build.DotCommand;
import no.ion.mvndeps.build.ModuleBuild;
import no.ion.mvndeps.graph.Vertex;
import no.ion.mvndeps.maven.ArtifactId;
import no.ion.mvndeps.maven.ModuleInfo;
import no.ion.mvndeps.maven.MavenCoordinate;
import no.ion.mvndeps.misc.FileWriter;
import no.ion.mvndeps.misc.Mvn;
import no.ion.mvndeps.misc.Option;
import no.ion.mvndeps.misc.Usage;

import javax.lang.model.SourceVersion;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
        Path inputPath = null;
        Path outputPath = null;
        Path projectDirectory = null;

        for (; !args.atEnd(); args.next()) {
            switch (args.getString()) {
                case "-i":
                    inputPath = Path.of(args.nextAsStringOptionValue());
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

        failIf(inputPath == null, "-i is required");
        Path finalInputPath = inputPath;
        failIf(uncheckIO(() -> !Files.isRegularFile(finalInputPath)), "Input file does not exist: " + inputPath);

        failIf(outputPath == null, "-o is required");
        Path finalOutputPath = outputPath;
        failIf(uncheckIO(() -> !Files.isDirectory(finalOutputPath.getParent())), "Directory of output path does not exist: " +
                outputPath.getParent());

        failIf(projectDirectory == null, "-p is required");
        Path finalProjectDirectory = projectDirectory;
        failIf(uncheckIO(() -> !Files.isDirectory(finalProjectDirectory)), "Project does not exist: " + projectDirectory);

        failIf(!args.atEnd(), "Extraneous arguments");

        new DotCommand(inputPath, outputPath, projectDirectory).go();
    }

    private void buildOrder() {
        var usage = new Usage();
        Option<String> format = usage.addOption("-f", "artifactId", spec -> switch (spec) {
            case "artifactId", "full", "groupId:artifactId", "path" -> spec;
            default -> throw new UsageError("Invalid option value: '" + spec + "'");
        });
        Option<Optional<Path>> outputDirectory = usage.addOption("-o", Optional.empty(), s -> Optional.of(Path.of(s)));
        Option<Path> projectDirectory = usage.addRequired("-p", Path::of);

        usage.readOptions(args);

        Build build = Build.read(projectDirectory.get());

        List<Vertex<MavenCoordinate, ModuleBuild, BuildEdge>> verticesInBuildOrder = build.buildOrder();

        var serialized = new StringBuilder();

        if (!format.isPresent() && outputDirectory.isPresent()) {
            format.setValue("full");
        }

        verticesInBuildOrder.stream()
                            .map(vertex -> switch (format.get()) {
                                case "artifactId" -> vertex.id().artifactId();
                                case "full" -> vertex.id().getGroupArtifact() + " " + vertex.get().module().modulePath();
                                case "groupId:artifactId" -> vertex.id().getGroupArtifact();
                                case "path" -> vertex.get().module().modulePath();
                                default -> throw new IllegalStateException("Unknown format: '" + format.get() + "'");
                            })
                            .forEach(line -> serialized.append(line).append('\n'));

        outputDirectory.get().ifPresentOrElse(outDir -> {
            createDirectoryUnlessItExists(outDir);
            Path buildOrderFilePath = buildOrderFilePath(outDir);
            uncheckIO(() -> Files.writeString(buildOrderFilePath,
                                              serialized,
                                              StandardOpenOption.TRUNCATE_EXISTING,
                                              StandardOpenOption.CREATE));
            System.out.println("Wrote build order to " + buildOrderFilePath);
        }, () -> {
            System.out.print(serialized);
        });
    }

    private void buildBasedOnBuildOrderFile() {
        var usage = new Usage();
        Option<Path> javaHome = usage.addOption("-j", null, Path::of);
        Option<Path> buildOrderPath = usage.addRequired("-o", Path::of);
        Option<Path> projectDirectory = usage.addRequired("-p", Path::of);
        Option<Path> mavenRepoLocal = usage.addOption("-r", defaultMavenRepoLocal(), Path::of);

        usage.readOptions(args);

        if (!args.atEnd())
            throw new UsageError("Extraneous arguments");

        Path resolvedBuildOrderPath = Files.isDirectory(buildOrderPath.get()) ?
                buildOrderFilePath(buildOrderPath.get()) :
                buildOrderPath.get();

        List<String> lines = uncheckIO(() -> Files.readAllLines(resolvedBuildOrderPath));
        Pattern linePattern = Pattern.compile("^(.*):(.*) (.*)$");
        List<ModuleInfo> moduleInfos = lines
                .stream()
                .filter(line -> !line.startsWith("#"))
                .map(line -> {
                    Matcher matcher = linePattern.matcher(line);
                    if (!matcher.find()) {
                        throw new UsageError("Bad line in build order file " + resolvedBuildOrderPath + ": " + line);
                    }


                    String groupId = matcher.group(1);
                    if (!SourceVersion.isName(groupId))
                        throw new UsageError("Not a valid groupId in build order file: " + line);

                    String artifactId = matcher.group(2);
                    if (artifactId.indexOf(' ') != -1)
                        throw new UsageError("Not a valid artifactId in build order file: " + line);

                    String path = matcher.group(3);
                    String normalized = Path.of(path).normalize().toString();
                    if (normalized.startsWith("/") || normalized.equals("..") || normalized.startsWith("../"))
                        throw new UsageError("Not a valid path in build order file: " + line);

                    return new ModuleInfo(new ArtifactId(groupId, artifactId), Path.of(path));
                })
                .collect(Collectors.toList());

        build(projectDirectory.get(), moduleInfos, javaHome.get(), mavenRepoLocal.get());
    }

    private static Path defaultMavenRepoLocal() {
        String home = System.getProperty("user.home");
        if (home == null)
            throw new UsageError("user.home system property (home directory) is not set");

        return Path.of(home);
    }

    private void build(Path projectDirectory, List<ModuleInfo> moduleInfos, Path javaHomeOrNull,
                       Path mavenRepoLocalOrNull) {
        Mvn mvn = new Mvn(javaHomeOrNull, projectDirectory, mavenRepoLocalOrNull);
        mvn.clean();

        for (var moduleInfo : moduleInfos) {
            long startNanos = System.nanoTime();
            mvn.install(moduleInfo.path());
            var elapsedTime = Duration.ofNanos(System.nanoTime() - startNanos);
        }
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
