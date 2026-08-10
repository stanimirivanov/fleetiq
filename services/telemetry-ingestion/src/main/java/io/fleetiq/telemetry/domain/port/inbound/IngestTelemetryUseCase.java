package io.fleetiq.telemetry.domain.port.inbound;

import io.fleetiq.telemetry.domain.model.TelemetrySample;

public interface IngestTelemetryUseCase {

    record IngestResult(boolean accepted, String message) {}

    IngestResult ingest(TelemetrySample sample);
}
