package no.ion.mvndeps;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static no.ion.mvndeps.Exceptions.uncheck;
import static no.ion.mvndeps.Exceptions.uncheckIO;

public class Mvn {
    private final Path projectDirectory;
    private final Path m2Repository;
    private final Path java11Home;

    public Mvn(Path java11Home, Path projectDirectory) {
        this.java11Home = uncheck(java11Home::toAbsolutePath);
        this.projectDirectory = uncheck(projectDirectory::toAbsolutePath);
        this.m2Repository = projectDirectory.resolve(".m2/repository");
    }

    public void clean() {
        mvn("clean");
    }

    /** Maven install the module, and return the elapsed time. */
    public Duration install(MavenModule module) {
        return mvn("install", "-pl", ":" + module.coordinate().artifactId());
    }

    private Duration mvn(String... additionalArguments) {
        List<String> command = new ArrayList<>();
        command.add("mvn");
        command.add("-Dmaven.repo.local=" + m2Repository);
        command.add("-B");
        command.add("-T1");
        command.addAll(List.of(additionalArguments));
        return exec(command);
    }

    private Duration exec(List<String> command) {
        System.out.println(command);

        ProcessBuilder processBuilder = new ProcessBuilder(command)
                .inheritIO()
                .redirectErrorStream(true)
                .directory(projectDirectory.toFile());

        Map<String, String> environment = processBuilder.environment();
        environment.put("JAVA_HOME", java11Home.toString());

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
}
