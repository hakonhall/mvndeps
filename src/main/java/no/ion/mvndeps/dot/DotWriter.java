package no.ion.mvndeps.dot;

class DotWriter {
    private final StringBuilder stringBuilder = new StringBuilder();

    private boolean needIndent = true;
    private int indentationLevel = 0;
    private int spaces = 2;
    private String indentation = "";

    DotWriter() {}

    DotWriter append(char c) {
        indentIfNecessary();
        stringBuilder.append(c);
        return this;
    }

    DotWriter append(String string) {
        indentIfNecessary();
        stringBuilder.append(string);
        return this;
    }

    DotWriter appendln(char c) {
        indentIfNecessary();
        stringBuilder.append(c);
        appendln();
        return this;
    }

    DotWriter appendln(String string) {
        indentIfNecessary();
        stringBuilder.append(string);
        appendln();
        return this;
    }

    DotWriter appendln() {
        stringBuilder.append('\n');
        needIndent = true;
        return this;
    }

    DotWriter indent() { return setIndentation(++indentationLevel); }
    DotWriter outdent() { return setIndentation(--indentationLevel); }

    @Override
    public String toString() { return stringBuilder.toString(); }

    private DotWriter setIndentation(int indentationLevel) {
        indentation = " ".repeat(spaces * indentationLevel);
        return this;
    }

    private void indentIfNecessary() {
        if (needIndent) {
            stringBuilder.append(indentation);
            needIndent = false;
        }
    }
}
