package net.tfminecraft.VehicleFramework.Events;

import org.bukkit.Location;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import net.tfminecraft.VehicleFramework.Cache.Cache;

public class VFExplosionEvent extends Event implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();

    /*
    private ActiveVehicle vehicle;
    private Ammunition ammo;
    */
    private Location location;
    private boolean blockDamage;
    private boolean cancelled;

    public VFExplosionEvent(Location location) {
        //this.vehicle = vehicle;
        this.location = location;
        //this.ammo = ammo;
        this.blockDamage = Cache.blockDamage; // Default value from Cache
        this.cancelled = false;
    }

    /*
    public ActiveVehicle getVehicle() {
        return vehicle;
    }

    public Ammunition getAmmo() {
        return ammo;
    }
    */

    public Location getLocation() {
        return location;
    }

    public boolean doesBlockDamage() {
        return blockDamage;
    }

    public void setBlockDamage(boolean blockDamage) {
        this.blockDamage = blockDamage;
    }

    /* Maybe later, i would have to verify the new location is loaded and im lazy tn
    public void setLocation(Location location) {
        this.location = location;
    }
    */
    
    // Cancellable Implementation
    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    // Required HandlerList methods
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }
}
