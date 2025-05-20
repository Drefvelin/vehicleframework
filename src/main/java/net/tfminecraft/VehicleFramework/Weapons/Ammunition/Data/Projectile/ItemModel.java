package net.tfminecraft.VehicleFramework.Weapons.Ammunition.Data.Projectile;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class ItemModel implements ProjectileModel{
	private boolean small;
	private Material material;
	private int model;
	
	public ItemModel(ConfigurationSection config) {
		material = Material.valueOf(config.getString("material").toUpperCase());
		small = config.getBoolean("small", false);
		model = config.getInt("model-data", 0);
	}

	public boolean isSmall() {
		return small;
	}

	public Material getMaterial() {
		return material;
	}

	public int getModel() {
		return model;
	}
	@Override
	public double getOffset() {
		if(small) {
			return 1.0;
		}
		return 1.5;
	}
	
	@Override
	public Entity spawn(Location loc) {
		loc = loc.clone().add(0, -1, 0);
		if(small) {
			loc = loc.clone().add(0, 0.5, 0);
		}
        // Spawn an ArmorStand
        ArmorStand armorStand = loc.getWorld().spawn(loc, ArmorStand.class);

        // Set ArmorStand properties
        armorStand.setInvisible(true);
        armorStand.setSmall(small);
        armorStand.setMarker(false);
        armorStand.setGravity(true);

        // Create an ItemStack for the head
        ItemStack itemStack = new ItemStack(material);

        // Apply custom model data if specified
        if (model >= 0) {
            ItemMeta meta = itemStack.getItemMeta();
            if (meta != null) {
                meta.setCustomModelData(model);
                itemStack.setItemMeta(meta);
            }
        }

        // Set the item on the ArmorStand's head
        armorStand.getEquipment().setHelmet(itemStack);

        return armorStand;
    }
}
