package no.ion.mvndeps.build;

import no.ion.mvndeps.FileWriter;
import no.ion.mvndeps.Mvn;
import no.ion.mvndeps.UserError;
import no.ion.mvndeps.program.ProgramArguments;
import no.ion.mvndeps.program.UsageError;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import static no.ion.mvndeps.Exceptions.uncheckIO;

public class BuildMain {
    private static final Path vespaPath = Path.of("/home/hakon/local/github/vespa-engine/vespa");
    private static final Path JAVA11_HOME = Path.of("/home/hakon/share/jdk-11.0.10+9");

    private final ProgramArguments args;

    private static void failIf(boolean condition, String message) {
        if (condition)
            throw new UsageError(message);
    }

    private static void fail(String message) {
        System.err.println(message);
        System.exit(1);
    }

    public static void main(String... args) {
        try {
            new BuildMain(new ProgramArguments(args)).go();
        } catch (UserError e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }
    }

    private BuildMain(ProgramArguments args) {
        this.args = args;
    }

    private void go() {
        switch (args.nextAsString("Missing command")) {
            case "dot":
                dotCommand();
                break;
            case "timings":
                buildAll();
                break;
            default:
                throw new UsageError("Unknown command: " + args.getString());
        }
    }

    private void dotCommand() {
        Path inputPath = null;
        Path outputPath = null;

        while (args.hasMore()) {
            switch (args.nextAsString()) {
                case "-i":
                    inputPath = Path.of(args.nextAsStringOptionValue());
                    continue;
                case "-o":
                    outputPath = Path.of(args.nextAsStringOptionValue());
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

        failIf(args.hasMore(), "Extraneous arguments");

        new DotCommand(inputPath, outputPath, vespaPath).go();
    }

    private void buildAll() {
        Path outputPath = null;

        while (args.hasMore()) {
            switch (args.nextAsString()) {
                case "-o":
                    outputPath = Path.of(args.nextAsStringOptionValue());
                    continue;
                default:
                    if (args.getString().startsWith("-"))
                        throw new UsageError("Unknown option: " + args.getString());
                    // fall through to break
            }
            break;
        }

        failIf(outputPath == null, "-o is required");
        Path finalOutputPath = outputPath;
        failIf(uncheckIO(() -> !Files.isDirectory(finalOutputPath.getParent())), "Directory of output path does not exist: " +
                outputPath.getParent());

        failIf(args.hasMore(), "Extraneous arguments");

        var mvn = new Mvn(JAVA11_HOME, vespaPath);
        mvn.clean();

        Build build = Build.read(vespaPath);

        try (var writer = FileWriter.truncateForWriting(outputPath)) {
            build.buildOrder().forEach(vertex -> {
                Duration elapsedTime = mvn.install(vertex.get().module());
                String serialized = String.format("%s %s %.3f\n",
                                                  vertex.get().module().modulePath(),
                                                  vertex.get().module().coordinate().getGroupArtifact(),
                                                  Math.round(elapsedTime.toNanos() / 1_000_000.0) / 1_000.0);
                writer.write(serialized);
            });
        }
    }
}
