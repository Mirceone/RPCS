package com.mirceone.core;

import java.io.IOException;

import static com.mirceone.core.Log.*;

public final class Exec {
    private Exec() {}

    public static void run(String... cmd)
            throws IOException, InterruptedException, CommandFailedException {
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
        pb.redirectError(ProcessBuilder.Redirect.INHERIT);
        Process p = pb.start();
        int code = p.waitFor();
        if (code != 0) {
            throw new CommandFailedException("Command failed (exit " + code + "): "
                    + String.join(" ", cmd), code);
        }
        info("exec ok: " + String.join(" ", cmd));
    }

    public static final class CommandFailedException extends Exception {
        public final int exitCode;
        public CommandFailedException(String msg, int exitCode) { super(msg); this.exitCode = exitCode; }
    }
}