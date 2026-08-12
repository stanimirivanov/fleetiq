package io.fleetiq.streaming.adapter.inbound.grpc;

import io.fleetiq.proto.streaming.v1.MutinyFleetStreamingGrpc;
import io.fleetiq.proto.streaming.v1.PositionUpdate;
import io.fleetiq.proto.streaming.v1.WatchFleetRequest;
import io.fleetiq.proto.streaming.v1.WatchVehicleRequest;
import io.fleetiq.streaming.domain.port.inbound.StreamingUseCase;
import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Multi;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@GrpcService
public class GrpcStreamingAdapter extends MutinyFleetStreamingGrpc.FleetStreamingImplBase {

    private static final Logger log = LoggerFactory.getLogger(GrpcStreamingAdapter.class);

    @Inject
    StreamingUseCase useCase;

    @Override
    public Multi<PositionUpdate> watchFleet(WatchFleetRequest request) {
        log.debug("gRPC watch fleet — streaming not yet implemented");
        // Server streaming will be implemented in Phase 1
        return Multi.createFrom().empty();
    }

    @Override
    public Multi<PositionUpdate> watchVehicle(WatchVehicleRequest request) {
        log.debug("gRPC watch vehicle: {}", request.getVin());
        return Multi.createFrom().empty();
    }
}
