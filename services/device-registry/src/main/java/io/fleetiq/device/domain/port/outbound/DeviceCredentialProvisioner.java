package io.fleetiq.device.domain.port.outbound;

import io.smallrye.mutiny.Uni;

/**
 * Provider-neutral boundary for issuing a device credential. Implementations
 * must return the secret only once and retain no recoverable copy of it.
 */
public interface DeviceCredentialProvisioner {

    record ProvisionedCredential(String username, String secret) {}

    /**
     * Issues a credential for one tenant-scoped device. The returned secret is the only clear-text
     * copy exposed to the caller and must not be logged or persisted by the application service.
     */
    Uni<ProvisionedCredential> provision(String tenantId, String vin);
}
