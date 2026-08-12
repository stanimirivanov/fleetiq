package io.fleetiq.topology.adapter.inbound.grpc;

import io.fleetiq.proto.common.v1.VehicleStatus;
import io.fleetiq.proto.topology.v1.CreateRelationshipRequest;
import io.fleetiq.proto.topology.v1.FindNearbyResponse;
import io.fleetiq.proto.topology.v1.GetFleetGraphResponse;
import io.fleetiq.topology.domain.model.TopologyEdge;
import io.fleetiq.topology.domain.model.TopologyNode;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Locale;

@ApplicationScoped
public class GrpcTopologyMapper {

    public TopologyEdge toDomain(CreateRelationshipRequest request) {
        return new TopologyEdge(
            request.getSourceVin(),
            request.getTargetVin(),
            request.getType().name().replace("RELATIONSHIP_TYPE_", ""),
            request.getPropertiesMap()
        );
    }

    public GetFleetGraphResponse toGraphResponse(List<TopologyNode> nodes) {
        return GetFleetGraphResponse.newBuilder()
            .addAllNodes(nodes.stream().map(this::toProto).toList())
            .build();
    }

    public FindNearbyResponse toNearbyResponse(List<String> vins) {
        return FindNearbyResponse.newBuilder().addAllVins(vins).build();
    }

    private io.fleetiq.proto.topology.v1.TopologyNode toProto(TopologyNode node) {
        return io.fleetiq.proto.topology.v1.TopologyNode.newBuilder()
            .setVin(node.vin())
            .setDeviceType(node.deviceType())
            .setStatus(toProtoStatus(node.status()))
            .build();
    }

    private VehicleStatus toProtoStatus(String status) {
        if (status == null || status.isBlank()) {
            return VehicleStatus.VEHICLE_STATUS_UNSPECIFIED;
        }
        try {
            return VehicleStatus.valueOf("VEHICLE_STATUS_" + status.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return VehicleStatus.VEHICLE_STATUS_UNSPECIFIED;
        }
    }
}
