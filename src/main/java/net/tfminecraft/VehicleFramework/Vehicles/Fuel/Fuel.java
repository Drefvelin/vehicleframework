package net.tfminecraft.VehicleFramework.Vehicles.Fuel;

import org.bukkit.configuration.ConfigurationSection;

import me.Plugins.TLibs.Objects.API.SubAPI.StringFormatter;

public class Fuel {
    private String name;
    private String id;
    private String item;
    private int amount;
    private boolean refuelWhileRunning;
    private String sound;
    private float soundVolume;
    private float soundPitch;

    public Fuel(String key, ConfigurationSection config) {
        id = key;
        name = StringFormatter.formatHex(config.getString("name", key));
        item = config.getString("item", "v.bedrock");
        amount = config.getInt("amount", 100);
        refuelWhileRunning = config.getBoolean("refuel-while-running", false);
        sound = config.getString("sound", "");
        soundVolume = (float) config.getDouble("sound-volume", 1.0);
        soundPitch = (float) config.getDouble("sound-pitch", 0.8);
    }

    public String getName() {
        return name;
    }

    public String getId() {
        return id;
    }

    public String getItem() {
        return item;
    }

    public int getAmount() {
        return amount;
    }

    public boolean refuelWhileRunning() {
        return refuelWhileRunning;
    }

    public String getSound() {
        return sound;
    }

    public float getSoundVolume() {
        return soundVolume;
    }

    public float getSoundPitch() {
        return soundPitch;
    }
}
