package io.fleetiq.security;

import jakarta.enterprise.context.RequestScoped;

/**
 * Holds the authenticated tenant identity for the lifetime of one inbound request.
 * Application code uses this abstraction instead of reading transport-specific
 * credentials or security metadata.
 */
@RequestScoped
public class CurrentTenant {
    private TenantIdentity identity;

    /**
     * Returns the identity established by the security interceptor.
     *
     * @throws MissingTenantException when called outside an authenticated tenant context
     */
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
