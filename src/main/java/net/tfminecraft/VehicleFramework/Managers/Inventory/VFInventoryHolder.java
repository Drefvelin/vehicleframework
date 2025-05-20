package net.tfminecraft.VehicleFramework.Managers.Inventory;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import net.tfminecraft.VehicleFramework.Enums.VFGUI;

public class VFInventoryHolder implements InventoryHolder {
    private final String id;
    private final VFGUI type;

    public VFInventoryHolder(String id, VFGUI type) {
        this.id = id;
        this.type = type;
    }

    public String getId() {
        return id;
    }
    
    public VFGUI getType() {
    	return type;
    }

    @Override
    public Inventory getInventory() {
        return null; // Not used in this case
    }
}
