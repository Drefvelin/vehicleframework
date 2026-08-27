package net.tfminecraft.VehicleFramework.Util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.bukkit.util.Vector;
import org.junit.jupiter.api.Test;

class ImpactVfxTest {

	private static final float TOLERANCE = 0.001f;

	@Test
	void surfaceOffset_usesFaceNormalOutward() {
		Vector incoming = new Vector(0, 0, 1);
		Vector face = new Vector(0, 0, -1);
		Vector offset = ImpactVfx.surfaceOffset(incoming, face);
		assertEquals(0.0, offset.getX(), TOLERANCE);
		assertEquals(0.0, offset.getY(), TOLERANCE);
		assertEquals(-0.06, offset.getZ(), TOLERANCE);
	}

	@Test
	void surfaceOffset_withoutFace_pullsBackAlongIncoming() {
		Vector incoming = new Vector(1, 0, 0);
		Vector offset = ImpactVfx.surfaceOffset(incoming, null);
		assertEquals(-0.06, offset.getX(), TOLERANCE);
	}
}
