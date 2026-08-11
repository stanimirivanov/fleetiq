package io.fleetiq.maintenance.adapter.inbound.grpc;

import io.fleetiq.maintenance.domain.port.inbound.PredictMaintenanceUseCase;
import io.fleetiq.proto.maintenance.v1.MaintenancePredictorGrpc;
import io.quarkus.grpc.GrpcService;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@GrpcService
public class MaintenanceGrpcAdapter extends MaintenancePredictorGrpc.MaintenancePredictorImplBase {

    private static final Logger log = LoggerFactory.getLogger(MaintenanceGrpcAdapter.class);

    @Inject
    PredictMaintenanceUseCase useCase;

    @Override
    public void predictMaintenance(
        io.fleetiq.proto.maintenance.v1.PredictMaintenanceRequest request,
        io.grpc.stub.StreamObserver<io.fleetiq.proto.maintenance.v1.PredictMaintenanceResponse> responseObserver
    ) {
        log.debug("gRPC predict maintenance: {}", request.getVin());
        var response = io.fleetiq.proto.maintenance.v1.PredictMaintenanceResponse.newBuilder().build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void getPredictionHistory(
        io.fleetiq.proto.maintenance.v1.GetPredictionHistoryRequest request,
        io.grpc.stub.StreamObserver<io.fleetiq.proto.maintenance.v1.GetPredictionHistoryResponse> responseObserver
    ) {
        log.debug("gRPC get prediction history: {}", request.getVin());
        var response = io.fleetiq.proto.maintenance.v1.GetPredictionHistoryResponse.newBuilder().build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void recordMaintenanceEvent(
        io.fleetiq.proto.maintenance.v1.RecordMaintenanceEventRequest request,
        io.grpc.stub.StreamObserver<io.fleetiq.proto.maintenance.v1.RecordMaintenanceEventResponse> responseObserver
    ) {
        log.debug("gRPC record maintenance event: {}", request.getVin());
        var response = io.fleetiq.proto.maintenance.v1.RecordMaintenanceEventResponse.newBuilder().build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
