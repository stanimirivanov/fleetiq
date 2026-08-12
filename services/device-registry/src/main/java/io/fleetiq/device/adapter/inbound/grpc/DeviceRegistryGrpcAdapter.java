package io.fleetiq.device.adapter.inbound.grpc;

import io.fleetiq.device.domain.port.inbound.DeviceRegistryUseCase;
import io.fleetiq.proto.device.v1.MutinyDeviceRegistryGrpc;
import io.fleetiq.proto.device.v1.GetDeviceRequest;
import io.fleetiq.proto.device.v1.GetDeviceResponse;
import io.fleetiq.proto.device.v1.RegisterDeviceRequest;
import io.fleetiq.proto.device.v1.RegisterDeviceResponse;
import io.fleetiq.proto.device.v1.UpdateDeviceStatusRequest;
import io.fleetiq.proto.device.v1.UpdateDeviceStatusResponse;
import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@GrpcService
public class DeviceRegistryGrpcAdapter extends MutinyDeviceRegistryGrpc.DeviceRegistryImplBase {

    private static final Logger log = LoggerFactory.getLogger(DeviceRegistryGrpcAdapter.class);

    @Inject
    DeviceRegistryUseCase useCase;

    @Override
    public Uni<RegisterDeviceResponse> registerDevice(RegisterDeviceRequest request) {
        log.debug("gRPC register device: {}", request.getVin());
        // Placeholder — full implementation in Phase 1
        return Uni.createFrom().item(RegisterDeviceResponse.newBuilder().build());
    }

    @Override
    public Uni<GetDeviceResponse> getDevice(GetDeviceRequest request) {
        log.debug("gRPC get device: {}", request.getVin());
        return Uni.createFrom().item(GetDeviceResponse.newBuilder().build());
    }

    @Override
    public Uni<UpdateDeviceStatusResponse> updateDeviceStatus(UpdateDeviceStatusRequest request) {
        log.debug("gRPC update status: {}", request.getVin());
        return Uni.createFrom().item(UpdateDeviceStatusResponse.newBuilder().build());
    }
}
