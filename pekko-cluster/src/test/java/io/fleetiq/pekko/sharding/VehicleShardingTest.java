package io.fleetiq.pekko.sharding;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class VehicleShardingTest {

    @Test
    void createsDistinctRoundTrippableEntityIdsPerTenant() {
        String vin = "1HGCM82633A004352";
        String first = VehicleSharding.encode("tenant-a", vin);
        String second = VehicleSharding.encode("tenant-b", vin);

        assertNotEquals(first, second);
        assertEquals("tenant-a", VehicleSharding.decode(first).tenantId());
        assertEquals(vin, VehicleSharding.decode(first).vin());
    }
}
