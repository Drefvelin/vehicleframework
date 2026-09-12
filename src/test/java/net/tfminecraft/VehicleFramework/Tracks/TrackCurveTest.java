package net.tfminecraft.VehicleFramework.Tracks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class TrackCurveTest {

	@Test
	void tooShort_failsWithRemaining() {
		TrackLayException ex = assertThrows(TrackLayException.class, () -> TrackCurve.between(
				0, 64, 0, 0, 64, 3, 8, 10, 15, 1.0));
		assertTrue(ex.getMessage().contains("8.0"));
		assertTrue(ex.getMessage().contains("3.0"));
	}

	@Test
	void twentyDegreeTurn_failsFifteenCap() {
		double[] end = endOnArc(0, 0, 0f, 20, 12);
		assertThrows(TrackLayException.class, () -> TrackCurve.lay(
				0, 64, 0, 0f, end[0], 64, end[1], 8, 15, 1.0));
	}

	@Test
	void shallowTurn_yawIncreases() throws TrackLayException {
		double[] end = endOnArc(0, 0, 0f, 10, 12);
		List<double[]> points = TrackCurve.lay(
				0, 64, 0, 0f, end[0], 64, end[1], 8, 15, 1.0);
		assertTrue(points.size() >= 8);
		float prev = yaw(points.get(0), points.get(1));
		for (int i = 1; i < points.size() - 1; i++) {
			float next = yaw(points.get(i), points.get(i + 1));
			assertTrue(next >= prev - 0.5f, "yaw should not jump back");
			prev = next;
		}
	}

	@Test
	void straightPlusZ_southYaw() throws TrackLayException {
		List<double[]> points = TrackCurve.between(0, 64, 0, 0, 64, 10, 8, 10, 15, 1.0);
		assertEquals(11, points.size());
		assertEquals(0, points.get(5)[0], 1e-6);
		assertEquals(5, points.get(5)[2], 1e-6);
		assertEquals(64, points.get(5)[1], 1e-6);
	}

	@Test
	void straightRise_staysFlatThenClimbs() throws TrackLayException {
		List<double[]> points = TrackCurve.between(0, 64, 0, 0, 66, 20, 8, 10, 15, 1.0);
		assertEquals(64, points.get(0)[1], 1e-6);
		assertEquals(66, points.get(points.size() - 1)[1], 1e-6);
		assertEquals(64, points.get(4)[1], 0.15);
	}

	@Test
	void between_eastChord_doesNotUseFacing() throws TrackLayException {
		List<double[]> points = TrackCurve.between(0, 161, 0, 61, 162, -2, 8, 15, 20, 1.0);
		assertEquals(61, points.get(points.size() - 1)[0], 1e-6);
		assertEquals(-2, points.get(points.size() - 1)[2], 1e-6);
	}

	private static double[] endOnArc(double ax, double az, float startYaw, double turnDeg, double chord) {
		double yawRad = Math.toRadians(startYaw);
		double tx = -Math.sin(yawRad);
		double tz = Math.cos(yawRad);
		double nx = -tz;
		double nz = tx;
		double phi = Math.toRadians(turnDeg);
		double radius = chord / (2.0 * Math.sin(Math.abs(phi) / 2.0));
		double cx = ax + nx * radius;
		double cz = az + nz * radius;
		double pox = ax - cx;
		double poz = az - cz;
		double cos = Math.cos(phi);
		double sin = Math.sin(phi);
		double rx = pox * cos - poz * sin;
		double rz = pox * sin + poz * cos;
		return new double[] {cx + rx, cz + rz};
	}

	private static float yaw(double[] a, double[] b) {
		return (float) Math.toDegrees(Math.atan2(-(b[0] - a[0]), b[2] - a[2]));
	}
}
