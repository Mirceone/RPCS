package com.mirceone.core;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.*;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.List;

public final class Log {
    private static final int DEFAULT_CAP = 1000;
    private static final DateTimeFormatter TS = DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    private static final ArrayDeque<String> RING = new ArrayDeque<>(DEFAULT_CAP);
    private static final Object LOCK = new Object();

    // File path can be overridden with RPCS_LOG_FILE
    private static final Path FILE = resolveLogPath();

    private Log() {}

    public static void debug(String msg) { append("DEBUG", msg); }
    public static void info(String msg)  { append("INFO", msg);  }
    public static void warn(String msg)  { append("WARN", msg);  }
    public static void error(String msg) { append("ERROR", msg); }

    private static void append(String level, String msg) {
        String line = String.format("%s [%s] %s", TS.format(Instant.now()), level, msg);
        synchronized (LOCK) {
            if (RING.size() == DEFAULT_CAP) RING.removeFirst();
            RING.addLast(line);
            if (FILE != null) writeLine(FILE, line);
        }
    }

    public static List<String> tail(int n) {
        synchronized (LOCK) {
            int size = RING.size();
            int start = Math.max(0, size - n);
            return RING.stream().skip(start).toList();
        }
    }

    private static void writeLine(Path file, String line) {
        try {
            Files.createDirectories(file.getParent());
            try (PrintWriter out = new PrintWriter(Files.newBufferedWriter(
                    file, StandardOpenOption.CREATE, StandardOpenOption.APPEND))) {
                out.println(line);
            }
        } catch (IOException ignored) {
            // swallow file I/O errors; in-memory ring still works
        }
    }

    private static Path resolveLogPath() {
        String p = System.getenv("RPCS_LOG_FILE");
        if (p != null && !p.isBlank()) return Paths.get(p);
        String home = System.getProperty("user.home");
        if (home == null || home.isBlank()) return null;
        return Paths.get(home, ".local", "share", "rpcs", "rpcs.log");
    }
}
