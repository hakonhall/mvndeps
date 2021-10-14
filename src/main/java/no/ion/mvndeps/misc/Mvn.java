package no.ion.mvndeps.misc;

import no.ion.mvndeps.maven.MavenModule;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static no.ion.mvndeps.misc.Exceptions.uncheck;
import static no.ion.mvndeps.misc.Exceptions.uncheckIO;

public class Mvn {
    private final Path projectDirectory;
    private final Optional<Path> m2Repository;
    private final Optional<Path> javaHome;

    public Mvn(Path java11HomeOrNull, Path projectDirectory, Path mavenRepoLocal) {
        this.javaHome = Optional.ofNullable(java11HomeOrNull).map(j -> uncheck(j::toAbsolutePath));
        this.projectDirectory = uncheck(projectDirectory::toAbsolutePath);
        this.m2Repository = Optional.ofNullable(mavenRepoLocal);
    }

    public void clean() {
        mvn("clean");
    }

    /** Maven install the module, and return the elapsed time. */
    public Duration install(Path relativeModulePath) {
        return mvn("install", "-pl", relativeModulePath.toString());
    }

    private Duration mvn(String... additionalArguments) {
        List<String> command = new ArrayList<>();
        command.add("mvn");
        m2Repository.ifPresent(r -> command.add("-Dmaven.repo.local=" + r));
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
        javaHome.ifPresent(j -> environment.put("JAVA_HOME", j.toString()));

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
