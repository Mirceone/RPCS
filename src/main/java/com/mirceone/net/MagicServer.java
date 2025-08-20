package com.mirceone.net;

import com.mirceone.core.*;
import static com.mirceone.core.Log.*;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Locale;

public final class MagicServer implements Runnable {
    private final CommandRegistry registry;
    private final int port;
    private final String secret;
    private final ReplayCache cache;
    private volatile boolean running = true;

    public MagicServer(CommandRegistry registry) {
        this.registry = registry;
        this.port = Config.port();
        this.secret = Config.secret();
        this.cache = new ReplayCache(Config.clockSkewSeconds() * 2L);
    }

    public void shutdown() { running = false; }

    @Override
    public void run() {
        if (secret == null || secret.isBlank()) {
            warn("[MagicServer] not started: RPCS_SECRET missing.");
            return;
        }
        try (DatagramSocket sock = new DatagramSocket(new InetSocketAddress(port))) {
            sock.setSoTimeout(2000); // short poll so we can exit promptly
            info("[MagicServer] listening UDP port " + port);
            byte[] buf = new byte[2048];
            while (running) {
                try {
                    DatagramPacket pkt = new DatagramPacket(buf, buf.length);
                    sock.receive(pkt);
                    String msg = new String(pkt.getData(), pkt.getOffset(), pkt.getLength(), StandardCharsets.UTF_8).trim();
                    handle(msg, pkt);
                } catch (java.net.SocketTimeoutException ignored) {
                    // loop to check running flag
                } catch (Exception e) {
                    error("[MagicServer] exception: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            error("[MagicServer] fatal: " + e.getMessage());
        } finally {
            info("[MagicServer] stopped.");
        }
    }

    private void handle(String msg, DatagramPacket pkt) {
        // Expected: RPCS|ts|nonce|cmd|arg1|...|hmacHex
        String[] parts = msg.split("\\|");
        if (parts.length < 5 || !"RPCS".equals(parts[0])) {
            debug("[MagicServer] ignoring junk packet from " + pkt.getAddress());
            return;
        }

        String tsStr = parts[1];
        String nonce = parts[2];
        String cmd = parts[3].toLowerCase(Locale.ROOT);
        String hmacHex = parts[parts.length - 1];
        String[] args = (parts.length > 5)
                ? Arrays.copyOfRange(parts, 4, parts.length - 1)
                : new String[0];

        long ts;
        try { ts = Long.parseLong(tsStr); } catch (NumberFormatException e) {
            warn("[MagicServer] bad ts from " + pkt.getAddress());
            return;
        }

        long now = ReplayCache.nowEpochSeconds();
        int skew = Config.clockSkewSeconds();
        if (Math.abs(now - ts) > skew) {
            warn("[MagicServer] drop (skew) from " + pkt.getAddress() +
                    " cmd=" + cmd + " ts=" + ts + " now=" + now);
            return;
        }
        if (!cache.accept(nonce)) {
            warn("[MagicServer] drop (replay) from " + pkt.getAddress() +
                    " nonce=" + nonce);
            return;
        }

        // Verify HMAC
        String canonical = canonicalize(parts);
        String expected = Crypto.hmacSha256Hex(secret, canonical);
        if (!expected.equalsIgnoreCase(hmacHex)) {
            warn("[MagicServer] drop (bad hmac) from " + pkt.getAddress() +
                    " cmd=" + cmd);
            return;
        }

        // Dispatch
        info("[MagicServer] dispatch from " + pkt.getAddress() + ":" + pkt.getPort() +
                " cmd=" + cmd + " args=" + Arrays.toString(args));
        boolean known = registry.runByName(cmd, args);
        if (!known) {
            warn("[MagicServer] unknown cmd=" + cmd + " from " + pkt.getAddress());
        }
    }

    /** Join everything up to (but excluding) the last field (hmac). */
    private static String canonicalize(String[] parts) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length - 1; i++) {
            if (i > 0) sb.append('|');
            sb.append(parts[i]);
        }
        return sb.toString();
    }
}
