package io.fleetiq.security;

import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.UnauthorizedException;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;
import org.eclipse.microprofile.jwt.JsonWebToken;

@TenantSecured
@Interceptor
@Priority(Interceptor.Priority.PLATFORM_BEFORE + 50)
public class TenantSecurityInterceptor {

    @Inject SecurityIdentity securityIdentity;
    @Inject JsonWebToken jwt;
    @Inject CurrentTenant currentTenant;

    @AroundInvoke
    Object establishTenant(InvocationContext context) throws Exception {
        if (securityIdentity.isAnonymous()) {
            throw new UnauthorizedException("Authentication is required");
        }
        String tenantId = jwt.getClaim("tenant_id");
        currentTenant.set(TenantIdentity.of(
            tenantId, securityIdentity.getPrincipal(), securityIdentity.getRoles()));
        return context.proceed();
    }
}
