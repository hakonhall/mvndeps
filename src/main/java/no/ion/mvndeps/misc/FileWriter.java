package no.ion.mvndeps.misc;

import java.io.BufferedWriter;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import static no.ion.mvndeps.misc.Exceptions.uncheckIO;

public class FileWriter implements AutoCloseable {
    private final BufferedWriter writer;

    public static FileWriter truncateForWriting(Path path) {
        var writer = uncheckIO(() -> Files.newBufferedWriter(path, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING));
        return new FileWriter(writer);
    }

    private FileWriter(BufferedWriter writer) {
        this.writer = writer;
    }

    public FileWriter writeln(String string) {
        return write(string + "\n");
    }

    public FileWriter write(String string) {
        uncheckIO(() -> writer.write(string));
        uncheckIO(writer::flush);
        return this;
    }

    @Override
    public void close() throws UncheckedIOException {
        uncheckIO(writer::close);;
    }
}
