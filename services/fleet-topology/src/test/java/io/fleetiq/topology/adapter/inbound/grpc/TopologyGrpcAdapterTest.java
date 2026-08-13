package io.fleetiq.topology.adapter.inbound.grpc;

import io.fleetiq.proto.common.v1.GeoPoint;
import io.fleetiq.proto.common.v1.VehicleStatus;
import io.fleetiq.proto.topology.v1.CreateRelationshipRequest;
import io.fleetiq.proto.topology.v1.FindNearbyRequest;
import io.fleetiq.proto.topology.v1.GetFleetGraphRequest;
import io.fleetiq.proto.topology.v1.RelationshipType;
import io.fleetiq.topology.domain.model.TopologyEdge;
import io.fleetiq.topology.domain.model.TopologyNode;
import io.fleetiq.topology.domain.port.inbound.TopologyUseCase;
import io.smallrye.mutiny.Uni;
import io.fleetiq.security.CurrentTenant;
import io.fleetiq.security.TenantIdentity;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TopologyGrpcAdapterTest {

    @Test
    void mapsAndDelegatesEveryRpc() {
        StubUseCase useCase = new StubUseCase();
        TopologyGrpcAdapter adapter = new TopologyGrpcAdapter(useCase, new GrpcTopologyMapper(), tenant());

        var created = adapter.createRelationship(CreateRelationshipRequest.newBuilder()
            .setSourceVin("source").setTargetVin("target")
            .setType(RelationshipType.RELATIONSHIP_TYPE_CONVOY).build())
            .await().indefinitely();
        var graph = adapter.getFleetGraph(GetFleetGraphRequest.newBuilder()
            .setRootVin("source").setMaxDepth(3).build()).await().indefinitely();
        var nearby = adapter.findNearbyVehicles(FindNearbyRequest.newBuilder()
            .setCenter(GeoPoint.newBuilder().setLatitude(52.52).setLongitude(13.405))
            .setRadiusKm(10).build()).await().indefinitely();

        assertTrue(created.getCreated());
        assertEquals("CONVOY", useCase.edge.relationshipType());
        assertEquals(VehicleStatus.VEHICLE_STATUS_MOVING, graph.getNodes(0).getStatus());
        assertEquals(List.of("nearby-vin"), nearby.getVinsList());
    }

    private static final class StubUseCase implements TopologyUseCase {
        private TopologyEdge edge;

        @Override
        public Uni<Void> createRelationship(String tenantId, TopologyEdge edge) {
            return Uni.createFrom().item(() -> {
                this.edge = edge;
                return null;
            });
        }

        @Override
        public Uni<List<TopologyNode>> getFleetGraph(String tenantId, String rootVin, int maxDepth) {
            return Uni.createFrom().item(List.of(new TopologyNode(rootVin, "OBD", "MOVING")));
        }

        @Override
        public Uni<List<String>> findNearbyVehicles(String tenantId, double latitude, double longitude, double radiusKm) {
            return Uni.createFrom().item(List.of("nearby-vin"));
        }
    }

    private static CurrentTenant tenant() {
        return new CurrentTenant() {
            @Override public TenantIdentity get() {
                return new TenantIdentity("tenant-a", "test", java.util.Set.of("operator"));
            }
        };
    }
}
