package io.fleetiq.telemetry.adapter.inbound.grpc;

import io.fleetiq.proto.common.v1.GeoPoint;
import io.fleetiq.proto.common.v1.Timestamp;
import io.fleetiq.proto.telemetry.v1.TelemetrySample;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.Instant;

/**
 * Translates telemetry between protobuf and the domain representation.
 * Missing optional protobuf values use boundary defaults: the current instant for an absent sample
 * timestamp and zero coordinates for an absent position.
 */
@ApplicationScoped
public class TelemetryGrpcMapper {

    public Instant mapTimestampToInstant(Timestamp timestamp) {
        if (timestamp == null || timestamp.getEpochMillis() == 0) {
            return null;
        }
        return Instant.ofEpochMilli(timestamp.getEpochMillis());
    }

    public io.fleetiq.telemetry.domain.model.TelemetrySample toDomain(TelemetrySample proto) {
        if (proto == null) {
            return null;
        }

        Instant timestamp = proto.hasTimestamp()
            ? Instant.ofEpochMilli(proto.getTimestamp().getEpochMillis())
            : Instant.now();

        double latitude = proto.hasPosition() ? proto.getPosition().getLatitude() : 0.0;
        double longitude = proto.hasPosition() ? proto.getPosition().getLongitude() : 0.0;
        double altitude = proto.hasPosition() ? proto.getPosition().getAltitude() : 0.0;

        return io.fleetiq.telemetry.domain.model.TelemetrySample.builder()
            .vin(proto.getVin())
            .timestamp(timestamp)
            .latitude(latitude)
            .longitude(longitude)
            .altitude(altitude)
            .speedKmh(proto.getSpeedKmh())
            .fuelLevelPct(proto.getFuelLevelPct())
            .engineTempCelsius(proto.getEngineTempCelsius())
            .batteryVoltage(proto.getBatteryVoltage())
            .customMetrics(proto.getCustomMetricsMap())
            .build();
    }

    public TelemetrySample toProto(io.fleetiq.telemetry.domain.model.TelemetrySample domain) {
        if (domain == null) {
            return TelemetrySample.getDefaultInstance();
        }

        var builder = TelemetrySample.newBuilder()
            .setVin(domain.vin() != null ? domain.vin() : "")
            .setSpeedKmh(domain.speedKmh())
            .setFuelLevelPct(domain.fuelLevelPct())
            .setEngineTempCelsius(domain.engineTempCelsius())
            .setBatteryVoltage(domain.batteryVoltage());

        if (domain.timestamp() != null) {
            builder.setTimestamp(Timestamp.newBuilder()
                .setEpochMillis(domain.timestamp().toEpochMilli())
                .build());
        }

        builder.setPosition(GeoPoint.newBuilder()
            .setLatitude(domain.latitude())
            .setLongitude(domain.longitude())
            .setAltitude(domain.altitude())
            .build());

        if (domain.customMetrics() != null) {
            builder.putAllCustomMetrics(domain.customMetrics());
        }

        return builder.build();
    }
}
