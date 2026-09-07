package io.fleetiq.security;

import java.security.Principal;
import java.util.Set;

/**
 * Immutable identity propagated from the authenticated principal into application services.
 * The tenant and subject are mandatory; roles are defensively copied so authorization checks
 * cannot be affected by later mutation of the security provider's role set.
 */
public record TenantIdentity(String tenantId, String subject, Set<String> roles) {
    public TenantIdentity {
        if (tenantId == null || tenantId.isBlank()) {
            throw new MissingTenantException("Authenticated token must contain a nonblank tenant_id claim");
        }
        if (subject == null || subject.isBlank()) {
            throw new MissingTenantException("Authenticated token must contain a subject");
        }
        roles = roles == null ? Set.of() : Set.copyOf(roles);
    }

    public static TenantIdentity of(String tenantId, Principal principal, Set<String> roles) {
        return new TenantIdentity(tenantId, principal == null ? null : principal.getName(), roles);
    }
}
