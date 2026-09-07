package io.fleetiq.maintenance.adapter.outbound.persistence;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/** Persistence detail for the transactional maintenance-recommendation outbox. */
@Entity
@Table(name = "maintenance_recommendation_outbox")
public class RecommendationOutboxEntity extends PanacheEntityBase {
    @Id
    public UUID id;

    @Column(name = "event_type", nullable = false, length = 100)
    public String eventType;

    @Column(name = "tenant_id", nullable = false, length = 100)
    public String tenantId;

    @Column(name = "prediction_id", nullable = false, unique = true)
    public UUID predictionId;

    @Column(nullable = false, columnDefinition = "BYTEA")
    public byte[] payload;

    @Column(name = "created_at", nullable = false)
    public Instant createdAt;
}
