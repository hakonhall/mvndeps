package no.ion.mvndeps.maven;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.ReaderFactory;

import java.io.Reader;
import java.nio.file.Path;

import static no.ion.mvndeps.Exceptions.uncheck;
import static no.ion.mvndeps.Exceptions.uncheckIO;

public class PomReader {
    Model readModel(Path directory) {
        Path pomPath = directory.resolve("pom.xml");

        Reader reader = uncheckIO(() -> ReaderFactory.newXmlReader(pomPath.toFile()));

        MavenXpp3Reader mavenXpp3Reader = new MavenXpp3Reader();

        try {
            return uncheck(() -> mavenXpp3Reader.read(reader));
        } finally {
            uncheckIO(reader::close);
        }
    }

    Pom readPom(Path directory) {
        Path pomPath = directory.resolve("pom.xml");

        Reader reader = uncheckIO(() -> ReaderFactory.newXmlReader(pomPath.toFile()));

        MavenXpp3Reader mavenXpp3Reader = new MavenXpp3Reader();

        final Model model;
        try {
            model = uncheck(() -> mavenXpp3Reader.read(reader));
        } finally {
            uncheckIO(reader::close);
        }

        return Pom.fromModel(pomPath, model);
    }
}
