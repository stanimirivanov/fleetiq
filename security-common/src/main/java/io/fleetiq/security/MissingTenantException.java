package io.fleetiq.security;

import io.quarkus.security.ForbiddenException;

/**
 * Indicates that authentication succeeded but did not provide usable tenant identity.
 * It is a forbidden response rather than a validation error because tenant selection is part
 * of the trusted security context, not user-supplied domain data.
 */
public class MissingTenantException extends ForbiddenException {
    public MissingTenantException(String message) {
        super(message);
    }
}
