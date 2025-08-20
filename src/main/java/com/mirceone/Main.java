package com.mirceone;

import com.mirceone.core.CommandRegistry;
import com.mirceone.core.Env;
import com.mirceone.core.Config;
import com.mirceone.core.Log;
import com.mirceone.net.MagicServer;

import java.util.*;

import static com.mirceone.core.Log.*;

public class Main {
    private static volatile boolean running = true;
    private static volatile boolean showLogsAfterCommand = false; // toggle via `logmode on|off`

    public static void main(String[] args) {
        if (!Env.isLinux()) {
            warn("Non-Linux OS detected. `systemctl` calls may fail.");
        }
        if (!Env.commandExists("systemctl")) {
            error("`systemctl` not found in PATH. Aborting.");
            System.exit(127);
        }

        CommandRegistry registry = new CommandRegistry();

        // --- start UDP magic server if secret is configured ---
        MagicServer server = null;
        Thread serverThread = null;
        String secret = Config.secret(); // warns in Config if missing
        if (secret != null && !secret.isBlank()) {
            server = new MagicServer(registry);
            serverThread = new Thread(server, "rpcs-magic-udp");
            serverThread.setDaemon(true);
            serverThread.start();
            info("MagicServer thread started.");
        } else {
            info("MagicServer not started (RPCS_SECRET missing).");
        }

        // --- shutdown handling ---
        MagicServer finalServer = server;
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            running = false;
            if (finalServer != null) finalServer.shutdown();
            info("Shutting down daemon...");
        }));

        // --- one-shot flags: --<command> [args...] (e.g., --suspend) ---
        if (args.length >= 1 && args[0].startsWith("--")) {
            String cmd = args[0].substring(2).toLowerCase(Locale.ROOT);
            String[] rest = Arrays.copyOfRange(args, 1, args.length);
            info("One-shot flag received: --" + cmd + " " + String.join(" ", rest));
            boolean known = registry.runByName(cmd, rest);
            if (!known) {
                error("Unknown command flag: --" + cmd);
                printUsageAndExit(registry, 1);
            } else if (showLogsAfterCommand || "1".equals(System.getenv("RPCS_SHOW_LOGS"))) {
                printTail(20);
            }
            return;
        }

        // --- interactive REPL ---
        printBanner(registry, server != null);
        try (Scanner sc = new Scanner(System.in)) {
            while (running) {
                System.out.print("[rpcs-daemon] > ");
                if (!sc.hasNextLine()) break;
                String line = sc.nextLine().trim();
                if (line.isEmpty()) continue;

                String[] parts = line.split("\\s+");
                String cmd = parts[0].toLowerCase(Locale.ROOT);
                String[] rest = Arrays.copyOfRange(parts, 1, parts.length);

                switch (cmd) {
                    case "help" -> printBanner(registry, server != null);
                    case "clear" -> clearScreen();
                    case "logs" -> {
                        int n = 50;
                        if (rest.length == 1) {
                            try { n = Integer.parseInt(rest[0]); } catch (NumberFormatException ignored) {}
                        }
                        printTail(n);
                    }
                    case "logmode" -> {
                        if (rest.length == 1) {
                            showLogsAfterCommand = rest[0].equalsIgnoreCase("on");
                            info("logmode: " + (showLogsAfterCommand ? "on" : "off"));
                            System.out.println("logmode: " + (showLogsAfterCommand ? "on" : "off"));
                        } else {
                            System.out.println("Usage: logmode on|off");
                        }
                    }
                    case "exit", "quit" -> { running = false; info("Bye."); System.out.println("Bye."); }
                    default -> {
                        info("Command: " + cmd + " " + String.join(" ", rest));
                        boolean known = registry.runByName(cmd, rest);
                        if (!known) {
                            System.out.println("Unknown command. Type `help`.");
                            warn("Unknown command: " + cmd);
                        } else if (showLogsAfterCommand) {
                            printTail(20);
                        }
                    }
                }
            }
        }
    }

    private static void printBanner(CommandRegistry reg, boolean listening) {
        System.out.println("RPCS Daemon (stdin mode)");
        System.out.println(listening
                ? "  [MagicServer] Listening for packets on UDP port " + Config.port()
                : "  [MagicServer] Not running (RPCS_SECRET missing)");
        System.out.println("\nCommands:");
        System.out.print(reg.helpText());
        System.out.println("""
                Other:
                  help            - show this help
                  clear           - clear screen
                  logs [N]        - show last N log lines (default 50)
                  logmode on|off  - auto-show last 20 lines after each command
                  exit            - stop daemon

                One-shot usage:
                  rpcs --suspend
                  rpcs --reboot
                  rpcs --poweroff
                """);
    }

    private static void printUsageAndExit(CommandRegistry reg, int code) {
        System.out.println("Usage:\n  rpcs --<command> [args...]\nCommands:");
        System.out.print(reg.helpText());
        System.exit(code);
    }

    private static void printTail(int n) {
        var lines = Log.tail(n);
        if (lines.isEmpty()) {
            System.out.println("(no logs yet)");
            return;
        }
        System.out.println("---- logs (last " + lines.size() + ") ----");
        for (String s : lines) System.out.println(s);
        System.out.println("---- end ----");
    }

    private static void clearScreen() {
        try {
            if (System.getProperty("os.name").toLowerCase().contains("win")) {
                new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
            } else {
                new ProcessBuilder("clear").inheritIO().start().waitFor();
            }
        } catch (Exception e) {
            System.out.println("\n".repeat(50));
        }
    }
}
