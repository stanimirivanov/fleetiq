package io.fleetiq.maintenance.adapter.outbound.persistence;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "maintenance_events")
public class MaintenanceEventEntity extends PanacheEntityBase {

    @Id
    @Column(name = "event_id")
    public UUID eventId;

    @Column(name = "tenant_id", nullable = false, length = 100)
    public String tenantId;

    @Column(name = "vin", nullable = false, length = 17)
    public String vin;

    @Column(name = "component", length = 100)
    public String component;

    @Column(name = "description", columnDefinition = "TEXT")
    public String description;

    @Column(name = "severity", nullable = false, length = 20)
    public String severity;

    @Column(name = "occurred_at", nullable = false)
    public Instant occurredAt;

    @CreationTimestamp
    @Column(name = "recorded_at", nullable = false, updatable = false)
    public Instant recordedAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    public Map<String, String> metadata;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "telemetry_snapshot", columnDefinition = "jsonb")
    public Map<String, Object> telemetrySnapshot;
}
