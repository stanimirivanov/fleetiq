package io.fleetiq.topology.adapter.inbound.grpc;

import io.fleetiq.proto.topology.v1.FleetTopologyGrpc;
import io.fleetiq.topology.domain.port.inbound.TopologyUseCase;
import io.quarkus.grpc.GrpcService;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@GrpcService
public class TopologyGrpcAdapter extends FleetTopologyGrpc.FleetTopologyImplBase {

    private static final Logger log = LoggerFactory.getLogger(TopologyGrpcAdapter.class);

    @Inject
    TopologyUseCase useCase;

    @Override
    public void createRelationship(
        io.fleetiq.proto.topology.v1.CreateRelationshipRequest request,
        io.grpc.stub.StreamObserver<io.fleetiq.proto.topology.v1.CreateRelationshipResponse> responseObserver
    ) {
        log.debug("gRPC create relationship: {} -> {}", request.getSourceVin(), request.getTargetVin());
        var response = io.fleetiq.proto.topology.v1.CreateRelationshipResponse.newBuilder()
            .setCreated(true).build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void getFleetGraph(
        io.fleetiq.proto.topology.v1.GetFleetGraphRequest request,
        io.grpc.stub.StreamObserver<io.fleetiq.proto.topology.v1.GetFleetGraphResponse> responseObserver
    ) {
        log.debug("gRPC get fleet graph: {}", request.getRootVin());
        var response = io.fleetiq.proto.topology.v1.GetFleetGraphResponse.newBuilder().build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void findNearbyVehicles(
        io.fleetiq.proto.topology.v1.FindNearbyRequest request,
        io.grpc.stub.StreamObserver<io.fleetiq.proto.topology.v1.FindNearbyResponse> responseObserver
    ) {
        log.debug("gRPC find nearby vehicles");
        var response = io.fleetiq.proto.topology.v1.FindNearbyResponse.newBuilder().build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
