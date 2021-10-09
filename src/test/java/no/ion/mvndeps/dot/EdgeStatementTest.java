package no.ion.mvndeps.dot;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class EdgeStatementTest {
    @Test
    void edgeStatement() {
        assertEdgeStatement("  a -> b\n", "a", "b");
        assertEdgeStatement("  \"with spaces\" -> \"this too\"\n", "with spaces", "this too");
        assertEdgeStatement("  a -> b [c=d]\n", "a", "b", "c=d");
        assertEdgeStatement("  a -> b [c=d,e=f]\n", "a", "b", "c=d", "e=f");
    }

    private void assertEdgeStatement(String expectedDot,
                                     String from, String to, String... attributes) {
        List<DotAttribute> attributeList = Stream.of(attributes)
                                                 .map(attribute -> {
                                                     int i = attribute.indexOf('=');
                                                     String name = attribute.substring(0, i);
                                                     String value = attribute.substring(i + 1);
                                                     return DotAttribute.from(name, value);
                                                 })
                                                 .collect(Collectors.toList());

        var statement = new EdgeStatement(DotId.from(from), DotId.from(to), attributeList);
        DotWriter writer = new DotWriter();
        writer.indent();
        statement.render(writer);
        assertEquals(expectedDot, writer.toString());
    }
}
