package no.ion.mvndeps.program;

public class ProgramArguments {
    private final String[] args;
    private int argi;

    public ProgramArguments(String... args) {
        this.args = args;
        this.argi = -1;
    }

    public boolean hasMore() {
        return argi + 1 < args.length;
    }

    public String getString() {
        return args[argi];
    }

    public String nextAsString() { return nextAsString("Missing arguments"); }
    public String nextAsString(String errorMessageIfNoMoreArguments) {
        if (!hasMore())
            throw new UsageError(errorMessageIfNoMoreArguments);
        return args[++argi];
    }
    public String nextAsStringOptionValue() {
        return nextAsString("Option '" + getString() + "' requires an argument");
    }

    private void next() {
        if (argi + 1 >= args.length)
            throw new UsageError("Missing arguments");
        ++argi;
    }
}
