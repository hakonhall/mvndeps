package no.ion.mvndeps;

import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;

public class Main {
    private final FileSystem fileSystem;
    private final String[] args;
    private int argi = 0;

    public Main(FileSystem fileSystem, String... args) {
        this.fileSystem = fileSystem;
        this.args = args;
    }

    public static void main(String... args) {
        var main = new Main(FileSystems.getDefault(), args);

        try {
            main.doMain();
        } catch (ErrorMessage e) {
            System.err.println(e.getMessage());
            System.exit(1);
        } catch (RuntimeException e) {
            e.printStackTrace();
            System.exit(1);
        }

        System.exit(0);
    }

    public void doMain() {
        Path projectRootDirectory = null;
        Path dotPath = null;

        for (; argi < args.length; ++argi) {
            switch (arg()) {
                case "-d":
                    dotPath = fileSystem.getPath(nextArgAsOptionValue());
                    break;
                case "-p":
                    projectRootDirectory = fileSystem.getPath(nextArgAsOptionValue());
                    break;
                default:
                    if (arg().startsWith("-")) {
                        throw new ErrorMessage("Unknown option: '" + arg() + "'");
                    }
                    break;
            }
        }

        if (dotPath == null) {
            throw new UserError("-d is required");
        } else if (!Files.isDirectory(dotPath.getParent())) {
            throw new UserError("Unable to write dot file: No such directory: " + dotPath.getParent());
        }

        if (projectRootDirectory == null) {
            throw new UserError("-p is required");
        }

        if (argi < args.length) {
            throw new UserError("Too many arguments");
        }

        RunGraph.compute(projectRootDirectory, dotPath);
    }

    private String arg() { return args[argi]; }

    private String nextArgAsOptionValue() {
        String option = arg();

        if (++argi >= args.length) {
            throw new UserError("Missing argument to option '" + option + "'");
        }

        return arg();
    }
}
