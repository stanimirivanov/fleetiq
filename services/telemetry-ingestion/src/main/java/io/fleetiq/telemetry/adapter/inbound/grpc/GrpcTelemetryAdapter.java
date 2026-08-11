package io.fleetiq.telemetry.adapter.inbound.grpc;

import io.fleetiq.proto.telemetry.v1.TelemetryIngestionGrpc;
import io.fleetiq.proto.telemetry.v1.IngestTelemetryRequest;
import io.fleetiq.proto.telemetry.v1.IngestTelemetryResponse;
import io.fleetiq.proto.telemetry.v1.IngestBatchResponse;
import io.fleetiq.proto.telemetry.v1.TelemetrySample;
import io.fleetiq.telemetry.domain.port.inbound.IngestTelemetryUseCase;
import io.grpc.stub.StreamObserver;
import io.quarkus.grpc.GrpcService;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@GrpcService
public class GrpcTelemetryAdapter extends TelemetryIngestionGrpc.TelemetryIngestionImplBase {

    private static final Logger log = LoggerFactory.getLogger(GrpcTelemetryAdapter.class);

    @Inject
    IngestTelemetryUseCase useCase;

    @Override
    public void ingestTelemetry(IngestTelemetryRequest request, StreamObserver<IngestTelemetryResponse> responseObserver) {
        log.debug("gRPC ingest telemetry: {}", request.getSample().getVin());
        var response = IngestTelemetryResponse.newBuilder()
            .setAccepted(true)
            .setMessage("Accepted via gRPC")
            .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public StreamObserver<TelemetrySample> ingestBatch(StreamObserver<IngestBatchResponse> responseObserver) {
        log.debug("gRPC ingest batch stream opened");
        return new StreamObserver<>() {
            int count = 0;

            @Override
            public void onNext(TelemetrySample sample) {
                count++;
                log.debug("Batch sample: {}", sample.getVin());
            }

            @Override
            public void onError(Throwable t) {
                log.error("Batch stream error", t);
            }

            @Override
            public void onCompleted() {
                var response = IngestBatchResponse.newBuilder()
                    .setAcceptedCount(count)
                    .build();
                responseObserver.onNext(response);
                responseObserver.onCompleted();
            }
        };
    }
}
