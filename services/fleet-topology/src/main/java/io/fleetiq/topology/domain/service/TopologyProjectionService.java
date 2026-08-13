package io.fleetiq.topology.domain.service;

import io.fleetiq.topology.domain.model.VehicleProjection;
import io.fleetiq.topology.domain.port.inbound.TopologyProjectionUseCase;
import io.fleetiq.topology.domain.port.outbound.TopologyRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;

import java.time.Instant;

@ApplicationScoped
@RequiredArgsConstructor
public class TopologyProjectionService implements TopologyProjectionUseCase {
    private final TopologyRepository repository;

    @Override
    public Uni<Void> projectDevice(VehicleProjection vehicle) {
        return repository.upsertVehicle(vehicle);
    }

    @Override
    public Uni<Void> projectPosition(String vin, double latitude, double longitude, double altitude,
                                     Instant observedAt) {
        return repository.updatePosition(vin, latitude, longitude, altitude, observedAt);
    }
}
