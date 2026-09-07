package io.fleetiq.pekko.actor;

import com.typesafe.config.ConfigFactory;
import io.fleetiq.pekko.api.VehicleStateService.TelemetryUpdate;
import io.fleetiq.pekko.api.VehicleStateService.VehicleCommand;
import org.apache.pekko.actor.testkit.typed.javadsl.ActorTestKit;
import org.apache.pekko.actor.testkit.typed.javadsl.TestProbe;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VehicleActorTest {

    private static final ActorTestKit TEST_KIT = ActorTestKit.create(
        ConfigFactory.parseString("""
            pekko.actor.provider = local
            pekko.persistence.journal.plugin = "pekko.persistence.journal.inmem"
            pekko.persistence.journal.auto-start-journals = []
            pekko.persistence.snapshot-store.plugin = "pekko.persistence.snapshot-store.local"
            pekko.persistence.snapshot-store.local.dir = "target/test-snapshots"
            """).withFallback(ConfigFactory.load()).resolve());
    private static final String VIN = "1HGCM82633A004352";

    @AfterAll
    static void shutdown() {
        TEST_KIT.shutdownTestKit();
    }

    @Test
    void acknowledgesTelemetryAndReturnsState() {
        String tenant = "tenant-state";
        var actor = TEST_KIT.spawn(VehicleActor.create(tenant, VIN));
        TestProbe<VehicleActor.OutcomeReply> outcome = TEST_KIT.createTestProbe();
        TestProbe<VehicleActor.StateReply> state = TEST_KIT.createTestProbe();
        Instant observedAt = Instant.parse("2026-08-12T12:00:00Z");

        actor.tell(new VehicleActor.RecordTelemetry(
            new TelemetryUpdate(tenant, VIN, observedAt, 52.52, 13.405, 72.5), outcome.ref()));
        actor.tell(new VehicleActor.GetState(state.ref()));

        assertEquals(1, outcome.receiveMessage().sequence());
        var snapshot = state.receiveMessage();
        assertEquals(VIN, snapshot.vin());
        assertEquals(tenant, snapshot.tenantId());
        assertEquals(observedAt, snapshot.lastObservedAt());
        assertEquals(72.5, snapshot.speedKmh());
        assertEquals(1, snapshot.telemetrySequence());
    }

    @Test
    void acceptsAnExactDuplicateWithoutAdvancingSequence() {
        String tenant = "tenant-duplicate-telemetry";
        var actor = TEST_KIT.spawn(VehicleActor.create(tenant, VIN));
        TestProbe<VehicleActor.OutcomeReply> outcome = TEST_KIT.createTestProbe();
        var update = new TelemetryUpdate(
            tenant, VIN, Instant.parse("2026-08-12T12:00:00Z"), 1, 2, 3);

        actor.tell(new VehicleActor.RecordTelemetry(update, outcome.ref()));
        actor.tell(new VehicleActor.RecordTelemetry(update, outcome.ref()));

        assertEquals(1, outcome.receiveMessage().sequence());
        assertEquals(1, outcome.receiveMessage().sequence());
    }

    @Test
    void rejectsConflictingTelemetryAtTheSameTimestamp() {
        String tenant = "tenant-conflicting-telemetry";
        var actor = TEST_KIT.spawn(VehicleActor.create(tenant, VIN));
        TestProbe<VehicleActor.OutcomeReply> outcome = TEST_KIT.createTestProbe();
        Instant observedAt = Instant.parse("2026-08-12T12:00:00Z");

        actor.tell(new VehicleActor.RecordTelemetry(
            new TelemetryUpdate(tenant, VIN, observedAt, 1, 1, 1), outcome.ref()));
        assertTrue(outcome.receiveMessage().accepted());
        actor.tell(new VehicleActor.RecordTelemetry(
            new TelemetryUpdate(tenant, VIN, observedAt, 2, 2, 2), outcome.ref()));

        var rejected = outcome.receiveMessage();
        assertFalse(rejected.accepted());
        assertEquals("Telemetry timestamp conflicts with current state", rejected.reason());
    }

    @Test
    void rejectsTelemetryOlderThanCurrentState() {
        String tenant = "tenant-ordering";
        var actor = TEST_KIT.spawn(VehicleActor.create(tenant, VIN));
        TestProbe<VehicleActor.OutcomeReply> outcome = TEST_KIT.createTestProbe();

        actor.tell(new VehicleActor.RecordTelemetry(
            new TelemetryUpdate(tenant, VIN, Instant.parse("2026-08-12T12:00:00Z"), 1, 1, 1), outcome.ref()));
        assertTrue(outcome.receiveMessage().accepted());
        actor.tell(new VehicleActor.RecordTelemetry(
            new TelemetryUpdate(tenant, VIN, Instant.parse("2026-08-12T11:59:59Z"), 2, 2, 2), outcome.ref()));

        var rejected = outcome.receiveMessage();
        assertFalse(rejected.accepted());
        assertEquals("Telemetry is older than current state", rejected.reason());
    }

    @Test
    void acceptsDuplicateCommandIdWithoutPersistingAnotherEvent() {
        String tenant = "tenant-command-idempotency";
        var actor = TEST_KIT.spawn(VehicleActor.create(tenant, VIN));
        TestProbe<VehicleActor.OutcomeReply> outcome = TEST_KIT.createTestProbe();
        var command = new VehicleCommand(tenant, VIN, "LOCK", "", "command-1");

        actor.tell(new VehicleActor.DispatchCommand(command, outcome.ref()));
        actor.tell(new VehicleActor.DispatchCommand(command, outcome.ref()));

        assertEquals(1, outcome.receiveMessage().sequence());
        assertEquals(1, outcome.receiveMessage().sequence());
    }

    @Test
    void recoversStateAndIdempotencyHistoryAfterRestart() {
        String tenant = "tenant-recovery";
        TestProbe<VehicleActor.OutcomeReply> outcome = TEST_KIT.createTestProbe();
        Instant observedAt = Instant.parse("2026-08-12T12:00:00Z");
        var command = new VehicleCommand(tenant, VIN, "LOCK", "", "recovery-command");
        var first = TEST_KIT.spawn(VehicleActor.create(tenant, VIN));

        first.tell(new VehicleActor.RecordTelemetry(
            new TelemetryUpdate(tenant, VIN, observedAt, 52.52, 13.405, 72.5), outcome.ref()));
        assertEquals(1, outcome.receiveMessage().sequence());
        first.tell(new VehicleActor.DispatchCommand(command, outcome.ref()));
        assertEquals(2, outcome.receiveMessage().sequence());
        TEST_KIT.stop(first);

        var recovered = TEST_KIT.spawn(VehicleActor.create(tenant, VIN));
        TestProbe<VehicleActor.StateReply> state = TEST_KIT.createTestProbe();
        recovered.tell(new VehicleActor.GetState(state.ref()));
        var snapshot = state.receiveMessage();

        assertEquals(observedAt, snapshot.lastObservedAt());
        assertEquals(72.5, snapshot.speedKmh());
        assertEquals(1, snapshot.telemetrySequence());
        recovered.tell(new VehicleActor.DispatchCommand(command, outcome.ref()));
        assertEquals(2, outcome.receiveMessage().sequence());
    }

    @Test
    void rejectsAnUpdateForTheSameVinFromAnotherTenant() {
        String tenant = "tenant-identity";
        var actor = TEST_KIT.spawn(VehicleActor.create(tenant, VIN));
        TestProbe<VehicleActor.OutcomeReply> outcome = TEST_KIT.createTestProbe();

        actor.tell(new VehicleActor.RecordTelemetry(
            new TelemetryUpdate("tenant-other", VIN, Instant.now(), 1, 1, 1), outcome.ref()));

        var rejected = outcome.receiveMessage();
        assertFalse(rejected.accepted());
        assertEquals("Tenant or VIN does not match entity identity", rejected.reason());
    }
}
