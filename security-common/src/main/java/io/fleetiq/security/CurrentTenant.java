package io.fleetiq.security;

import jakarta.enterprise.context.RequestScoped;

@RequestScoped
public class CurrentTenant {
    private TenantIdentity identity;

    public TenantIdentity get() {
        if (identity == null) {
            throw new MissingTenantException("Tenant identity has not been established for this request");
        }
        return identity;
    }

    void set(TenantIdentity identity) {
        this.identity = identity;
    }
}
