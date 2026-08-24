package net.tfminecraft.VehicleFramework.Events;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import net.tfminecraft.VehicleFramework.Data.VehicleRemovePayload;
import net.tfminecraft.VehicleFramework.Vehicles.ActiveVehicle;

public class VehicleRemoveEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    private final ActiveVehicle vehicle;
    private final VehicleRemovePayload payload;

    public VehicleRemoveEvent(ActiveVehicle vehicle, VehicleRemovePayload payload) {
        this.vehicle = vehicle;
        this.payload = payload != null
            ? payload
            : VehicleRemovePayload.remove(null);
    }

    public ActiveVehicle getVehicle() {
        return vehicle;
    }

    public VehicleRemovePayload getPayload() {
        return payload;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }
}
