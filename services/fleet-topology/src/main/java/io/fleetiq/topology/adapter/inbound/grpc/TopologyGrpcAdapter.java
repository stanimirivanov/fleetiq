package io.fleetiq.topology.adapter.inbound.grpc;

import io.fleetiq.proto.topology.v1.CreateRelationshipRequest;
import io.fleetiq.proto.topology.v1.CreateRelationshipResponse;
import io.fleetiq.proto.topology.v1.FindNearbyRequest;
import io.fleetiq.proto.topology.v1.FindNearbyResponse;
import io.fleetiq.proto.topology.v1.GetFleetGraphRequest;
import io.fleetiq.proto.topology.v1.GetFleetGraphResponse;
import io.fleetiq.proto.topology.v1.MutinyFleetTopologyGrpc;
import io.fleetiq.topology.domain.port.inbound.TopologyUseCase;
import io.fleetiq.security.TenantSecured;
import io.fleetiq.security.CurrentTenant;
import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Uni;
import lombok.RequiredArgsConstructor;
import jakarta.annotation.security.RolesAllowed;

@GrpcService
@RequiredArgsConstructor
@TenantSecured
@RolesAllowed({"operator", "service"})
public class TopologyGrpcAdapter extends MutinyFleetTopologyGrpc.FleetTopologyImplBase {

    private final TopologyUseCase useCase;
    private final GrpcTopologyMapper mapper;
    private final CurrentTenant currentTenant;

    @Override
    public Uni<CreateRelationshipResponse> createRelationship(CreateRelationshipRequest request) {
        return Uni.createFrom().item(() -> mapper.toDomain(request))
            .onItem().transformToUni(edge -> useCase.createRelationship(currentTenant.get().tenantId(), edge))
            .replaceWith(CreateRelationshipResponse.newBuilder().setCreated(true).build());
    }

    @Override
    public Uni<GetFleetGraphResponse> getFleetGraph(GetFleetGraphRequest request) {
        return useCase.getFleetGraph(currentTenant.get().tenantId(), request.getRootVin(), request.getMaxDepth())
            .map(mapper::toGraphResponse);
    }

    @Override
    public Uni<FindNearbyResponse> findNearbyVehicles(FindNearbyRequest request) {
        return useCase.findNearbyVehicles(
                currentTenant.get().tenantId(),
                request.getCenter().getLatitude(),
                request.getCenter().getLongitude(),
                request.getRadiusKm())
            .map(mapper::toNearbyResponse);
    }
}
