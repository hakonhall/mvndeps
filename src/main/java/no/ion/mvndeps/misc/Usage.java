package no.ion.mvndeps.misc;

import no.ion.mvndeps.ProgramArguments;
import no.ion.mvndeps.UsageError;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class Usage {
    private final Map<String, Option<?>> options = new HashMap<>();

    public Usage() {}

    public <T> Option<T> addOption(String name, T defaultValueOrNull, Function<String, T> mapper) {
        return defineOption(name, false, true, defaultValueOrNull, mapper);
    }

    public Option<Boolean> addFlag(String name) {
        return defineOption(name, false, false, false, Boolean::parseBoolean);
    }

    public <T> Option<T> addRequired(String name, Function<String, T> mapper) {
        return defineOption(name, true, true, null, mapper);
    }

    public void readOptions(ProgramArguments args) {
        for (; !args.atEnd(); args.next()) {
            Option<?> option = options.get(args.getString());
            if (option != null) {
                option.setPresent();
                if (option.hasValue()) {
                    if (args.atEnd()) {
                        throw new UsageError("Missing value of option " + args.getString());
                    }
                    option.setValue(args.nextAsString());
                } else {
                    // Default option value is "true"
                    option.setValue("true");
                }
                continue;
            }

            if (args.getString().startsWith("-"))
                throw new UsageError("Unknown option: '" + args.getString() + "'");

            break;
        }

        options.forEach((key, value) -> value.validate());
    }

    private <T> Option<T> defineOption(String name, boolean required, boolean hasValue, T defaultValueOrNull,
                                       Function<String, T> mapper) {
        var option = new Option<T>(name, required, hasValue, defaultValueOrNull, mapper);
        options.put(name, option);
        return option;
    }
}
