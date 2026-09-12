package net.tfminecraft.VehicleFramework.Tracks;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class TrackClearanceOverlapTest {

	@Test
	void frogWindow_allowsOverlapAtJunction(@TempDir java.nio.file.Path dir) throws Exception {
		TrackRegistry registry = new TrackRegistry(dir.toFile());
		TrackSpline stem = registry.lay("world", 0, 64, 0, 0, 64, 40).spline();
		double s = 20;
		TrackPose pose = stem.sampleAt(s);
		List<double[]> frog = List.of(
				new double[] {pose.x, pose.y, pose.z},
				new double[] {pose.x + 0.4, pose.y, pose.z + 0.4});
		assertThrows(TrackLayException.class, () -> TrackClearance.checkOverlap(
				"world", frog, registry, Set.of(), null));
		assertDoesNotThrow(() -> TrackClearance.checkOverlap(
				"world", frog, registry, Set.of(),
				new TrackClearance.FrogIgnore(stem.getId(), s)));
	}

	@Test
	void frogWindow_stillRefusesFarRecross(@TempDir java.nio.file.Path dir) throws Exception {
		TrackRegistry registry = new TrackRegistry(dir.toFile());
		TrackSpline stem = registry.lay("world", 0, 64, 0, 0, 64, 40).spline();
		TrackPose far = stem.sampleAt(35);
		List<double[]> recross = List.of(
				new double[] {far.x, far.y, far.z},
				new double[] {far.x + 0.2, far.y, far.z + 0.2});
		assertThrows(TrackLayException.class, () -> TrackClearance.checkOverlap(
				"world", recross, registry, Set.of(),
				new TrackClearance.FrogIgnore(stem.getId(), 8)));
	}

	@Test
	void ignoreSplineIds_skipsWholeSpline(@TempDir java.nio.file.Path dir) throws Exception {
		TrackRegistry registry = new TrackRegistry(dir.toFile());
		TrackSpline stem = registry.lay("world", 0, 64, 0, 0, 64, 40).spline();
		TrackPose pose = stem.sampleAt(20);
		List<double[]> frog = List.of(
				new double[] {pose.x, pose.y, pose.z},
				new double[] {pose.x + 0.4, pose.y, pose.z});
		assertDoesNotThrow(() -> TrackClearance.checkOverlap(
				"world", frog, registry, Set.of(stem.getId()), null));
	}
}
