package io.fleetiq.telemetry.domain.port.outbound;

import io.fleetiq.telemetry.domain.model.TelemetrySample;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.quarkus.test.vertx.UniAsserter;
import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public abstract class TelemetryRepositoryContract {

    private static final String VIN = "1HGCM82633A004352";
    private static final String TENANT = "tenant-a";
    private static final Instant FIRST = Instant.parse("2026-08-13T08:00:00Z");
    private static final Instant SECOND = Instant.parse("2026-08-13T08:05:00Z");

    protected abstract TelemetryRepository repository();
    protected abstract Uni<?> resetRepository();

    @Test
    @RunOnVertxContext
    void savesAndReturnsSamplesInNewestFirstOrder(UniAsserter asserter) {
        asserter.execute(this::resetRepository);
        asserter.execute(() -> repository().save(TENANT, sample(VIN, FIRST, 40.0, Map.of("oil_pressure", 3.4))));
        asserter.execute(() -> repository().save(TENANT, sample(VIN, SECOND, 80.0, Map.of("oil_pressure", 3.8))));
        asserter.execute(() -> repository().save(TENANT, sample("WVWZZZ1JZXW000001", SECOND, 120.0, Map.of())));
        asserter.assertThat(
            () -> repository().findByVinAndTimeRange(TENANT, VIN, FIRST.minusSeconds(1), SECOND.plusSeconds(1)),
            samples -> {
                assertEquals(2, samples.size());
                assertEquals(SECOND, samples.get(0).timestamp());
                assertEquals(FIRST, samples.get(1).timestamp());
                assertEquals(Map.of("oil_pressure", 3.8), samples.get(0).customMetrics());
                assertEquals(52.52, samples.get(0).latitude());
                assertEquals(13.405, samples.get(0).longitude());
            });
    }

    @Test
    @RunOnVertxContext
    void calculatesAverageSpeedInsideRequestedRange(UniAsserter asserter) {
        asserter.execute(this::resetRepository);
        asserter.execute(() -> repository().save(TENANT, sample(VIN, FIRST, 40.0, Map.of())));
        asserter.execute(() -> repository().save(TENANT, sample(VIN, SECOND, 80.0, Map.of())));
        asserter.assertEquals(
            () -> repository().getAverageSpeed(TENANT, VIN, FIRST.minusSeconds(1), SECOND.plusSeconds(1)), 60.0);
        asserter.assertEquals(
            () -> repository().getAverageSpeed(TENANT, VIN, SECOND, SECOND.plusSeconds(1)), 80.0);
    }

    @Test
    @RunOnVertxContext
    void returnsNeutralResultsWhenNoSamplesMatch(UniAsserter asserter) {
        asserter.execute(this::resetRepository);
        asserter.assertThat(
            () -> repository().findByVinAndTimeRange(TENANT, VIN, FIRST, SECOND),
            samples -> assertTrue(samples.isEmpty()));
        asserter.assertEquals(() -> repository().getAverageSpeed(TENANT, VIN, FIRST, SECOND), 0.0);
    }

    @Test
    @RunOnVertxContext
    void isolatesIdenticalVinsAcrossTenants(UniAsserter asserter) {
        asserter.execute(this::resetRepository);
        asserter.execute(() -> repository().save("tenant-a", sample(VIN, FIRST, 40.0, Map.of())));
        asserter.execute(() -> repository().save("tenant-b", sample(VIN, SECOND, 80.0, Map.of())));
        asserter.assertThat(
            () -> repository().findByVinAndTimeRange("tenant-a", VIN, FIRST.minusSeconds(1), SECOND.plusSeconds(1)),
            samples -> {
                assertEquals(1, samples.size());
                assertEquals(FIRST, samples.getFirst().timestamp());
            });
    }

    private static TelemetrySample sample(String vin, Instant timestamp, double speed, Map<String, Double> metrics) {
        return TelemetrySample.builder()
            .vin(vin).timestamp(timestamp)
            .latitude(52.52).longitude(13.405).altitude(34.0)
            .speedKmh(speed).fuelLevelPct(68.0).engineTempCelsius(91.2).batteryVoltage(12.7)
            .customMetrics(metrics)
            .build();
    }
}
