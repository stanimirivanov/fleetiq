package io.fleetiq.pekko.actor;

import io.fleetiq.pekko.api.VehicleStateService.TelemetryUpdate;
import org.apache.pekko.actor.testkit.typed.javadsl.ActorTestKit;
import org.apache.pekko.actor.testkit.typed.javadsl.TestProbe;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VehicleActorTest {

    private static final ActorTestKit TEST_KIT = ActorTestKit.create();
    private static final String VIN = "1HGCM82633A004352";

    @AfterAll
    static void shutdown() {
        TEST_KIT.shutdownTestKit();
    }

    @Test
    void acknowledgesTelemetryAndReturnsState() {
        var actor = TEST_KIT.spawn(VehicleActor.create(VIN));
        TestProbe<VehicleActor.OutcomeReply> outcome = TEST_KIT.createTestProbe();
        TestProbe<VehicleActor.StateReply> state = TEST_KIT.createTestProbe();
        Instant observedAt = Instant.parse("2026-08-12T12:00:00Z");

        actor.tell(new VehicleActor.RecordTelemetry(
            new TelemetryUpdate(VIN, observedAt, 52.52, 13.405, 72.5), outcome.ref()));
        actor.tell(new VehicleActor.GetState(state.ref()));

        assertEquals(1, outcome.receiveMessage().sequence());
        var snapshot = state.receiveMessage();
        assertEquals(VIN, snapshot.vin());
        assertEquals(observedAt, snapshot.lastObservedAt());
        assertEquals(72.5, snapshot.speedKmh());
        assertEquals(1, snapshot.telemetrySequence());
    }

    @Test
    void rejectsTelemetryOlderThanCurrentState() {
        var actor = TEST_KIT.spawn(VehicleActor.create(VIN));
        TestProbe<VehicleActor.OutcomeReply> outcome = TEST_KIT.createTestProbe();

        actor.tell(new VehicleActor.RecordTelemetry(
            new TelemetryUpdate(VIN, Instant.parse("2026-08-12T12:00:00Z"), 1, 1, 1), outcome.ref()));
        assertTrue(outcome.receiveMessage().accepted());
        actor.tell(new VehicleActor.RecordTelemetry(
            new TelemetryUpdate(VIN, Instant.parse("2026-08-12T11:59:59Z"), 2, 2, 2), outcome.ref()));

        var rejected = outcome.receiveMessage();
        assertFalse(rejected.accepted());
        assertEquals("Telemetry is older than current state", rejected.reason());
    }
}
