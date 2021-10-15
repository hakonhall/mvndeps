package no.ion.mvndeps;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MainTest {
    @Test
    void buildOrder() {
        new Main("build-order", "-p", ".").go();
    }
}