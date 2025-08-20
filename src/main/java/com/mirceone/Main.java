package com.mirceone;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Scanner;

public class Main {

    private static volatile boolean running = true;

    public static void main(String[] args) {
        // Basic env guardrails
        if (!isLinux()) {
            System.err.println("[WARN] Non-Linux OS detected. `systemctl` calls will likely fail.");
        }
        if (!commandExists("systemctl")) {
            System.err.println("[ERROR] `systemctl` not found in PATH. Aborting.");
            System.exit(127);
        }

        // Shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            running = false;
            System.out.println("Shutting down daemon...");
        }));

        // One-shot flags
        if (args.length == 1) {
            switch (args[0]) {
                case "--suspend" -> { runAction("suspend"); return; }
                case "--reboot"  -> { runAction("reboot");  return; }   // optional
                case "--poweroff"-> { runAction("poweroff");return; }   // optional
                default -> { /* fall through to interactive mode */ }
            }
        }

        // Interactive loop
        printBanner();
        try (Scanner scanner = new Scanner(System.in)) {
            while (running) {
                System.out.print("[rpcs-daemon] > ");
                if (!scanner.hasNextLine()) break;
                String line = scanner.nextLine().trim().toLowerCase(Locale.ROOT);
                if (line.isEmpty()) continue;

                switch (line) {
                    case "reboot"   -> runAction("reboot");
                    case "poweroff" -> runAction("poweroff");
                    case "suspend"  -> runAction("suspend");
                    case "test"  -> runAction("devTest");
                    case "help"     -> printHelp();
                    case "exit", "quit" -> {
                        running = false;
                        System.out.println("Bye.");
                    }
                    default -> System.out.println("Unknown command. Type `help`.");
                }
            }
        }
    }

    private static void runAction(String action) {
        try {
            switch (action) {
                case "reboot"   -> CommandRunner.run("systemctl", "reboot");
                case "poweroff" -> CommandRunner.run("systemctl", "poweroff");
                case "suspend"  -> CommandRunner.run("systemctl", "suspend");
                case "devTest"  -> CommandRunner.run("echo", "dev Test working ;)");
                default -> {
                    System.out.println("Unknown action: " + action);
                    return;
                }
            }
        } catch (IOException e) {
            System.err.println("[ERROR] Failed to start command: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("[ERROR] Command interrupted.");
        } catch (CommandFailedException e) {
            System.err.println("[ERROR] " + e.getMessage());
            if (e.exitCode == 1) {
                System.err.println("Tip: This may require elevated privileges (sudo/polkit).");
            }
        }
    }

    private static void printBanner() {
        System.out.println("""
                RPCS Daemon (stdin mode)
                Commands:
                  reboot    - systemctl reboot
                  poweroff  - systemctl poweroff
                  suspend   - systemctl suspend
                  help      - show this help
                  exit      - stop daemon
                One-shot usage:
                  java -jar rpcs-daemon-0.1.0.jar --suspend
                """);
    }

    private static void printHelp() { printBanner(); }

    private static boolean isLinux() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        return os.contains("linux");
    }

    private static boolean commandExists(String name) {
        // quick PATH lookup
        String path = System.getenv("PATH");
        if (path == null) return false;
        for (String dir : path.split(":")) {
            Path p = Path.of(dir, name);
            if (Files.isExecutable(p)) return true;
        }
        return false;
    }

    // --- Simple process runner with clear errors
    static class CommandRunner {
        static void run(String... cmd) throws IOException, InterruptedException, CommandFailedException {
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
            pb.redirectError(ProcessBuilder.Redirect.INHERIT);
            Process p = pb.start();
            int code = p.waitFor();
            if (code != 0) {
                throw new CommandFailedException("Command failed (exit " + code + "): " + String.join(" ", cmd), code);
            }
        }
    }

    static class CommandFailedException extends Exception {
        final int exitCode;
        CommandFailedException(String msg, int exitCode) {
            super(msg);
            this.exitCode = exitCode;
        }
    }
}
