package io.fleetiq.maintenance.adapter.outbound.telemetry;

import io.fleetiq.maintenance.domain.model.TelemetryReading;
import io.fleetiq.maintenance.domain.model.TelemetryWindow;
import io.fleetiq.maintenance.domain.port.outbound.TelemetryWindowSource;
import io.fleetiq.proto.common.v1.Timestamp;
import io.fleetiq.proto.telemetry.v1.GetTelemetryWindowRequest;
import io.fleetiq.proto.telemetry.v1.MutinyTelemetryIngestionGrpc;
import io.grpc.Metadata;
import io.grpc.stub.MetadataUtils;
import io.quarkus.grpc.GrpcClient;
import io.quarkus.security.identity.SecurityIdentity;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.time.Instant;

/** gRPC adapter that preserves the caller's bearer token at the telemetry boundary. */
@ApplicationScoped
public class GrpcTelemetryWindowSource implements TelemetryWindowSource {

    private static final Metadata.Key<String> AUTHORIZATION = Metadata.Key.of(
        "Authorization", Metadata.ASCII_STRING_MARSHALLER);

    @Inject
    @GrpcClient("telemetry")
    MutinyTelemetryIngestionGrpc.MutinyTelemetryIngestionStub client;

    @Inject
    SecurityIdentity securityIdentity;

    @Override
    public Uni<TelemetryWindow> load(String tenantId, String vin, Instant from, Instant to) {
        Metadata headers = new Metadata();
        if (!(securityIdentity.getPrincipal() instanceof JsonWebToken token)
            || token.getRawToken() == null || token.getRawToken().isBlank()) {
            return Uni.createFrom().failure(new SecurityException("A bearer token is required for telemetry access"));
        }
        headers.put(AUTHORIZATION, "Bearer " + token.getRawToken());
        var request = GetTelemetryWindowRequest.newBuilder()
            .setVin(vin)
            .setFrom(timestamp(from))
            .setTo(timestamp(to))
            .build();

        return client.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(headers))
            .getTelemetryWindow(request)
            .map(response -> new TelemetryWindow(
                vin,
                from,
                to,
                response.getSamplesList().stream()
                    .map(sample -> new TelemetryReading(
                        Instant.ofEpochMilli(sample.getTimestamp().getEpochMillis()),
                        sample.getEngineTempCelsius(),
                        sample.getBatteryVoltage(),
                        sample.getSpeedKmh()))
                    .toList()
            ));
    }

    private static Timestamp timestamp(Instant value) {
        return Timestamp.newBuilder().setEpochMillis(value.toEpochMilli()).build();
    }
}
