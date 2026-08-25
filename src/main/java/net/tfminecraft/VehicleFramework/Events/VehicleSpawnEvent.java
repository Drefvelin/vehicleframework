package net.tfminecraft.VehicleFramework.Events;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import net.tfminecraft.VehicleFramework.Vehicles.ActiveVehicle;

public final class VehicleSpawnEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    private final ActiveVehicle vehicle;

    public VehicleSpawnEvent(ActiveVehicle vehicle) {
        this.vehicle = vehicle;
    }

    public ActiveVehicle getVehicle() {
        return vehicle;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }
}
