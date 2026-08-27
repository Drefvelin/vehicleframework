package net.tfminecraft.VehicleFramework.Events;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

import net.tfminecraft.VehicleFramework.Vehicles.ActiveVehicle;

/**
 * Fired before ownership is written when a player would claim a previously
 * unowned ({@code none}) vehicle via interact or admin takeover. Cancel to
 * block the claim (owner stays {@code none}).
 */
public final class VehicleOwnerClaimedEvent extends PlayerEvent implements Cancellable {
	private static final HandlerList HANDLERS = new HandlerList();

	private final ActiveVehicle vehicle;
	private final String previousOwner;
	private final String newOwner;
	private boolean cancelled;

	public VehicleOwnerClaimedEvent(
			Player player,
			ActiveVehicle vehicle,
			String previousOwner,
			String newOwner) {
		super(player);
		this.vehicle = vehicle;
		this.previousOwner = previousOwner;
		this.newOwner = newOwner;
	}

	public ActiveVehicle getVehicle() {
		return vehicle;
	}

	public String getPreviousOwner() {
		return previousOwner;
	}

	public String getNewOwner() {
		return newOwner;
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
