package net.tfminecraft.VehicleFramework.Managers.Inventory;

import java.util.Optional;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import net.tfminecraft.VehicleFramework.Enums.VFGUI;
import net.tfminecraft.VehicleFramework.Vehicles.ActiveVehicle;

public class VFInventoryHolder implements InventoryHolder {
    private final String id;
    private final VFGUI type;
    private Optional<ActiveVehicle> vehicle = Optional.empty();

    public VFInventoryHolder(String id, VFGUI type) {
        this.id = id;
        this.type = type;
    }

    public VFInventoryHolder(String id, VFGUI type, ActiveVehicle v) {
        this.id = id;
        this.type = type;
        this.vehicle = Optional.of(v);
    }

    public String getId() {
        return id;
    }
    
    public VFGUI getType() {
    	return type;
    }

    public Optional<ActiveVehicle> getVehicle() {
        return vehicle;
    }

    @Override
    public Inventory getInventory() {
        return null; // Not used in this case
    }
}
