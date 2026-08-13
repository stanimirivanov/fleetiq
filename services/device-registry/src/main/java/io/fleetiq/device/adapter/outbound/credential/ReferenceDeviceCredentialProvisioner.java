package io.fleetiq.device.adapter.outbound.credential;

import io.fleetiq.device.domain.port.outbound.DeviceCredentialProvisioner;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Reference credential provider for local development. It demonstrates the
 * one-time-secret contract and retains only salted PBKDF2 verifiers. Production
 * deployments replace this adapter with their broker or secrets platform.
 */
@ApplicationScoped
public class ReferenceDeviceCredentialProvisioner implements DeviceCredentialProvisioner {

    private static final int SECRET_BYTES = 32;
    private static final int SALT_BYTES = 16;
    private static final int ITERATIONS = 210_000;
    private static final int KEY_BITS = 256;

    private final SecureRandom random = new SecureRandom();
    private final Map<String, String> verifiers = new ConcurrentHashMap<>();

    @ConfigProperty(name = "fleetiq.device-credentials.reference-enabled", defaultValue = "true")
    boolean enabled;

    @Override
    public Uni<ProvisionedCredential> provision(String tenantId, String vin) {
        return Uni.createFrom().item(() -> {
            if (!enabled) {
                throw new IllegalStateException(
                    "No production device credential provider is configured");
            }
            String username = tenantId + "/" + vin;
            String secret = randomToken(SECRET_BYTES);
            byte[] salt = randomBytes(SALT_BYTES);
            verifiers.put(username, encodeVerifier(secret, salt));
            return new ProvisionedCredential(username, secret);
        }).runSubscriptionOn(Infrastructure.getDefaultWorkerPool());
    }

    private String randomToken(int bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes(bytes));
    }

    private byte[] randomBytes(int size) {
        byte[] value = new byte[size];
        random.nextBytes(value);
        return value;
    }

    private String encodeVerifier(String secret, byte[] salt) {
        try {
            var spec = new PBEKeySpec(secret.toCharArray(), salt, ITERATIONS, KEY_BITS);
            byte[] hash = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                .generateSecret(spec).getEncoded();
            return ITERATIONS + "$" + Base64.getEncoder().encodeToString(salt) + "$"
                + Base64.getEncoder().encodeToString(hash);
        } catch (GeneralSecurityException failure) {
            throw new IllegalStateException("PBKDF2 is unavailable", failure);
        }
    }
}
