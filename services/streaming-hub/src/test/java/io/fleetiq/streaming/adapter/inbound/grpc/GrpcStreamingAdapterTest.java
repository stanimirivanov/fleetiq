package io.fleetiq.streaming.adapter.inbound.grpc;

import io.fleetiq.proto.streaming.v1.WatchVehicleRequest;
import io.fleetiq.security.CurrentTenant;
import io.fleetiq.security.TenantIdentity;
import io.fleetiq.streaming.domain.model.PositionEvent;
import io.fleetiq.streaming.domain.port.inbound.StreamingUseCase;
import io.smallrye.mutiny.Multi;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GrpcStreamingAdapterTest {

    @Test
    void propagatesAuthenticatedTenantToStreamSubscription() {
        AtomicReference<String> tenantSeen = new AtomicReference<>();
        StreamingUseCase useCase = new StreamingUseCase() {
            @Override
            public Multi<PositionEvent> watchFleet(String tenantId, Set<String> vins, Duration interval) {
                tenantSeen.set(tenantId);
                return Multi.createFrom().empty();
            }

            @Override
            public Multi<PositionEvent> watchVehicle(String tenantId, String vin, Duration interval) {
                tenantSeen.set(tenantId);
                return Multi.createFrom().item(new PositionEvent(tenantId, vin,
                    Instant.parse("2026-08-13T12:00:00Z"), 52.52, 13.405, 34, 72, "MOVING"));
            }
        };
        CurrentTenant tenant = new CurrentTenant() {
            @Override public TenantIdentity get() {
                return new TenantIdentity("tenant-a", "test", Set.of("operator"));
            }
        };
        var adapter = new GrpcStreamingAdapter(useCase, new GrpcPositionMapper(), tenant);

        var updates = adapter.watchVehicle(WatchVehicleRequest.newBuilder()
                .setVin("1HGCM82633A004352").build())
            .collect().asList().await().indefinitely();

        assertEquals("tenant-a", tenantSeen.get());
        assertEquals(1, updates.size());
    }
}
