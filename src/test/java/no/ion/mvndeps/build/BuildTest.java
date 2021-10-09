package no.ion.mvndeps.build;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BuildTest {
    private final Path projectDirectory = Path.of(".");

    @Test
    void buildAll() {
        // Building this project would require running this test, which requires building the project, which...
        //BuildMain.main("timings", "-p", projectDirectory.toString(), "-o", "target/build-timings.txt");
    }

    @Test
    void test() {
        String dot = Build.read(projectDirectory).toDot("build");
        assertEquals("""
                     digraph build {
                       "no.ion:mvndeps"
                     }
                     """,
                     dot);
    }

}