package io.fleetiq.telemetry.adapter.outbound.persistence;

import io.fleetiq.telemetry.domain.port.outbound.TelemetryRepository;
import io.fleetiq.telemetry.domain.port.outbound.TelemetryRepositoryContract;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.pgclient.PgPool;
import jakarta.inject.Inject;

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
        return pgPool.query("TRUNCATE TABLE telemetry_samples").execute();
    }
}
