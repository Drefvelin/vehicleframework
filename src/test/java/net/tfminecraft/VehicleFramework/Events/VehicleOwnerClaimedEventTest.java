package net.tfminecraft.VehicleFramework.Events;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.bukkit.event.Cancellable;
import org.junit.jupiter.api.Test;

class VehicleOwnerClaimedEventTest {
	@Test
	void handlerList_isShared() {
		assertNotNull(VehicleOwnerClaimedEvent.getHandlerList());
		assertSame(VehicleOwnerClaimedEvent.getHandlerList(), VehicleOwnerClaimedEvent.getHandlerList());
	}

	@Test
	void implementsCancellable_defaultNotCancelled() {
		assertTrue(Cancellable.class.isAssignableFrom(VehicleOwnerClaimedEvent.class));
		VehicleOwnerClaimedEvent event = new VehicleOwnerClaimedEvent(null, null, "none", "player_Test");
		assertFalse(event.isCancelled());
		event.setCancelled(true);
		assertTrue(event.isCancelled());
	}
}
