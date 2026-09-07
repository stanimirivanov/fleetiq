package io.fleetiq.pekko.api;

import java.util.regex.Pattern;

/** Shared validation used by the public vehicle-state commands before they reach an actor. */
public final class VehicleStateValidation {
    private static final Pattern VIN = Pattern.compile("[A-HJ-NPR-Z0-9]{17}");

    private VehicleStateValidation() {}

    /** Rejects values that are not 17-character VINs or contain the excluded letters I, O, or Q. */
    public static void validateVin(String vin) {
        if (vin == null || !VIN.matcher(vin).matches()) {
            throw new IllegalArgumentException("VIN must contain 17 characters and exclude I, O and Q");
        }
    }
}
