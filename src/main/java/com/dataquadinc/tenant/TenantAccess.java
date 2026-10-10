package com.dataquadinc.tenant;

/**
 * IDOR guard: treat blank/null tenant_id as the default (mymulya) after backfill.
 */
public final class TenantAccess {

    private TenantAccess() {
    }

    public static boolean isForeignTenant(String entityTenantId) {
        String entity = (entityTenantId == null || entityTenantId.isBlank())
                ? TenantContext.DEFAULT_TENANT
                : entityTenantId.trim();
        return !entity.equalsIgnoreCase(TenantContext.getTenantId());
    }
}
