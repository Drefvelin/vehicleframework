package net.tfminecraft.VehicleFramework.Vehicles.Component.Fuel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;

import org.junit.jupiter.api.Test;

import net.tfminecraft.VehicleFramework.Vehicles.Component.Propulsion.Throttle;

class FuelTankTest {

    @Test
    void tick_reverseGear_doesNotInstantlyDrainFuel() {
        FuelTank tank = new FuelTank(500, 500, 1, new ArrayList<>(), null);
        Throttle reverse = new Throttle("Reverse", 0, -20, null);
        reverse.setThrottle(-10);

        tank.tick(reverse);

        assertTrue(tank.getCurrent() > 0, "reverse throttle must not drain entire tank in one tick");
        assertEquals(499, tank.getCurrent(), 0.001);
    }

    @Test
    void tick_forwardGear_burnsAtConfiguredRate() {
        FuelTank tank = new FuelTank(500, 500, 1, new ArrayList<>(), null);
        Throttle forward = new Throttle("First", 40, 0, null);
        forward.setThrottle(20);

        tank.tick(forward);

        assertEquals(499, tank.getCurrent(), 0.001);
    }

    @Test
    void tick_idleThrottle_doesNotBurnFuel() {
        FuelTank tank = new FuelTank(500, 500, 1, new ArrayList<>(), null);
        Throttle reverse = new Throttle("Reverse", 0, -20, null);

        tank.tick(reverse);

        assertEquals(500, tank.getCurrent(), 0.001);
    }
}
