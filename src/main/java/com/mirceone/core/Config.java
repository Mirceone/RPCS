package com.mirceone.core;

public class Config {
    private Config() {}

    public static int port() {
        String v = System.getenv("RPCS_PORT");
        if (v == null|| v.isEmpty()) return 9097;

        try { return Integer.parseInt(v.trim()); } catch (NumberFormatException e) { return 9097; }
    }

    public static String secret() {
        String v = System.getenv("RPCS_SECRET");
        if (v == null || v.isBlank()) {
            System.err.println("RPCS_SECRET is null or empty");
            return null;
        }
        return v;
    }

    public static int clockSkewSeconds() {
        String v = System.getenv("RPCS_SKEW");
        if (v == null || v.isBlank()) return 60; // default ±60s
        try { return Integer.parseInt(v.trim()); } catch (NumberFormatException e) { return 60; }
    }
}
