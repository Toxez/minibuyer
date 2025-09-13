package ua.vdev.minibuyer.commands.types;

import java.util.Arrays;

public enum CommandType {
    RELOAD("reload"),
    LEVEL("level"),
    ITEMS("items");

    private final String command;

    CommandType(String command) {
        this.command = command;
    }

    public String getCommand() {
        return command;
    }

    public static CommandType fromString(String value) {
        return Arrays.stream(values())
                .filter(type -> type.command.equalsIgnoreCase(value))
                .findFirst()
                .orElse(null);
    }
}