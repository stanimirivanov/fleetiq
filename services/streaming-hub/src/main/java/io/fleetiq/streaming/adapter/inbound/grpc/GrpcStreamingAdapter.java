package io.fleetiq.streaming.adapter.inbound.grpc;

import io.fleetiq.proto.streaming.v1.MutinyFleetStreamingGrpc;
import io.fleetiq.proto.streaming.v1.PositionUpdate;
import io.fleetiq.proto.streaming.v1.WatchFleetRequest;
import io.fleetiq.proto.streaming.v1.WatchVehicleRequest;
import io.fleetiq.streaming.domain.port.inbound.StreamingUseCase;
import io.fleetiq.security.TenantSecured;
import io.fleetiq.security.CurrentTenant;
import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Multi;
import lombok.RequiredArgsConstructor;
import jakarta.annotation.security.RolesAllowed;

import java.time.Duration;
import java.util.Set;

@GrpcService
@RequiredArgsConstructor
@TenantSecured
@RolesAllowed({"operator", "service"})
public class GrpcStreamingAdapter extends MutinyFleetStreamingGrpc.FleetStreamingImplBase {

    private final StreamingUseCase useCase;
    private final GrpcPositionMapper mapper;
    private final CurrentTenant currentTenant;

    @Override
    public Multi<PositionUpdate> watchFleet(WatchFleetRequest request) {
        return useCase.watchFleet(
                currentTenant.get().tenantId(),
                Set.copyOf(request.getVinsList()), interval(request.getMinUpdateIntervalSeconds()))
            .map(mapper::toProto);
    }

    @Override
    public Multi<PositionUpdate> watchVehicle(WatchVehicleRequest request) {
        return useCase.watchVehicle(
                currentTenant.get().tenantId(),
                request.getVin(), interval(request.getMinUpdateIntervalSeconds()))
            .map(mapper::toProto);
    }

    private Duration interval(double seconds) {
        if (!Double.isFinite(seconds) || seconds < 0) {
            throw new IllegalArgumentException("Minimum update interval must be finite and non-negative");
        }
        return Duration.ofMillis(Math.round(seconds * 1_000));
    }
}
