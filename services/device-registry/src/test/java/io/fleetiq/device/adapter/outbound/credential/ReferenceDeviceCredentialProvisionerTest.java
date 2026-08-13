package io.fleetiq.device.adapter.outbound.credential;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ReferenceDeviceCredentialProvisionerTest {

    @Test
    void returnsTenantAndVinPrincipalAndRotatesTheOneTimeSecret() {
        var provider = new ReferenceDeviceCredentialProvisioner();
        provider.enabled = true;

        var first = provider.provision("tenant-a", "1HGCM82633A004352").await().indefinitely();
        var rotated = provider.provision("tenant-a", "1HGCM82633A004352").await().indefinitely();

        assertEquals("tenant-a/1HGCM82633A004352", first.username());
        assertEquals(first.username(), rotated.username());
        assertNotEquals(first.secret(), rotated.secret());
    }

    @Test
    void failsClosedWhenReferenceProviderIsDisabled() {
        var provider = new ReferenceDeviceCredentialProvisioner();
        provider.enabled = false;

        assertThrows(IllegalStateException.class,
            () -> provider.provision("tenant-a", "1HGCM82633A004352").await().indefinitely());
    }
}
