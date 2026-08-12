package io.fleetiq.telemetry.adapter.inbound.grpc;

import io.fleetiq.proto.telemetry.v1.*;
import io.fleetiq.telemetry.domain.port.inbound.IngestTelemetryUseCase;
import io.fleetiq.telemetry.domain.port.inbound.IngestTelemetryUseCase.IngestResult;
import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class GrpcTelemetryAdapter extends MutinyTelemetryIngestionGrpc.TelemetryIngestionImplBase {

    private final IngestTelemetryUseCase useCase;
    private final TelemetryGrpcMapper mapper;

    @Override
    public Uni<IngestTelemetryResponse> ingestTelemetry(IngestTelemetryRequest request) {
        var sample = mapper.toDomain(request.getSample());

        return useCase.ingest(sample)
            .map(result -> IngestTelemetryResponse.newBuilder()
                .setAccepted(result.accepted())
                .setMessage(result.message())
                .build());
    }

    @Override
    public Uni<IngestBatchResponse> ingestBatch(Multi<io.fleetiq.proto.telemetry.v1.TelemetrySample> requestStream) {
        return requestStream
            .onItem().transform(mapper::toDomain)
            // ✅ Concatenate processes incoming items sequentially with backpressure controls
            .onItem().transformToUniAndConcatenate(useCase::ingest)
            // ✅ Accumulate stream results safely without multi-threading race conditions
            .collect().in(BatchAccumulator::new, BatchAccumulator::accumulate)
            .map(BatchAccumulator::toResponse);
    }

    // Helper accumulator for thread-safe stream reduction
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
