package com.mirceone.core;

import java.util.*;

public final class CommandRegistry {
    private final Map<String, Command> byName = new LinkedHashMap<>();

    public CommandRegistry() {
        // auto-discover commands with ServiceLoader
        ServiceLoader<Command> loader = ServiceLoader.load(Command.class);
        for (Command c : loader) register(c);
    }

    public void register(Command cmd) {
        String name = cmd.name().toLowerCase(Locale.ROOT);
        if (byName.containsKey(name)) {
            throw new IllegalArgumentException("Duplicate command: " + name);
        }
        byName.put(name, cmd);
        for (String alias : cmd.aliases()) {
            String a = alias.toLowerCase(Locale.ROOT);
            if (byName.containsKey(a)) {
                throw new IllegalArgumentException("Duplicate alias: " + alias);
            }
            byName.put(a, cmd);
        }
    }

    public boolean runByName(String name, String[] args) {
        Command cmd = byName.get(name.toLowerCase(Locale.ROOT));
        if (cmd == null) return false;
        try {
            cmd.run(args);
        } catch (Exec.CommandFailedException e) {
            System.err.println("[ERROR] " + e.getMessage());
            if (e.exitCode == 1) System.err.println("Tip: May require sudo/polkit.");
        } catch (Exception e) {
            System.err.println("[ERROR] " + e.getMessage());
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
        }
        return true;
    }

    public String helpText() {
        // deduplicate: prefer canonical names
        LinkedHashSet<Command> uniq = new LinkedHashSet<>(byName.values());
        StringBuilder sb = new StringBuilder();
        for (Command c : uniq) {
            String aliases = c.aliases().isEmpty() ? "" : " (aliases: " + String.join(", ", c.aliases()) + ")";
            sb.append(String.format("  %-10s - %s%s%n", c.name(), c.description(), aliases));
        }
        return sb.toString();
    }
}
