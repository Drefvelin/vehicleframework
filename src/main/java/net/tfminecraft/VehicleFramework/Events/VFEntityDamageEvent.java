package net.tfminecraft.VehicleFramework.Events;

import org.bukkit.entity.Entity;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class VFEntityDamageEvent extends Event implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();

    private final Entity entity;
    private final Entity damager;
    private final String cause;
    private double damage;
    private boolean cancelled;

    public VFEntityDamageEvent(Entity entity, Entity damager, String cause, double damage) {
        this.entity = entity;
        this.damager = damager;
        this.cause = cause;
        this.damage = damage;
        this.cancelled = false;
    }

    // Getters
    public Entity getEntity() {
        return entity;
    }
    public Entity getDamager() {
    	return damager;
    }

    public String getCause() {
        return cause;
    }

    public double getDamage() {
        return damage;
    }

    public void setDamage(double damage) {
        this.damage = damage;
    }

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

