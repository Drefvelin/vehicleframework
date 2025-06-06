package net.tfminecraft.VehicleFramework.Bones;

import org.bukkit.configuration.ConfigurationSection;

public class RotationLimits {

    private boolean yawEnabled = false;
    private boolean pitchEnabled = false;
    private boolean rollEnabled = false;

    private float minYaw;
    private float maxYaw;
    private float minPitch;
    private float maxPitch;
    private float minRoll;
    private float maxRoll;

    public RotationLimits(ConfigurationSection config) {
        if (config.isSet("min-yaw") || config.isSet("max-yaw")) {
            yawEnabled = true;
            minYaw = (float) config.getDouble("min-yaw", -180.0);
            maxYaw = (float) config.getDouble("max-yaw", 180.0);
        }

        if (config.isSet("min-pitch") || config.isSet("max-pitch")) {
            pitchEnabled = true;
            minPitch = (float) config.getDouble("min-pitch", -90.0);
            maxPitch = (float) config.getDouble("max-pitch", 90.0);
        }

        if (config.isSet("min-roll") || config.isSet("max-roll")) {
            rollEnabled = true;
            minRoll = (float) config.getDouble("min-roll", -180.0);
            maxRoll = (float) config.getDouble("max-roll", 180.0);
        }
    }

    public RotationLimits() {
        // No limits at all
        yawEnabled = false;
        pitchEnabled = false;
        rollEnabled = false;
    }

    public boolean withinAll(float yaw, float pitch, float roll) {
        return withinYaw(yaw) && withinPitch(pitch) && withinRoll(roll);
    }

    public boolean withinYaw(float yaw) {
        if (!yawEnabled) return true;
        return within(yaw, minYaw, maxYaw);
    }

    public boolean withinPitch(float pitch) {
        if (!pitchEnabled) return true;
        return within(pitch, minPitch, maxPitch);
    }

    public boolean withinRoll(float roll) {
        if (!rollEnabled) return true;
        return within(roll, minRoll, maxRoll);
    }

    private boolean within(float value, float min, float max) {
        return value >= min && value <= max;
    }

    // Optional: clamping functions (for convenience)
    public float clampYaw(float yaw) {
        if (!yawEnabled) return yaw;
        return clamp(yaw, minYaw, maxYaw);
    }

    public float clampPitch(float pitch) {
        if (!pitchEnabled) return pitch;
        return clamp(pitch, minPitch, maxPitch);
    }

    public float clampRoll(float roll) {
        if (!rollEnabled) return roll;
        return clamp(roll, minRoll, maxRoll);
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    public float getMinYaw() { return minYaw; }
    public float getMaxYaw() { return maxYaw; }
    public float getMinPitch() { return minPitch; }
    public float getMaxPitch() { return maxPitch; }
    public float getMinRoll() { return minRoll; }
    public float getMaxRoll() { return maxRoll; }
}


