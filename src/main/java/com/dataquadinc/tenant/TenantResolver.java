package com.dataquadinc.tenant;

public final class TenantResolver {

    public static final String AVENTRA = "aventra";

    private TenantResolver() {
    }

    public static String fromHost(String host) {
        if (host == null || host.isBlank()) {
            return null;
        }
        String h = host.trim().toLowerCase();
        if (h.equals("aventrainc.ai") || h.endsWith(".aventrainc.ai")) {
            return AVENTRA;
        }
        if (h.startsWith("aventra.")) {
            return AVENTRA;
        }
        return null;
    }
}
