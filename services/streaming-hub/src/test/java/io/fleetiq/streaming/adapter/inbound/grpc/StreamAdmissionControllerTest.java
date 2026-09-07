package io.fleetiq.streaming.adapter.inbound.grpc;

import io.fleetiq.security.TenantIdentity;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.helpers.test.AssertSubscriber;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StreamAdmissionControllerTest {

    private final TenantIdentity identity = new TenantIdentity("tenant-a", "operator-1", Set.of("operator"));

    @Test
    void rejectsExcessStreamsAndReleasesCapacityWhenClientCancels() {
        var controller = new StreamAdmissionController(1, 10);
        var first = AssertSubscriber.<String>create(0);
        controller.<String>admit(identity, 1, () -> Multi.createFrom().nothing())
            .subscribe().withSubscriber(first);

        StatusRuntimeException rejected = assertThrows(StatusRuntimeException.class, () ->
            controller.admit(identity, 1, () -> Multi.createFrom().item("never"))
                .collect().asList().await().indefinitely());
        assertEquals(Status.Code.RESOURCE_EXHAUSTED, rejected.getStatus().getCode());

        first.cancel();
        assertEquals("accepted", controller.admit(identity, 1,
            () -> Multi.createFrom().item("accepted")).collect().first().await().indefinitely());
    }

    @Test
    void rejectsOversizedFleetSelection() {
        var controller = new StreamAdmissionController(1, 2);
        StatusRuntimeException rejected = assertThrows(StatusRuntimeException.class, () ->
            controller.admit(identity, 3, () -> Multi.createFrom().item("never"))
                .collect().asList().await().indefinitely());
        assertEquals(Status.Code.INVALID_ARGUMENT, rejected.getStatus().getCode());
    }
}
