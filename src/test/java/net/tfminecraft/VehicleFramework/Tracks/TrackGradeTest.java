package net.tfminecraft.VehicleFramework.Tracks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class TrackGradeTest {

	@Test
	void sameY_staysFlat() throws TrackLayException {
		List<double[]> points = line(0, 64, 0, 0, 64, 20);
		TrackGrade.apply(points, 64, 64, 10, 15);
		for (double[] p : points) {
			assertEquals(64, p[1], 1e-6);
		}
	}

	@Test
	void riseFitsDesired_flatThenRamp() throws TrackLayException {
		List<double[]> points = line(0, 64, 0, 0, 66, 20);
		TrackGrade.apply(points, 64, 66, 10, 15);
		assertEquals(64, points.get(0)[1], 1e-6);
		assertEquals(66, points.get(points.size() - 1)[1], 1e-6);
		double ramp = 2.0 / Math.tan(Math.toRadians(10));
		double flatEnd = 20 - ramp;
		int flatCount = 0;
		for (int i = 0; i < points.size() - 1; i++) {
			if (points.get(i)[2] + 1e-6 < flatEnd) {
				assertEquals(64, points.get(i)[1], 0.05);
				flatCount++;
			}
		}
		assertTrue(flatCount >= 4);
		double lastDy = points.get(points.size() - 1)[1] - points.get(points.size() - 2)[1];
		assertTrue(lastDy > 0.05);
	}

	@Test
	void fallFitsDesired_rampThenFlat() throws TrackLayException {
		List<double[]> points = line(0, 66, 0, 0, 64, 20);
		TrackGrade.apply(points, 66, 64, 10, 15);
		assertEquals(66, points.get(0)[1], 1e-6);
		assertEquals(64, points.get(points.size() - 1)[1], 1e-6);
		double ramp = 2.0 / Math.tan(Math.toRadians(10));
		assertTrue(points.get(1)[1] < 66 - 0.05);
		int flatCount = 0;
		for (int i = 0; i < points.size(); i++) {
			if (points.get(i)[2] + 1e-6 > ramp) {
				assertEquals(64, points.get(i)[1], 0.08);
				flatCount++;
			}
		}
		assertTrue(flatCount >= 4);
	}

	@Test
	void riseNeedsMax_succeeds() throws TrackLayException {
		List<double[]> points = line(0, 64, 0, 0, 66.5, 12);
		TrackGrade.apply(points, 64, 66.5, 10, 15);
		assertEquals(66.5, points.get(points.size() - 1)[1], 1e-6);
		assertEquals(64, points.get(0)[1], 1e-6);
	}

	@Test
	void requiredStep_pullsRampEarlier() throws TrackLayException {
		List<double[]> points = line(0, 64, 0, 0, 65, 20);
		TrackGrade.apply(points, 64, 65, 10, 20);
		double[] minY = new double[points.size()];
		for (int i = 0; i < points.size(); i++) {
			minY[i] = points.get(i)[1];
		}
		minY[10] = 65;
		assertEquals(64, points.get(10)[1], 0.2);
		TrackGrade.applyRequiredHeights(points, minY, 20);
		assertEquals(64, points.get(0)[1], 1e-6);
		assertEquals(65, points.get(10)[1], 0.15);
		assertEquals(65, points.get(points.size() - 1)[1], 1e-6);
	}

	@Test
	void riseTooSteep_failsWithRemaining() {
		List<double[]> points = line(0, 64, 0, 0, 72, 12);
		TrackLayException ex = assertThrows(TrackLayException.class,
				() -> TrackGrade.apply(points, 64, 72, 10, 15));
		assertTrue(ex.getMessage().contains("15.0"));
		assertTrue(ex.getMessage().contains("more blocks"));
	}

	private static List<double[]> line(
			double ax, double ay, double az,
			double bx, double by, double bz) {
		List<double[]> points = new ArrayList<>();
		int n = (int) Math.round(Math.hypot(bx - ax, bz - az));
		for (int i = 0; i <= n; i++) {
			double t = i / (double) n;
			points.add(new double[] {
					ax + (bx - ax) * t,
					ay + (by - ay) * t,
					az + (bz - az) * t
			});
		}
		return points;
	}
}
