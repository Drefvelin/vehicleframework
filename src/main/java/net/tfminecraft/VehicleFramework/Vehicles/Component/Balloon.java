package net.tfminecraft.VehicleFramework.Vehicles.Component;

import java.util.List;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import com.ticxo.modelengine.api.model.ActiveModel;

import net.tfminecraft.VehicleFramework.Database.IncompleteComponent;
import net.tfminecraft.VehicleFramework.Enums.Component;
import net.tfminecraft.VehicleFramework.Vehicles.ActiveVehicle;

public class Balloon extends VehicleComponent {
    private double lift;
    private double delta = 0.0;

    public Balloon(ConfigurationSection config) {
        super(Component.BALLOON, config);
        lift = config.getDouble("lift", 0.5);
    }

    public Balloon(Balloon another, ActiveVehicle v, ActiveModel m, IncompleteComponent ic) {
        super(another, v, m, ic);
        lift = another.getBaseLift();
        delta = another.getDelta(); // Carry over delta if needed
    }

    @Override
    public void slowTick(List<Player> nearby) {
        super.slowTick(nearby);
    }

    public double getBaseLift() {
        return lift;
    }

    public void setDelta(double delta) {
        this.delta = delta;
    }

    public double getDelta() {
        double base = getBaseLift();
        double decay = base * 0.10; // 10% of base lift

        // Smoothly reduce delta toward 0
        if (delta > 0) {
            delta = Math.max(0, delta - decay);
        } else if (delta < 0) {
            delta = Math.min(0, delta + decay);
        }

        return delta;
    }

    public double getLift() {
        
        double healthPercent = healthData.getHealthPercentage();
        if(v.isDestroyed()) healthPercent = 0;
        double h = healthPercent / 100.0; // normalize to [0, 1]

        double liftFactor = -6 * h * h + 11 * h - 4;
        return lift * liftFactor;
    }
}
