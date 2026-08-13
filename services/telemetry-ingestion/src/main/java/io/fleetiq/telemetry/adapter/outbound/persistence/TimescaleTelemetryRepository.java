package io.fleetiq.telemetry.adapter.outbound.persistence;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.fleetiq.telemetry.domain.model.TelemetrySample;
import io.fleetiq.telemetry.domain.port.outbound.TelemetryRepository;
import io.fleetiq.proto.events.v1.PositionProjectionEvent;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.pgclient.PgPool;
import io.vertx.mutiny.sqlclient.Row;
import io.vertx.mutiny.sqlclient.RowSet;
import io.vertx.mutiny.sqlclient.Tuple;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.stream.StreamSupport;
import java.util.UUID;

@ApplicationScoped
@RequiredArgsConstructor
public class TimescaleTelemetryRepository implements TelemetryRepository {

    private final PgPool pgPool;
    private final ObjectMapper objectMapper; // ✅ Injected CDI Bean

    private static final TypeReference<Map<String, Double>> DOUBLE_MAP_TYPE = new TypeReference<>() {};

    private static final String INSERT_SQL = """
        INSERT INTO telemetry_samples (time, tenant_id, vin, latitude, longitude, altitude,
            speed_kmh, fuel_level_pct, engine_temp_celsius, battery_voltage, custom_metrics)
        VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11::jsonb)
        """;

    private static final String SELECT_RANGE_SQL = """
        SELECT time, vin, latitude, longitude, altitude,
            speed_kmh, fuel_level_pct, engine_temp_celsius, battery_voltage, custom_metrics
        FROM telemetry_samples
        WHERE tenant_id = $1 AND vin = $2 AND time >= $3 AND time <= $4
        ORDER BY time DESC
        LIMIT 1000
        """;

    private static final String INSERT_OUTBOX_SQL = """
        INSERT INTO projection_outbox (id, event_type, payload)
        VALUES ($1, 'position-projection.v1', $2)
        """;

    private static final String AVERAGE_SPEED_SQL = """
        SELECT AVG(speed_kmh) as avg_speed
        FROM telemetry_samples
        WHERE tenant_id = $1 AND vin = $2 AND time >= $3 AND time <= $4
        """;

    @Override
    public Uni<Void> save(String tenantId, TelemetrySample sample) {
        Tuple tuple = Tuple.tuple()
            // ✅ Preserve UTC offset using OffsetDateTime for TIMESTAMPTZ
            .addOffsetDateTime(OffsetDateTime.ofInstant(sample.timestamp(), ZoneOffset.UTC))
            .addString(tenantId)
            .addString(sample.vin())
            .addDouble(sample.latitude())
            .addDouble(sample.longitude())
            .addDouble(sample.altitude())
            .addDouble(sample.speedKmh())
            .addDouble(sample.fuelLevelPct())
            .addDouble(sample.engineTempCelsius())
            .addDouble(sample.batteryVoltage())
            .addString(toJsonString(sample.customMetrics()));

        byte[] event = PositionProjectionEvent.newBuilder()
            .setVin(sample.vin())
            .setLatitude(sample.latitude())
            .setLongitude(sample.longitude())
            .setAltitude(sample.altitude())
            .setObservedAtEpochMillis(sample.timestamp().toEpochMilli())
            .setTenantId(tenantId)
            .build().toByteArray();

        return pgPool.withTransaction(connection -> connection.preparedQuery(INSERT_SQL)
            .execute(tuple)
            .call(() -> connection.preparedQuery(INSERT_OUTBOX_SQL)
                .execute(Tuple.of(UUID.randomUUID(), event))))
            .replaceWithVoid();
    }

    @Override
    public Uni<List<TelemetrySample>> findByVinAndTimeRange(String tenantId, String vin, Instant from, Instant to) {
        Tuple tuple = Tuple.tuple()
            .addString(tenantId)
            .addString(vin)
            .addOffsetDateTime(OffsetDateTime.ofInstant(from, ZoneOffset.UTC))
            .addOffsetDateTime(OffsetDateTime.ofInstant(to, ZoneOffset.UTC));

        return pgPool.preparedQuery(SELECT_RANGE_SQL)
            .execute(tuple)
            .map(this::mapRowsToSamples);
    }

    @Override
    public Uni<Double> getAverageSpeed(String tenantId, String vin, Instant from, Instant to) {
        Tuple tuple = Tuple.tuple()
            .addString(tenantId)
            .addString(vin)
            .addOffsetDateTime(OffsetDateTime.ofInstant(from, ZoneOffset.UTC))
            .addOffsetDateTime(OffsetDateTime.ofInstant(to, ZoneOffset.UTC));

        return pgPool.preparedQuery(AVERAGE_SPEED_SQL)
            .execute(tuple)
            .map(rows -> {
                if (rows.size() == 0) return 0.0;
                Row row = rows.iterator().next();
                Double avg = row.getDouble("avg_speed");
                return avg != null ? avg : 0.0;
            });
    }

    private List<TelemetrySample> mapRowsToSamples(RowSet<Row> rows) {
        return StreamSupport.stream(rows.spliterator(), false)
            .map(row -> TelemetrySample.builder()
                .vin(row.getString("vin"))
                .timestamp(row.getOffsetDateTime("time").toInstant())
                .latitude(row.getDouble("latitude"))
                .longitude(row.getDouble("longitude"))
                .altitude(row.getDouble("altitude"))
                .speedKmh(row.getDouble("speed_kmh"))
                .fuelLevelPct(row.getDouble("fuel_level_pct"))
                .engineTempCelsius(row.getDouble("engine_temp_celsius"))
                .batteryVoltage(row.getDouble("battery_voltage"))
                .customMetrics(parseJsonMap(row.getString("custom_metrics")))
                .build())
            .toList();
    }

    private String toJsonString(Map<String, Double> map) {
        if (map == null || map.isEmpty()) return "{}";
        try {
            return objectMapper.writeValueAsString(map);
        } catch (Exception e) {
            throw new IllegalArgumentException("Custom metrics cannot be serialized", e);
        }
    }

    private Map<String, Double> parseJsonMap(String json) {
        if (json == null || json.isEmpty() || "{}".equals(json)) return Map.of();
        try {
            // ✅ TypeReference guarantees Double parsing
            return objectMapper.readValue(json, DOUBLE_MAP_TYPE);
        } catch (Exception e) {
            throw new IllegalStateException("Stored custom metrics are invalid JSON", e);
        }
    }
}
