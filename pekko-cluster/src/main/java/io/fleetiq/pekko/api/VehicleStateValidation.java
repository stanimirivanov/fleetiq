package io.fleetiq.pekko.api;

import java.util.regex.Pattern;

public final class VehicleStateValidation {
    private static final Pattern VIN = Pattern.compile("[A-HJ-NPR-Z0-9]{17}");

    private VehicleStateValidation() {}

    public static void validateVin(String vin) {
        if (vin == null || !VIN.matcher(vin).matches()) {
            throw new IllegalArgumentException("VIN must contain 17 characters and exclude I, O and Q");
        }
    }
}
