package no.ion.mvndeps.dot;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NodeStatementTest {
    @Test
    void nodeStatement() {
        assertNodeStatement("  a\n", "a");
        assertNodeStatement("  \"with spaces\"\n", "with spaces");
        assertNodeStatement("  a [b=c]\n", "a", "b=c");
        assertNodeStatement("  a [b=c,d=e]\n", "a", "b=c", "d=e");
    }

    private void assertNodeStatement(String expectedDot,
                                     String id, String... attributes) {
        List<DotAttribute> attributeList = Stream.of(attributes)
                                                 .map(attribute -> {
                                                     int i = attribute.indexOf('=');
                                                     String name = attribute.substring(0, i);
                                                     String value = attribute.substring(i + 1);
                                                     return DotAttribute.from(name, value);
                                                 })
                                                 .collect(Collectors.toList());

        var statement = new NodeStatement(DotId.from(id), attributeList);
        DotWriter writer = new DotWriter();
        writer.indent();
        statement.render(writer);
        assertEquals(expectedDot, writer.toString());
    }
}