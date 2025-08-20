package com.mirceone.core;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

public final class Env {
    private Env() {}

    public static boolean isLinux() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        return os.contains("linux");
    }

    public static boolean commandExists(String name) {
        String path = System.getenv("PATH");
        if (path == null) return false;
        for (String dir : path.split(":")) {
            if (Files.isExecutable(Path.of(dir, name))) return true;
        }
        return false;
    }
}
