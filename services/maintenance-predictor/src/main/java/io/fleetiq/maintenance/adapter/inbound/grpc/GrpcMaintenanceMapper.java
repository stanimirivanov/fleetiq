package io.fleetiq.maintenance.adapter.inbound.grpc;

import io.fleetiq.maintenance.domain.model.MaintenanceRecord;
import io.fleetiq.maintenance.domain.model.PredictionResult;
import io.fleetiq.maintenance.domain.model.Severity;
import io.fleetiq.proto.common.v1.Timestamp;
import io.fleetiq.proto.maintenance.v1.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.Named;

import java.time.Instant;
import java.util.UUID;

@Mapper(componentModel = MappingConstants.ComponentModel.JAKARTA)
public interface GrpcMaintenanceMapper {

    // --- Inbound: Proto Request -> Domain Model ---

    @Mapping(target = "eventId", expression = "java(java.util.UUID.randomUUID())")
    @Mapping(target = "severity", source = "severity", qualifiedByName = "stringToSeverity")
    @Mapping(target = "occurredAt", source = "occurredAt")
    @Mapping(target = "recordedAt", ignore = true)
    @Mapping(target = "telemetrySnapshot", ignore = true)
    MaintenanceRecord toDomain(RecordMaintenanceEventRequest request);


    // --- Outbound: Domain Model -> Proto Response ---

    @Mapping(target = "predictionId", source = "predictionId", qualifiedByName = "uuidToString")
    @Mapping(target = "generatedAt", expression = "java(mapInstantToTimestamp(java.time.Instant.now()))")
    PredictMaintenanceResponse toProto(PredictionResult domain);

    RecordMaintenanceEventResponse toRecordEventResponse(MaintenanceRecord domain);


    // --- Custom Type Mapping Helpers ---

    default Instant mapTimestampToInstant(Timestamp timestamp) {
        if (timestamp == null || timestamp.getEpochMillis() == 0) {
            return null;
        }
        return Instant.ofEpochMilli(timestamp.getEpochMillis());
    }

    default Timestamp mapInstantToTimestamp(Instant instant) {
        if (instant == null) {
            return Timestamp.getDefaultInstance();
        }
        return Timestamp.newBuilder()
            .setEpochMillis(instant.toEpochMilli())
            .build();
    }

    @Named("uuidToString")
    default String uuidToString(UUID uuid) {
        return uuid != null ? uuid.toString() : "";
    }

    @Named("stringToSeverity")
    default Severity stringToSeverity(String severity) {
        if (severity == null || severity.isBlank()) {
            return Severity.LOW; // or handle default
        }
        return Severity.valueOf(severity.toUpperCase());
    }
}
