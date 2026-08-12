package io.fleetiq.topology.adapter.inbound.grpc;

import io.fleetiq.proto.topology.v1.*;
import io.fleetiq.topology.domain.port.inbound.TopologyUseCase;
import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@GrpcService
public class TopologyGrpcAdapter extends MutinyFleetTopologyGrpc.FleetTopologyImplBase {

    private static final Logger log = LoggerFactory.getLogger(TopologyGrpcAdapter.class);

    @Inject
    TopologyUseCase useCase;

    @Override
    public Uni<CreateRelationshipResponse> createRelationship(CreateRelationshipRequest request) {
        log.debug("gRPC create relationship: {} -> {}", request.getSourceVin(), request.getTargetVin());
        return Uni.createFrom().item(CreateRelationshipResponse.newBuilder().setCreated(true).build());
    }

    @Override
    public Uni<GetFleetGraphResponse> getFleetGraph(GetFleetGraphRequest request) {
        log.debug("gRPC get fleet graph: {}", request.getRootVin());
        return Uni.createFrom().item(GetFleetGraphResponse.newBuilder().build());
    }

    @Override
    public Uni<FindNearbyResponse> findNearbyVehicles(FindNearbyRequest request) {
        log.debug("gRPC find nearby vehicles");
        return Uni.createFrom().item(FindNearbyResponse.newBuilder().build());
    }
}
