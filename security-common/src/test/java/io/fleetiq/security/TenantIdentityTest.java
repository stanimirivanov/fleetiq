package io.fleetiq.security;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TenantIdentityTest {
    @Test
    void capturesImmutableAuthenticatedTenant() {
        var identity = new TenantIdentity("acme", "service-account", Set.of("service"));
        assertEquals("acme", identity.tenantId());
        assertEquals(Set.of("service"), identity.roles());
    }

    @Test
    void rejectsMissingTenantAndSubject() {
        assertThrows(MissingTenantException.class,
            () -> new TenantIdentity("", "subject", Set.of()));
        assertThrows(MissingTenantException.class,
            () -> new TenantIdentity("acme", "", Set.of()));
    }
}
