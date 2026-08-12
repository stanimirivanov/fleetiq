package io.fleetiq.streaming.adapter.inbound.grpc;

import io.fleetiq.proto.common.v1.GeoPoint;
import io.fleetiq.proto.common.v1.Timestamp;
import io.fleetiq.proto.common.v1.VehicleStatus;
import io.fleetiq.proto.streaming.v1.PositionUpdate;
import io.fleetiq.streaming.domain.model.PositionEvent;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Locale;

@ApplicationScoped
public class GrpcPositionMapper {

    public PositionUpdate toProto(PositionEvent event) {
        return PositionUpdate.newBuilder()
            .setVin(event.vin())
            .setTimestamp(Timestamp.newBuilder().setEpochMillis(event.observedAt().toEpochMilli()))
            .setPosition(GeoPoint.newBuilder()
                .setLatitude(event.latitude())
                .setLongitude(event.longitude())
                .setAltitude(event.altitude()))
            .setSpeedKmh(event.speedKmh())
            .setStatus(toStatus(event.status()))
            .build();
    }

    private VehicleStatus toStatus(String status) {
        if (status == null || status.isBlank()) {
            return VehicleStatus.VEHICLE_STATUS_UNSPECIFIED;
        }
        try {
            return VehicleStatus.valueOf("VEHICLE_STATUS_" + status.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return VehicleStatus.VEHICLE_STATUS_UNSPECIFIED;
        }
    }
}
