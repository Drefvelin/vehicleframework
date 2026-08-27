package net.tfminecraft.VehicleFramework.Events;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

class VehicleRepairStartEventTest {
	@Test
	void handlerList_isShared() {
		assertNotNull(VehicleRepairStartEvent.getHandlerList());
		assertSame(VehicleRepairStartEvent.getHandlerList(), VehicleRepairStartEvent.getHandlerList());
	}
}
