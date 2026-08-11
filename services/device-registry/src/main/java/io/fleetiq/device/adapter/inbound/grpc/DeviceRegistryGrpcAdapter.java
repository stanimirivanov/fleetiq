package io.fleetiq.device.adapter.inbound.grpc;

import io.fleetiq.device.domain.port.inbound.DeviceRegistryUseCase;
import io.fleetiq.proto.device.v1.DeviceRegistryGrpc;
import io.quarkus.grpc.GrpcService;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@GrpcService
public class DeviceRegistryGrpcAdapter extends DeviceRegistryGrpc.DeviceRegistryImplBase {

    private static final Logger log = LoggerFactory.getLogger(DeviceRegistryGrpcAdapter.class);

    @Inject
    DeviceRegistryUseCase useCase;

    @Override
    public void registerDevice(
        io.fleetiq.proto.device.v1.RegisterDeviceRequest request,
        io.grpc.stub.StreamObserver<io.fleetiq.proto.device.v1.RegisterDeviceResponse> responseObserver
    ) {
        log.debug("gRPC register device: {}", request.getVin());
        // Placeholder — full implementation in Phase 1
        var response = io.fleetiq.proto.device.v1.RegisterDeviceResponse.newBuilder().build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void getDevice(
        io.fleetiq.proto.device.v1.GetDeviceRequest request,
        io.grpc.stub.StreamObserver<io.fleetiq.proto.device.v1.GetDeviceResponse> responseObserver
    ) {
        log.debug("gRPC get device: {}", request.getVin());
        var response = io.fleetiq.proto.device.v1.GetDeviceResponse.newBuilder().build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void updateDeviceStatus(
        io.fleetiq.proto.device.v1.UpdateDeviceStatusRequest request,
        io.grpc.stub.StreamObserver<io.fleetiq.proto.device.v1.UpdateDeviceStatusResponse> responseObserver
    ) {
        log.debug("gRPC update status: {}", request.getVin());
        var response = io.fleetiq.proto.device.v1.UpdateDeviceStatusResponse.newBuilder().build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
