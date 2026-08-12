package io.fleetiq.device.adapter.outbound.persistence;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;

@Entity
@Table(name = "devices")
public class DeviceEntity extends PanacheEntityBase {

    @Id
    @Column(name = "vin", length = 17)
    public String vin;

    @Column(name = "device_type", nullable = false, length = 50)
    public String deviceType;

    @Column(name = "manufacturer", length = 100)
    public String manufacturer;

    @Column(name = "model", length = 100)
    public String model;

    @Column(name = "year")
    public int year;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "capabilities", columnDefinition = "jsonb")
    public Map<String, String> capabilities;

    @Column(name = "status", nullable = false, length = 20)
    public String status;

    @CreationTimestamp
    @Column(name = "registered_at", nullable = false)
    public Instant registeredAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    public Instant updatedAt;
}
