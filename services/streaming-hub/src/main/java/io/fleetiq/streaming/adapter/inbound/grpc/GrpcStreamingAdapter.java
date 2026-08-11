package io.fleetiq.streaming.adapter.inbound.grpc;

import io.fleetiq.proto.streaming.v1.FleetStreamingGrpc;
import io.fleetiq.proto.streaming.v1.PositionUpdate;
import io.fleetiq.proto.streaming.v1.WatchFleetRequest;
import io.fleetiq.proto.streaming.v1.WatchVehicleRequest;
import io.fleetiq.streaming.domain.port.inbound.StreamingUseCase;
import io.grpc.stub.StreamObserver;
import io.quarkus.grpc.GrpcService;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@GrpcService
public class GrpcStreamingAdapter extends FleetStreamingGrpc.FleetStreamingImplBase {

    private static final Logger log = LoggerFactory.getLogger(GrpcStreamingAdapter.class);

    @Inject
    StreamingUseCase useCase;

    @Override
    public void watchFleet(WatchFleetRequest request, StreamObserver<PositionUpdate> responseObserver) {
        log.debug("gRPC watch fleet — streaming not yet implemented");
        // Server streaming will be implemented in Phase 1
        responseObserver.onCompleted();
    }

    @Override
    public void watchVehicle(WatchVehicleRequest request, StreamObserver<PositionUpdate> responseObserver) {
        log.debug("gRPC watch vehicle: {}", request.getVin());
        responseObserver.onCompleted();
    }
}
