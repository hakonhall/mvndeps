package no.ion.mvndeps.dot;

import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

import static java.util.Objects.requireNonNull;

/**
 * A DotId represents an ID in the DOT language, quoted if necessary.
 */
public class DotId {
    private final String id;

    public static DotId from(String rawId) { return new DotId(maybeQuote(rawId)); }
    public static DotId fromQuoted(String id) { return new DotId(id); }

    private DotId(String id) {
        this.id = requireNonNull(id);
    }

    @Override
    public String toString() { return id; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DotId dotId = (DotId) o;
        return id.equals(dotId.id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }

    private static final Pattern STRING_ID = Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_]*$");
    private static final Pattern NUMERAL_ID = Pattern.compile("^[-]?(.[0-9]*|[0-9]*(.[0-9]*)?)$");
    private static Set<String> KEYWORDS = Set.of("node", "edge", "graph", "digraph", "subgraph", "strict");

    /** Quote id, if necessary, to make it a valid DOT ID. */
    public static String maybeQuote(String id) {
        requireNonNull(id);

        if (!KEYWORDS.contains(id.toLowerCase())) {
            if (STRING_ID.matcher(id).find()) {
                return id;
            }

            if (NUMERAL_ID.matcher(id).find()) {
                return id;
            }

            // Ignoring HTML string: <...>
        }

        return '"' + id.replace("\"", "\\\"") + '"';
    }
}
