package no.ion.mvndeps;

import java.util.Iterator;

public class ProgramArguments {
    private final String[] args;
    private int argi;

    public ProgramArguments(String... args) {
        this.args = args;
        this.argi = 0;
    }

    public boolean atEnd() {
        return argi >= args.length;
    }

    public void next() {
        if (argi < args.length)
            ++argi;
    }

    public String getString() {
        if (atEnd())
            throw new UsageError("Missing arguments");
        return args[argi];
    }

    public String nextAsString() { return nextAsString("Missing arguments"); }
    public String nextAsString(String errorMessageIfNoMoreArguments) {
        if (argi + 1 >= args.length)
            throw new UsageError(errorMessageIfNoMoreArguments);
        return args[++argi];
    }
    public String nextAsStringOptionValue() {
        return nextAsString("Option '" + getString() + "' requires an argument");
    }
}
