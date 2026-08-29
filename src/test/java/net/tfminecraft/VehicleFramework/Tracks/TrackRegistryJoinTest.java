package net.tfminecraft.VehicleFramework.Tracks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class TrackRegistryJoinTest {

	@Test
	void appendKeepsId(@TempDir java.nio.file.Path dir) throws Exception {
		TrackRegistry registry = new TrackRegistry(dir.toFile());
		TrackSpline first = registry.lay("world", 0, 64, 0, 0, 64, 10).spline();
		UUID id = first.getId();
		TrackSpline second = registry.lay("world", 0, 64, 10, 0, 64, 20).spline();
		assertEquals(id, second.getId());
		assertTrue(second.length() > first.length());
		assertEquals(1, registry.inWorld("world").size());
	}

	@Test
	void prependKeepsId(@TempDir java.nio.file.Path dir) throws Exception {
		TrackRegistry registry = new TrackRegistry(dir.toFile());
		TrackSpline first = registry.lay("world", 0, 64, 0, 0, 64, 10).spline();
		UUID id = first.getId();
		TrackSpline second = registry.lay("world", 0, 64, 0, 0, 64, -10).spline();
		assertEquals(id, second.getId());
		assertEquals(0, second.first().x, 0.2);
		assertTrue(second.first().z < -8);
	}

	@Test
	void interiorDigSplits(@TempDir java.nio.file.Path dir) throws Exception {
		TrackRegistry registry = new TrackRegistry(dir.toFile());
		TrackSpline spline = registry.lay("world", 0, 64, 0, 0, 64, 20).spline();
		UUID id = spline.getId();
		int mid = spline.getSamples().size() / 2;
		DigResult result = registry.digAt(spline, mid);
		assertEquals(DigResult.Kind.SPLIT, result.kind);
		assertEquals(id, result.kept.getId());
		assertNotEquals(id, result.tail.getId());
		assertEquals(2, registry.inWorld("world").size());
	}

	@Test
	void connectEndToStart_merges(@TempDir java.nio.file.Path dir) throws Exception {
		TrackRegistry registry = new TrackRegistry(dir.toFile());
		TrackSpline a = registry.lay("world", 0, 64, 0, 0, 64, 10).spline();
		TrackSpline b = registry.lay("world", 0, 64, 30, 0, 64, 40).spline();
		UUID keep = a.getId();
		TrackLayResult linkedResult = registry.lay("world", 0, 64, 10, 0, 64, 30);
		TrackSpline linked = linkedResult.spline();
		assertEquals(TrackLayResult.Kind.CONNECT, linkedResult.kind);
		assertEquals(keep, linked.getId());
		assertEquals(1, registry.inWorld("world").size());
		assertTrue(linked.last().z > 38);
	}

	@Test
	void replace_promotesLoopWhenEndsMeet(@TempDir java.nio.file.Path dir) {
		TrackRegistry registry = new TrackRegistry(dir.toFile());
		UUID id = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
		TrackSpline open = TrackSpline.fromPoints(
				id, "world", false,
				List.of(
						new double[] {0, 64, 0},
						new double[] {8, 64, 0},
						new double[] {8, 64, 8},
						new double[] {0.4, 64, 0}));
		TrackSpline stored = registry.replace(open);
		assertTrue(stored.isLoop());
		assertTrue(registry.get(id).orElseThrow().isLoop());
	}

	@Test
	void extendMerge_setsLoopWhenEndsMeet() {
		List<double[]> merged = List.of(
				new double[] {0, 64, 0},
				new double[] {8, 64, 0},
				new double[] {8, 64, 8},
				new double[] {0.4, 64, 0});
		assertTrue(TrackSpline.shouldLoop(merged, 1.5));
		TrackSpline next = TrackSpline.fromPoints(
				UUID.randomUUID(), "world", TrackSpline.shouldLoop(merged, 1.5), merged);
		assertTrue(next.isLoop());
	}
}
