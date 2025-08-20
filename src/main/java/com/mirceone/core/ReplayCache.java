package com.mirceone.core;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ReplayCache {
    private final Map<String, Long> seen = new ConcurrentHashMap<>();
    private final long ttlMillis;

    public ReplayCache(long ttlSeconds) { this.ttlMillis = ttlSeconds * 1000L; }

    /** @return true if nonce is fresh (accepted), false if seen/replayed */
    public boolean accept(String nonce) {
        long now = System.currentTimeMillis();
        purge(now);
        return seen.putIfAbsent(nonce, now) == null;
    }

    private void purge(long now) {
        long cutoff = now - ttlMillis;
        seen.entrySet().removeIf(e -> e.getValue() < cutoff);
    }

    public static long nowEpochSeconds() { return Instant.now().getEpochSecond(); }
}
