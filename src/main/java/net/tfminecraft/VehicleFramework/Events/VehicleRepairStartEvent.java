package net.tfminecraft.VehicleFramework.Events;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

import net.tfminecraft.VehicleFramework.Vehicles.ActiveVehicle;

/**
 * Fired when a player starts a vehicle repair (repair item), before seat/speed
 * checks and the repair GUI. Cancel to block repair without opening the window.
 */
public final class VehicleRepairStartEvent extends PlayerEvent implements Cancellable {
	private static final HandlerList HANDLERS = new HandlerList();

	private final ActiveVehicle vehicle;
	private boolean cancelled;

	public VehicleRepairStartEvent(Player player, ActiveVehicle vehicle) {
		super(player);
		this.vehicle = vehicle;
	}

	public ActiveVehicle getVehicle() {
		return vehicle;
	}

	@Override
	public boolean isCancelled() {
		return cancelled;
	}

	@Override
	public void setCancelled(boolean cancelled) {
		this.cancelled = cancelled;
	}

	@Override
	public HandlerList getHandlers() {
		return HANDLERS;
	}

	public static HandlerList getHandlerList() {
		return HANDLERS;
	}
}
