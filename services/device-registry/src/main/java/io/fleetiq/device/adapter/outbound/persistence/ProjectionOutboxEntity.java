package io.fleetiq.device.adapter.outbound.persistence;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "projection_outbox")
public class ProjectionOutboxEntity extends PanacheEntityBase {
    @Id public UUID id;
    @Column(name = "event_type", nullable = false) public String eventType;
    @Column(nullable = false) public byte[] payload;
    @Column(name = "created_at", nullable = false) public Instant createdAt;
}
