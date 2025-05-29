package net.tfminecraft.VehicleFramework.Vehicles.Fuel;

import org.bukkit.configuration.ConfigurationSection;

import me.Plugins.TLibs.Objects.API.SubAPI.StringFormatter;

public class Fuel {
    private String name;
    private String id;
    private String item;
    private int amount;

    public Fuel(String key, ConfigurationSection config) {
        id = key;
        name = StringFormatter.formatHex(config.getString("name", key));
        item = config.getString("item", "v.bedrock");
        amount = config.getInt("amount", 100);
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
}
