package net.tfminecraft.VehicleFramework.Tracks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class TrackVisualBakeTest {

	@Test
	void sixCollinearSamples_twoLarge() {
		TrackSpline spline = TrackSpline.fromPoints(
				UUID.randomUUID(), "world", false,
				List.of(
						new double[] {0, 64, 0},
						new double[] {0, 64, 1},
						new double[] {0, 64, 2},
						new double[] {0, 64, 3},
						new double[] {0, 64, 4},
						new double[] {0, 64, 5}));
		List<TrackVisual> visuals = TrackVisualBake.bake(spline);
		assertEquals(2, visuals.size());
		assertEquals(TrackVisual.Type.LARGE, visuals.get(0).type);
		assertEquals(TrackVisual.Type.LARGE, visuals.get(1).type);
		assertEquals(0, visuals.get(0).startIndex);
		assertEquals(3, visuals.get(1).startIndex);
	}

	@Test
	void rightAngleCorner_staysSmallAtTurn() {
		TrackSpline spline = TrackSpline.fromPoints(
				UUID.randomUUID(), "world", false,
				List.of(
						new double[] {0, 64, 0},
						new double[] {0, 64, 1},
						new double[] {1, 64, 1}));
		List<TrackVisual> visuals = TrackVisualBake.bake(spline);
		assertTrue(visuals.size() >= 2);
		boolean sawSmall = false;
		for (TrackVisual visual : visuals) {
			if (visual.type == TrackVisual.Type.SMALL) {
				sawSmall = true;
			}
		}
		assertTrue(sawSmall);
		assertTrue(visuals.stream().noneMatch(v -> v.type == TrackVisual.Type.LARGE));
	}

	@Test
	void brokenMiddleEdge_splitsRun() {
		TrackSpline spline = TrackSpline.fromPoints(
				UUID.randomUUID(), "world", false,
				List.of(
						new double[] {0, 64, 0},
						new double[] {0, 64, 1},
						new double[] {0, 64, 2},
						new double[] {0, 64, 3},
						new double[] {0, 64, 4}));
		spline = spline.withSegment(2, new TrackSegment(2, true, 1.0));
		List<TrackVisual> visuals = TrackVisualBake.bake(spline);
		assertTrue(visuals.stream().noneMatch(v -> v.type == TrackVisual.Type.LARGE));
		assertTrue(visuals.stream().noneMatch(v -> v.startIndex == 2));
	}

	@Test
	void pitchJump_isNotMerged() {
		List<TrackSample> samples = List.of(
				new TrackSample(0, 64, 0, 0f, 0f, 0),
				new TrackSample(0, 65, 1, 0f, 45f, 1),
				new TrackSample(0, 66, 2, 0f, 45f, 2));
		TrackSpline spline = new TrackSpline(UUID.randomUUID(), "world", false, samples, null);
		List<TrackVisual> visuals = TrackVisualBake.bake(spline);
		assertEquals(TrackVisual.Type.SMALL, visuals.get(0).type);
		assertTrue(visuals.get(0).length == 1);
	}

	@Test
	void splineVisuals_cachesBake() {
		TrackSpline spline = TrackSpline.fromPoints(
				UUID.randomUUID(), "world", false,
				List.of(new double[] {0, 64, 0}, new double[] {0, 64, 5}));
		List<TrackVisual> first = spline.visuals();
		org.junit.jupiter.api.Assertions.assertSame(first, spline.visuals());
		spline.invalidateVisuals();
		assertEquals(first.size(), spline.visuals().size());
	}

	@Test
	void collinearPrefix_growsMediumThenLarge() {
		List<TrackVisual> two = TrackVisualBake.bake(TrackSpline.fromPoints(
				UUID.randomUUID(), "world", false,
				List.of(new double[] {0, 64, 0}, new double[] {0, 64, 1})));
		assertEquals(1, two.size());
		assertEquals(TrackVisual.Type.MEDIUM, two.get(0).type);

		List<TrackVisual> three = TrackVisualBake.bake(TrackSpline.fromPoints(
				UUID.randomUUID(), "world", false,
				List.of(
						new double[] {0, 64, 0},
						new double[] {0, 64, 1},
						new double[] {0, 64, 2})));
		assertEquals(1, three.size());
		assertEquals(TrackVisual.Type.LARGE, three.get(0).type);
	}

	@Test
	void growingCollinear_replacesWholePiece() {
		List<TrackVisual> two = TrackVisualBake.bake(TrackSpline.fromPoints(
				UUID.randomUUID(), "world", false,
				List.of(new double[] {0, 64, 0}, new double[] {0, 64, 1})));
		List<TrackVisual> three = TrackVisualBake.bake(TrackSpline.fromPoints(
				UUID.randomUUID(), "world", false,
				List.of(
						new double[] {0, 64, 0},
						new double[] {0, 64, 1},
						new double[] {0, 64, 2})));
		assertEquals(0, TrackVisualDiff.firstChange(two, three));
	}

	@Test
	void keepPrefix_unchangedWhenStrokeGrows() {
		List<double[]> keep = List.of(
				new double[] {0, 64, 0},
				new double[] {0, 64, 1},
				new double[] {0, 64, 2},
				new double[] {0, 64, 3},
				new double[] {0, 64, 4},
				new double[] {0, 64, 5});
		List<TrackVisual> before = TrackVisualBake.bake(
				TrackSpline.fromPoints(UUID.randomUUID(), "world", false, keep));
		List<double[]> grown = new java.util.ArrayList<>(keep);
		grown.add(new double[] {0, 64, 6});
		List<TrackVisual> after = TrackVisualBake.bake(
				TrackSpline.fromPoints(UUID.randomUUID(), "world", false, grown));
		assertTrue(TrackVisualDiff.same(before.get(0), after.get(0)));
		assertEquals(1, TrackVisualDiff.firstChange(before, after));
	}
}
