package net.tfminecraft.VehicleFramework.Tracks;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

class TrackPiecesTest {

	@Test
	void cost_isSamplesMinusOrigin() {
		assertEquals(0, TrackPieces.cost(null));
		assertEquals(0, TrackPieces.cost(List.of(new double[] {0, 64, 0})));
		assertEquals(4, TrackPieces.cost(List.of(
				new double[] {0, 64, 0},
				new double[] {0, 64, 1},
				new double[] {0, 64, 2},
				new double[] {0, 64, 3},
				new double[] {0, 64, 4})));
		assertEquals(3, TrackPieces.cost(List.of(
				new double[] {0, 64, 10},
				new double[] {0, 64, 11},
				new double[] {0, 64, 12},
				new double[] {0, 64, 13})));
	}

	@Test
	void persistPoints_newPrefix() {
		List<double[]> stroke = List.of(
				new double[] {0, 64, 0},
				new double[] {0, 64, 1},
				new double[] {0, 64, 2},
				new double[] {0, 64, 3});
		List<double[]> paid = TrackPieces.persistPoints(List.of(), stroke, true, 2);
		assertEquals(3, paid.size());
		assertEquals(2, paid.get(2)[2], 1e-9);
	}

	@Test
	void persistPoints_appendKeepsOld() {
		List<double[]> keep = List.of(
				new double[] {0, 64, 0},
				new double[] {0, 64, 1});
		List<double[]> stroke = List.of(
				new double[] {0, 64, 1},
				new double[] {0, 64, 2},
				new double[] {0, 64, 3});
		List<double[]> out = TrackPieces.persistPoints(keep, stroke, true, 1);
		assertEquals(3, out.size());
		assertEquals(0, out.get(0)[2], 1e-9);
		assertEquals(2, out.get(2)[2], 1e-9);
	}
}
