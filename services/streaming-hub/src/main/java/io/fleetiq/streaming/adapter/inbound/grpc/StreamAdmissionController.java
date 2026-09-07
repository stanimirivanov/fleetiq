package io.fleetiq.streaming.adapter.inbound.grpc;

import io.fleetiq.security.TenantIdentity;
import io.grpc.Status;
import io.smallrye.mutiny.Multi;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Applies per-principal subscription limits before opening a live stream.
 * Admission is evaluated lazily at subscription time, rejected requests become gRPC failures, and
 * capacity is released on completion, cancellation, failure, or synchronous factory failure.
 */
@ApplicationScoped
public class StreamAdmissionController {

    private final ConcurrentHashMap<String, Integer> activeByPrincipal = new ConcurrentHashMap<>();
    private final int maxConcurrentSubscriptions;
    private final int maxVinsPerSubscription;

    @Inject
    public StreamAdmissionController(
        @ConfigProperty(name = "fleetiq.streaming.max-concurrent-subscriptions", defaultValue = "5")
        int maxConcurrentSubscriptions,
        @ConfigProperty(name = "fleetiq.streaming.max-vins-per-subscription", defaultValue = "500")
        int maxVinsPerSubscription) {
        if (maxConcurrentSubscriptions < 1 || maxVinsPerSubscription < 1) {
            throw new IllegalArgumentException("Streaming limits must be positive");
        }
        this.maxConcurrentSubscriptions = maxConcurrentSubscriptions;
        this.maxVinsPerSubscription = maxVinsPerSubscription;
    }

    /** Wraps a stream factory with per-principal concurrency and selected-VIN limits. */
    public <T> Multi<T> admit(TenantIdentity identity, int selectedVinCount,
                              Supplier<Multi<T>> streamFactory) {
        if (selectedVinCount > maxVinsPerSubscription) {
            return Multi.createFrom().failure(Status.INVALID_ARGUMENT
                .withDescription("Too many VINs in one subscription").asRuntimeException());
        }
        String key = identity.tenantId() + '\u0000' + identity.subject();
        return Multi.createFrom().<T>deferred(() -> {
            int active = activeByPrincipal.merge(key, 1, Integer::sum);
            if (active > maxConcurrentSubscriptions) {
                release(key);
                return Multi.createFrom().failure(Status.RESOURCE_EXHAUSTED
                    .withDescription("Concurrent stream limit exceeded").asRuntimeException());
            }
            try {
                return streamFactory.get().onTermination().invoke(() -> release(key));
            } catch (RuntimeException failure) {
                release(key);
                return Multi.createFrom().failure(failure);
            }
        });
    }

    private void release(String key) {
        activeByPrincipal.computeIfPresent(key, (ignored, active) -> active == 1 ? null : active - 1);
    }
}
