package io.fleetiq.security;

import io.quarkus.security.ForbiddenException;

public class MissingTenantException extends ForbiddenException {
    public MissingTenantException(String message) {
        super(message);
    }
}
