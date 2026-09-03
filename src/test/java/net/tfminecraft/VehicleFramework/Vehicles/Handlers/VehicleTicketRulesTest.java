package net.tfminecraft.VehicleFramework.Vehicles.Handlers;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import net.tfminecraft.VehicleFramework.Data.OwnerData;
import net.tfminecraft.VehicleFramework.Enums.SeatType;

class VehicleTicketRulesTest {

	@Test
	void ticketsOff_allowsPassenger() {
		assertTrue(VehicleTicketRules.mayEnter(false, SeatType.PASSENGER, false, false));
	}

	@Test
	void captain_allowedWithoutTicket() {
		assertTrue(VehicleTicketRules.mayEnter(true, SeatType.CAPTAIN, false, false));
		assertTrue(VehicleTicketRules.mayEnter(true, SeatType.GUNNER, false, false));
		assertTrue(VehicleTicketRules.mayEnter(true, SeatType.MECHANIC, false, false));
	}

	@Test
	void ownerOrWhitelist_skipsPassenger() {
		assertTrue(VehicleTicketRules.mayEnter(true, SeatType.PASSENGER, true, false));
	}

	@Test
	void passenger_noTicket_denied() {
		assertFalse(VehicleTicketRules.mayEnter(true, SeatType.PASSENGER, false, false));
	}

	@Test
	void passenger_matchingTicket_allowed() {
		assertTrue(VehicleTicketRules.mayEnter(true, SeatType.PASSENGER, false, true));
	}

	@Test
	void uuidMismatch_isNoMatch() {
		assertFalse(VehicleTicketRules.mayEnter(true, SeatType.PASSENGER, false, false));
	}

	@Test
	void mayOpenSeatMenu_whitelistOff_allowsAnyone() {
		OwnerData access = gatedAccess();
		access.setWhiteListed(false);
		assertTrue(VehicleTicketRules.mayOpenSeatMenu(access, "stranger", false));
	}

	@Test
	void mayOpenSeatMenu_whitelistOn_notListed_noTicket_denied() {
		OwnerData access = gatedAccess();
		assertFalse(VehicleTicketRules.mayOpenSeatMenu(access, "stranger", false));
	}

	@Test
	void mayOpenSeatMenu_whitelistOn_notListed_matchingTicket_allowed() {
		OwnerData access = gatedAccess();
		access.setTicketsEnabled(true);
		assertTrue(VehicleTicketRules.mayOpenSeatMenu(access, "stranger", true));
	}

	@Test
	void mayOpenSeatMenu_whitelistOn_notListed_ticketButTicketsDisabled_denied() {
		OwnerData access = gatedAccess();
		access.setTicketsEnabled(false);
		assertFalse(VehicleTicketRules.mayOpenSeatMenu(access, "stranger", true));
	}

	@Test
	void mayOpenSeatMenu_whitelistOn_listedPlayer_allowedWithoutTicket() {
		OwnerData access = gatedAccess();
		access.addToWhiteList("player_friend");
		assertTrue(VehicleTicketRules.mayOpenSeatMenu(access, "friend", false));
	}

	private static OwnerData gatedAccess() {
		OwnerData access = new OwnerData();
		access.setOwner("player_owner");
		access.setWhiteListed(true);
		return access;
	}
}
