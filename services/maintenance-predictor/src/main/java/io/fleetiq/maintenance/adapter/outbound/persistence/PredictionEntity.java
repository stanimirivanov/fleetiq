package io.fleetiq.maintenance.adapter.outbound.persistence;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "maintenance_predictions")
public class PredictionEntity extends PanacheEntityBase {

    @Id
    @Column(name = "prediction_id")
    public UUID predictionId;

    @Column(name = "tenant_id", nullable = false, length = 100)
    public String tenantId;

    @Column(name = "vin", nullable = false, length = 17)
    public String vin;

    @Column(name = "failure_probability", nullable = false)
    public double failureProbability;

    @Column(name = "predicted_component", length = 100)
    public String predictedComponent;

    @Column(name = "estimated_days_until_failure")
    public int estimatedDaysUntilFailure;

    @Column(name = "recommendation", columnDefinition = "TEXT")
    public String recommendation;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "evidence_ids", columnDefinition = "text[]")
    public List<String> evidenceIds;

    @CreationTimestamp
    @Column(name = "generated_at", nullable = false, updatable = false)
    public Instant generatedAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "actual_outcome", columnDefinition = "jsonb")
    public Map<String, Object> actualOutcome;

    @Column(name = "outcome_recorded_at")
    public Instant outcomeRecordedAt;
}
