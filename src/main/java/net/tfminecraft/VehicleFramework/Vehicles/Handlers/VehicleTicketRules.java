package net.tfminecraft.VehicleFramework.Vehicles.Handlers;

import net.tfminecraft.VehicleFramework.Data.OwnerData;
import net.tfminecraft.VehicleFramework.Enums.SeatType;

public final class VehicleTicketRules {
	private VehicleTicketRules() {
	}

	public static boolean ownerOrWhitelisted(OwnerData data, String playerName) {
		if (data == null || playerName == null) {
			return false;
		}
		String playerEntry = "player_" + playerName;
		if (data.getOwner() != null && data.getOwner().equalsIgnoreCase(playerEntry)) {
			return true;
		}
		if (data.getWhiteList() == null) {
			return false;
		}
		for (String entry : data.getWhiteList()) {
			if (entry == null) {
				continue;
			}
			if (entry.equalsIgnoreCase(playerEntry) || entry.equalsIgnoreCase(playerName)) {
				return true;
			}
		}
		return false;
	}

	public static boolean mayOpenSeatMenu(OwnerData access, String playerName, boolean hasMatchingTicket) {
		if (access == null) {
			return true;
		}
		if (!access.isWhiteListed()
				|| access.getOwner() == null
				|| access.getOwner().equalsIgnoreCase("none")) {
			return true;
		}
		if (ownerOrWhitelisted(access, playerName)) {
			return true;
		}
		return access.isTicketsEnabled() && hasMatchingTicket;
	}

	public static boolean mayEnter(boolean ticketsEnabled, SeatType seat, boolean ownerOrWhitelist, boolean hasMatchingTicket) {
		if (!ticketsEnabled) {
			return true;
		}
		if (ownerOrWhitelist) {
			return true;
		}
		if (seat == null || seat != SeatType.PASSENGER) {
			return true;
		}
		return hasMatchingTicket;
	}
}
