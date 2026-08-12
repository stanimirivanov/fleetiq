package io.fleetiq.maintenance.adapter.inbound.grpc;

import io.fleetiq.maintenance.domain.port.inbound.PredictMaintenanceUseCase;
import io.fleetiq.proto.maintenance.v1.*;
import io.smallrye.mutiny.Uni;
import io.quarkus.grpc.GrpcService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class MaintenanceGrpcAdapter extends MutinyMaintenancePredictorGrpc.MaintenancePredictorImplBase {

    private final PredictMaintenanceUseCase useCase;
    private final GrpcMaintenanceMapper mapper;

    @Override
    public Uni<PredictMaintenanceResponse> predictMaintenance(PredictMaintenanceRequest request) {
        log.info("gRPC predict maintenance: {}", request.getVin());

        return useCase.predict(request.getVin(), request.getLookbackDays())
            .map(mapper::toProto);
    }

    @Override
    public Uni<GetPredictionHistoryResponse> getPredictionHistory(GetPredictionHistoryRequest request) {
        log.debug("gRPC get prediction history: {}", request.getVin());

        return useCase.getHistory(request.getVin(), request.getLimit())
            .map(list -> GetPredictionHistoryResponse.newBuilder()
                .addAllPredictions(list.stream().map(mapper::toProto).toList())
                .build());
    }

    @Override
    public Uni<RecordMaintenanceEventResponse> recordMaintenanceEvent(RecordMaintenanceEventRequest request) {
        log.info("gRPC record maintenance event: {}", request.getVin());

        var domainRecord = mapper.toDomain(request);

        return useCase.recordEvent(domainRecord)
            .map(mapper::toRecordEventResponse);
    }
}
