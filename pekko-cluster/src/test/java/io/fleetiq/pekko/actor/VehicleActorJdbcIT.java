package io.fleetiq.pekko.actor;

import com.typesafe.config.ConfigFactory;
import io.fleetiq.pekko.api.VehicleStateService.TelemetryUpdate;
import io.fleetiq.pekko.api.VehicleStateService.VehicleCommand;
import org.apache.pekko.actor.testkit.typed.javadsl.ActorTestKit;
import org.apache.pekko.actor.testkit.typed.javadsl.TestProbe;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import java.sql.DriverManager;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Proves that state and command idempotency survive recovery from the real JDBC journal. */
class VehicleActorJdbcIT {

    private static final GenericContainer<?> POSTGRES = new GenericContainer<>(
        DockerImageName.parse("postgres:16-alpine"))
        .withEnv("POSTGRES_USER", "fleetiq")
        .withEnv("POSTGRES_PASSWORD", "fleetiq_dev")
        .withEnv("POSTGRES_DB", "pekko_journal_db")
        .withExposedPorts(5432);

    private static ActorTestKit testKit;

    @BeforeAll
    static void startDatabaseAndActorSystem() throws Exception {
        POSTGRES.start();
        String jdbcUrl = "jdbc:postgresql://" + POSTGRES.getHost() + ':'
            + POSTGRES.getMappedPort(5432) + "/pekko_journal_db";
        try (var connection = DriverManager.getConnection(jdbcUrl, "fleetiq", "fleetiq_dev");
             var statement = connection.createStatement()) {
            statement.executeUpdate("""
                CREATE TABLE public.event_journal (
                    ordering BIGSERIAL,
                    persistence_id VARCHAR(255) NOT NULL,
                    sequence_number BIGINT NOT NULL,
                    deleted BOOLEAN DEFAULT FALSE NOT NULL,
                    writer VARCHAR(255) NOT NULL,
                    write_timestamp BIGINT,
                    adapter_manifest VARCHAR(255),
                    event_ser_id INTEGER NOT NULL,
                    event_ser_manifest VARCHAR(255) NOT NULL,
                    event_payload BYTEA NOT NULL,
                    meta_ser_id INTEGER,
                    meta_ser_manifest VARCHAR(255),
                    meta_payload BYTEA,
                    PRIMARY KEY (persistence_id, sequence_number)
                )
                """);
            statement.executeUpdate(
                "CREATE UNIQUE INDEX event_journal_ordering_idx ON public.event_journal(ordering)");
            statement.executeUpdate("""
                CREATE TABLE public.snapshot (
                    persistence_id VARCHAR(255) NOT NULL,
                    sequence_number BIGINT NOT NULL,
                    created BIGINT NOT NULL,
                    snapshot_ser_id INTEGER NOT NULL,
                    snapshot_ser_manifest VARCHAR(255) NOT NULL,
                    snapshot_payload BYTEA NOT NULL,
                    meta_ser_id INTEGER,
                    meta_ser_manifest VARCHAR(255),
                    meta_payload BYTEA,
                    PRIMARY KEY (persistence_id, sequence_number)
                )
                """);
        }

        var overrides = ConfigFactory.parseString("""
            pekko.actor.provider = local
            pekko.persistence.journal.plugin = "jdbc-journal"
            pekko.persistence.journal.auto-start-journals = []
            pekko.persistence.snapshot-store.plugin = "jdbc-snapshot-store"
            pekko-persistence-jdbc.shared-databases.slick.db.host = "%s"
            pekko-persistence-jdbc.shared-databases.slick.db.port = %d
            pekko-persistence-jdbc.shared-databases.slick.db.name = "pekko_journal_db"
            pekko-persistence-jdbc.shared-databases.slick.db.url = "%s"
            pekko-persistence-jdbc.shared-databases.slick.db.user = "fleetiq"
            pekko-persistence-jdbc.shared-databases.slick.db.password = "fleetiq_dev"
            """.formatted(POSTGRES.getHost(), POSTGRES.getMappedPort(5432), jdbcUrl));
        testKit = ActorTestKit.create(overrides.withFallback(ConfigFactory.load()).resolve());
    }

    @AfterAll
    static void stopDatabaseAndActorSystem() {
        if (testKit != null) testKit.shutdownTestKit();
        POSTGRES.stop();
    }

    @Test
    void recoversVehicleStateAndCommandIdsFromPostgres() throws Exception {
        String tenant = "jdbc-recovery-" + UUID.randomUUID();
        String vin = "1HGCM82633A004352";
        Instant observedAt = Instant.parse("2026-08-12T12:00:00Z");
        var command = new VehicleCommand(tenant, vin, "LOCK", "", "jdbc-command-1");
        TestProbe<VehicleActor.OutcomeReply> outcome = testKit.createTestProbe();
        var first = testKit.spawn(VehicleActor.create(tenant, vin));

        first.tell(new VehicleActor.RecordTelemetry(
            new TelemetryUpdate(tenant, vin, observedAt, 52.52, 13.405, 72.5), outcome.ref()));
        assertEquals(1, outcome.receiveMessage().sequence());
        first.tell(new VehicleActor.DispatchCommand(command, outcome.ref()));
        assertEquals(2, outcome.receiveMessage().sequence());
        testKit.stop(first);

        var recovered = testKit.spawn(VehicleActor.create(tenant, vin));
        TestProbe<VehicleActor.StateReply> state = testKit.createTestProbe();
        recovered.tell(new VehicleActor.GetState(state.ref()));
        var reply = state.receiveMessage();
        assertEquals(observedAt, reply.lastObservedAt());
        assertEquals(72.5, reply.speedKmh());
        assertEquals(1, reply.telemetrySequence());

        recovered.tell(new VehicleActor.DispatchCommand(command, outcome.ref()));
        assertEquals(2, outcome.receiveMessage().sequence());

        try (var connection = DriverManager.getConnection(
                "jdbc:postgresql://" + POSTGRES.getHost() + ':' + POSTGRES.getMappedPort(5432)
                    + "/pekko_journal_db", "fleetiq", "fleetiq_dev");
             var statement = connection.prepareStatement(
                 "SELECT count(*) FROM event_journal WHERE persistence_id LIKE ?")) {
            statement.setString(1, "Vehicle|%");
            try (var rows = statement.executeQuery()) {
                rows.next();
                assertEquals(2, rows.getInt(1));
            }
        }
    }
}
