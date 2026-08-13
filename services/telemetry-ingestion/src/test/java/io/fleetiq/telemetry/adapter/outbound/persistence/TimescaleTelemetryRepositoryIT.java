package io.fleetiq.telemetry.adapter.outbound.persistence;

import io.fleetiq.telemetry.domain.port.outbound.TelemetryRepository;
import io.fleetiq.telemetry.domain.port.outbound.TelemetryRepositoryContract;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.quarkus.test.vertx.UniAsserter;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.pgclient.PgPool;
import jakarta.inject.Inject;
import io.fleetiq.proto.events.v1.PositionProjectionEvent;
import io.fleetiq.telemetry.domain.model.TelemetrySample;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
class TimescaleTelemetryRepositoryIT extends TelemetryRepositoryContract {

    @Inject TimescaleTelemetryRepository repository;
    @Inject PgPool pgPool;

    @Override
    protected TelemetryRepository repository() {
        return repository;
    }

    @Override
    protected Uni<?> resetRepository() {
        return pgPool.query("TRUNCATE TABLE projection_outbox, telemetry_samples").execute();
    }

    @Test
    @RunOnVertxContext
    void savesTelemetryAndOutboxEventAtomically(UniAsserter asserter) {
        asserter.execute(this::resetRepository);
        asserter.execute(() -> repository.save(new TelemetrySample(
            "2HGFC2F59JH000001", Instant.parse("2026-08-13T08:00:00Z"),
            52.52, 13.405, 34, 72, 68, 91, 12.7, Map.of())));
        asserter.assertThat(() -> pgPool.query("SELECT payload FROM projection_outbox").execute(), rows -> {
            assertEquals(1, rows.size());
            try {
                var event = PositionProjectionEvent.parseFrom(rows.iterator().next().getBuffer("payload").getBytes());
                assertEquals("2HGFC2F59JH000001", event.getVin());
                assertEquals(52.52, event.getLatitude());
            } catch (com.google.protobuf.InvalidProtocolBufferException e) {
                throw new AssertionError(e);
            }
        });
    }
}
