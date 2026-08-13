package io.fleetiq.topology.domain;

import io.fleetiq.topology.domain.model.TopologyEdge;
import io.fleetiq.topology.domain.model.TopologyNode;
import io.fleetiq.topology.domain.port.outbound.TopologyRepository;
import io.fleetiq.topology.domain.service.TopologyService;
import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class TopologyServiceTest {

    @Test
    void delegatesWithoutLeavingTheReactiveChain() {
        StubRepository repository = new StubRepository();
        TopologyService service = new TopologyService(repository);
        TopologyEdge edge = new TopologyEdge("source", "target", "CONVOY", Map.of());

        service.createRelationship(edge).await().indefinitely();
        var nodes = service.getFleetGraph("source", 2).await().indefinitely();
        var nearby = service.findNearbyVehicles(52.52, 13.405, 5).await().indefinitely();

        assertSame(edge, repository.edge);
        assertEquals("source", nodes.getFirst().vin());
        assertEquals(List.of("nearby-vin"), nearby);
    }

    private static final class StubRepository implements TopologyRepository {
        private TopologyEdge edge;

        @Override
        public Uni<Void> upsertVehicle(io.fleetiq.topology.domain.model.VehicleProjection vehicle) {
            return Uni.createFrom().voidItem();
        }

        @Override
        public Uni<Void> updatePosition(String vin, double latitude, double longitude, double altitude,
                                        java.time.Instant observedAt) {
            return Uni.createFrom().voidItem();
        }

        @Override
        public Uni<Void> createRelationship(TopologyEdge edge) {
            return Uni.createFrom().item(() -> {
                this.edge = edge;
                return null;
            });
        }

        @Override
        public Uni<List<TopologyNode>> findConnectedNodes(String vin, int maxDepth) {
            return Uni.createFrom().item(List.of(new TopologyNode(vin, "OBD", "IDLE")));
        }

        @Override
        public Uni<List<String>> findNearby(double latitude, double longitude, double radiusKm) {
            return Uni.createFrom().item(List.of("nearby-vin"));
        }
    }
}
