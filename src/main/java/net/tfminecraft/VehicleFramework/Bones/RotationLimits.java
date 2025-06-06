package net.tfminecraft.VehicleFramework.Bones;

import org.bukkit.configuration.ConfigurationSection;

public class RotationLimits {

    private boolean empty = false;

    private float minYaw;
    private float maxYaw;
    private float minPitch;
    private float maxPitch;
    private float minRoll;
    private float maxRoll;

    public RotationLimits(ConfigurationSection config) {
        this.minYaw = (float) config.getDouble("min-yaw", -180.0);
        this.maxYaw = (float) config.getDouble("max-yaw", 180.0);
        this.minPitch = (float) config.getDouble("min-pitch", -90.0);
        this.maxPitch = (float) config.getDouble("max-pitch", 90.0);
        this.minRoll = (float) config.getDouble("min-roll", -180.0);
        this.maxRoll = (float) config.getDouble("max-roll", 180.0);
    }

    public RotationLimits() {
        empty = true;
    }

    public float clampYaw(float yaw) {
        if(empty) return yaw;
        return clamp(yaw, minYaw, maxYaw);
    }

    public float clampPitch(float pitch) {
        if(empty) return pitch;
        return clamp(pitch, minPitch, maxPitch);
    }

    public float clampRoll(float roll) {
        if(empty) return roll;
        return clamp(roll, minRoll, maxRoll);
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}

