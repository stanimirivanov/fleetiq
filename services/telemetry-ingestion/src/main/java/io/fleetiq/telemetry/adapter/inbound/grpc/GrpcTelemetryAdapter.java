package io.fleetiq.telemetry.adapter.inbound.grpc;

import io.fleetiq.proto.telemetry.v1.*;
import io.fleetiq.telemetry.domain.port.inbound.IngestTelemetryUseCase;
import io.fleetiq.telemetry.domain.port.inbound.IngestTelemetryUseCase.IngestResult;
import io.fleetiq.security.TenantSecured;
import io.fleetiq.security.CurrentTenant;
import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import jakarta.annotation.security.RolesAllowed;

/**
 * Authenticated gRPC boundary for unary, client-streaming, and windowed telemetry operations.
 * Stream items are processed sequentially so downstream demand provides backpressure and the
 * batch accumulator never requires concurrent mutation.
 */
@Slf4j
@GrpcService
@RequiredArgsConstructor
@TenantSecured
public class GrpcTelemetryAdapter extends MutinyTelemetryIngestionGrpc.TelemetryIngestionImplBase {

    private final IngestTelemetryUseCase useCase;
    private final TelemetryGrpcMapper mapper;
    private final CurrentTenant currentTenant;

    @Override
    @RolesAllowed({"device", "service"})
    public Uni<IngestTelemetryResponse> ingestTelemetry(IngestTelemetryRequest request) {
        var sample = mapper.toDomain(request.getSample());

        return useCase.ingest(currentTenant.get().tenantId(), sample)
            .map(result -> IngestTelemetryResponse.newBuilder()
                .setAccepted(result.accepted())
                .setMessage(result.message())
                .build());
    }

    @Override
    @RolesAllowed({"device", "service"})
    public Uni<IngestBatchResponse> ingestBatch(Multi<io.fleetiq.proto.telemetry.v1.TelemetrySample> requestStream) {
        return requestStream
            .onItem().transform(mapper::toDomain)
            .onItem().transformToUniAndConcatenate(sample ->
                useCase.ingest(currentTenant.get().tenantId(), sample))
            .collect().in(BatchAccumulator::new, BatchAccumulator::accumulate)
            .map(BatchAccumulator::toResponse);
    }

    @Override
    @RolesAllowed({"operator", "service"})
    public Uni<GetTelemetryWindowResponse> getTelemetryWindow(GetTelemetryWindowRequest request) {
        var from = mapper.mapTimestampToInstant(request.getFrom());
        var to = mapper.mapTimestampToInstant(request.getTo());
        if (request.getVin().isBlank() || from == null || to == null || !from.isBefore(to)) {
            return Uni.createFrom().failure(new IllegalArgumentException("A VIN and valid telemetry window are required"));
        }

        return useCase.getTelemetryRange(currentTenant.get().tenantId(), request.getVin(), from, to)
            .map(samples -> GetTelemetryWindowResponse.newBuilder()
                .addAllSamples(samples.stream().map(mapper::toProto).toList())
                .build());
    }

    private static class BatchAccumulator {
        private int accepted = 0;
        private int rejected = 0;

        public void accumulate(IngestResult result) {
            if (result.accepted()) {
                accepted++;
            } else {
                rejected++;
            }
        }

        public IngestBatchResponse toResponse() {
            return IngestBatchResponse.newBuilder()
                .setAcceptedCount(accepted)
                .setRejectedCount(rejected)
                .build();
        }
    }
}
