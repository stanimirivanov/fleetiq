package io.fleetiq.device.adapter.inbound.grpc;

import io.fleetiq.device.domain.model.DeviceValidationException;
import io.fleetiq.device.domain.port.inbound.DeviceRegistryUseCase;
import io.fleetiq.proto.device.v1.GetDeviceRequest;
import io.fleetiq.proto.device.v1.GetDeviceResponse;
import io.fleetiq.proto.device.v1.MutinyDeviceRegistryGrpc;
import io.fleetiq.proto.device.v1.RegisterDeviceRequest;
import io.fleetiq.proto.device.v1.RegisterDeviceResponse;
import io.fleetiq.proto.device.v1.UpdateDeviceStatusRequest;
import io.fleetiq.proto.device.v1.UpdateDeviceStatusResponse;
import io.grpc.Status;
import io.grpc.StatusException;
import io.grpc.StatusRuntimeException;
import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Uni;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class DeviceRegistryGrpcAdapter extends MutinyDeviceRegistryGrpc.DeviceRegistryImplBase {

    private final DeviceRegistryUseCase useCase;
    private final GrpcDeviceMapper mapper;

    @Override
    public Uni<RegisterDeviceResponse> registerDevice(RegisterDeviceRequest request) {
        return Uni.createFrom().item(() -> mapper.toCommand(request))
            .onItem().transformToUni(useCase::register)
            .onItem().transformToUni(result -> switch (result) {
                case DeviceRegistryUseCase.RegisterResult.Registered registered ->
                    Uni.createFrom().item(mapper.toRegisterResponse(registered.device()));
                case DeviceRegistryUseCase.RegisterResult.AlreadyExists duplicate ->
                    Uni.createFrom().failure(Status.ALREADY_EXISTS
                        .withDescription("Device already exists: " + duplicate.vin())
                        .asRuntimeException());
            })
            .onFailure(DeviceValidationException.class).transform(this::invalidArgument)
            .onFailure(this::isUnexpected).invoke(this::logUnexpected)
            .onFailure(this::isUnexpected).transform(this::internalError);
    }

    @Override
    public Uni<GetDeviceResponse> getDevice(GetDeviceRequest request) {
        return Uni.createFrom().item(request::getVin)
            .onItem().transformToUni(useCase::getByVin)
            .onItem().transformToUni(device -> device
                .map(value -> Uni.createFrom().item(mapper.toGetResponse(value)))
                .orElseGet(() -> Uni.createFrom().failure(Status.NOT_FOUND
                    .withDescription("Device not found: " + request.getVin())
                    .asRuntimeException())))
            .onFailure(DeviceValidationException.class).transform(this::invalidArgument)
            .onFailure(this::isUnexpected).invoke(this::logUnexpected)
            .onFailure(this::isUnexpected).transform(this::internalError);
    }

    @Override
    public Uni<UpdateDeviceStatusResponse> updateDeviceStatus(UpdateDeviceStatusRequest request) {
        return Uni.createFrom().item(() -> mapper.toCommand(request))
            .onItem().transformToUni(useCase::updateStatus)
            .onItem().transformToUni(result -> switch (result) {
                case DeviceRegistryUseCase.UpdateStatusResult.Updated updated ->
                    Uni.createFrom().item(mapper.toUpdateResponse(updated.device()));
                case DeviceRegistryUseCase.UpdateStatusResult.NotFound missing ->
                    Uni.createFrom().failure(Status.NOT_FOUND
                        .withDescription("Device not found: " + missing.vin())
                        .asRuntimeException());
            })
            .onFailure(DeviceValidationException.class).transform(this::invalidArgument)
            .onFailure(this::isUnexpected).invoke(this::logUnexpected)
            .onFailure(this::isUnexpected).transform(this::internalError);
    }

    private Throwable invalidArgument(Throwable failure) {
        return Status.INVALID_ARGUMENT
            .withDescription(failure.getMessage())
            .asRuntimeException();
    }

    private boolean isUnexpected(Throwable failure) {
        return !(failure instanceof StatusException) && !(failure instanceof StatusRuntimeException);
    }

    private void logUnexpected(Throwable failure) {
        log.error("Unexpected device-registry gRPC failure", failure);
    }

    private Throwable internalError(Throwable failure) {
        return Status.INTERNAL
            .withDescription("Device registry operation failed")
            .withCause(failure)
            .asRuntimeException();
    }
}
