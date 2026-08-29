package net.tfminecraft.VehicleFramework.Tracks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class TrackRegistryJunctionTest {

	@Test
	void spacing_allowsSixteenApart(@TempDir java.nio.file.Path dir) throws Exception {
		TrackRegistry registry = new TrackRegistry(dir.toFile());
		TrackSpline stem = registry.lay("world", 0, 64, 0, 0, 64, 40).spline();
		TrackJunction first = registry.putJunction(completed(stem, 4));
		TrackJunction second = registry.putJunction(completed(stem, 4 + 16));
		assertEquals(2, registry.junctionsOn(stem.getId()).size());
		assertEquals(first.id, registry.getJunction(first.id).orElseThrow().id);
		assertEquals(20, second.s, 1e-6);
	}

	@Test
	void spacing_rejectsCloserThanSixteen(@TempDir java.nio.file.Path dir) throws Exception {
		TrackRegistry registry = new TrackRegistry(dir.toFile());
		TrackSpline stem = registry.lay("world", 0, 64, 0, 0, 64, 40).spline();
		registry.putJunction(completed(stem, 4));
		assertThrows(TrackLayException.class, () -> registry.putJunction(completed(stem, 4 + 15.9)));
		assertEquals(1, registry.junctionsOn(stem.getId()).size());
	}

	@Test
	void spacing_loopWraps(@TempDir java.nio.file.Path dir) throws Exception {
		TrackRegistry registry = new TrackRegistry(dir.toFile());
		TrackSpline stem = registry.replace(TrackSpline.fromPoints(
				UUID.randomUUID(), "world", true,
				List.of(
						new double[] {0, 64, 0},
						new double[] {20, 64, 0},
						new double[] {20, 64, 20},
						new double[] {0, 64, 20})));
		assertTrue(stem.isLoop());
		registry.putJunction(completed(stem, 1));
		assertThrows(TrackLayException.class, () -> registry.putJunction(completed(stem, stem.length() - 1)));
	}

	@Test
	void attachBranch_secondDifferentIdFails(@TempDir java.nio.file.Path dir) throws Exception {
		TrackRegistry registry = new TrackRegistry(dir.toFile());
		TrackSpline stem = registry.lay("world", 0, 64, 0, 0, 64, 40).spline();
		TrackJunction placed = registry.putJunction(node(stem, 8));
		UUID firstBranch = UUID.randomUUID();
		UUID otherBranch = UUID.randomUUID();
		registry.attachBranch(placed.id, firstBranch);
		assertThrows(TrackLayException.class, () -> registry.attachBranch(placed.id, otherBranch));
		assertEquals(firstBranch, registry.getJunction(placed.id).orElseThrow().branchSplineId().orElseThrow());
	}

	@Test
	void loadFromDisk_restoresThenStemDeleteRemovesFile(@TempDir java.nio.file.Path dir) throws Exception {
		TrackRegistry registry = new TrackRegistry(dir.toFile());
		TrackSpline stem = registry.lay("world", 0, 64, 0, 0, 64, 40).spline();
		TrackJunction placed = registry.putJunction(completed(stem, 8));
		TrackRegistry reloaded = new TrackRegistry(dir.toFile());
		reloaded.loadFromDisk();
		assertTrue(reloaded.getJunction(placed.id).isPresent());
		assertEquals(placed.s, reloaded.getJunction(placed.id).orElseThrow().s, 1e-6);
		assertTrue(reloaded.delete(stem.getId()));
		assertFalse(reloaded.getJunction(placed.id).isPresent());
		assertEquals(0, reloaded.junctionsOn(stem.getId()).size());
		TrackRegistry afterDelete = new TrackRegistry(dir.toFile());
		afterDelete.loadFromDisk();
		assertFalse(afterDelete.getJunction(placed.id).isPresent());
	}

	@Test
	void loadFromDisk_dropsJunctionWithoutBranch(@TempDir java.nio.file.Path dir) throws Exception {
		TrackRegistry registry = new TrackRegistry(dir.toFile());
		TrackSpline stem = registry.lay("world", 0, 64, 0, 0, 64, 40).spline();
		TrackJunction placed = registry.putJunction(node(stem, 8));
		TrackRegistry reloaded = new TrackRegistry(dir.toFile());
		reloaded.loadFromDisk();
		assertFalse(reloaded.getJunction(placed.id).isPresent());
		assertEquals(0, reloaded.junctionsOn(stem.getId()).size());
	}

	@Test
	void deleteBranchSpline_removesJunction(@TempDir java.nio.file.Path dir) throws Exception {
		TrackRegistry registry = new TrackRegistry(dir.toFile());
		TrackSpline stem = registry.lay("world", 0, 64, 0, 0, 64, 40).spline();
		TrackSpline branch = registry.lay("world", 20, 64, 0, 20, 64, 20).spline();
		TrackJunction placed = registry.putJunction(node(stem, 8));
		registry.attachBranch(placed.id, branch.getId());
		assertTrue(registry.delete(branch.getId()));
		assertFalse(registry.getJunction(placed.id).isPresent());
	}

	@Test
	void layBranch_createsSplineAndRefusesSecond(@TempDir java.nio.file.Path dir) throws Exception {
		TrackRegistry registry = new TrackRegistry(dir.toFile());
		TrackSpline stem = registry.lay("world", 0, 64, 0, 0, 64, 40).spline();
		TrackJunction placed = registry.putJunction(node(stem, 10));
		TrackSpline branch = registry.layBranch(placed.id, "world", null, 2, 64, 28);
		assertTrue(branch.length() >= 8);
		assertEquals(branch.getId(), registry.getJunction(placed.id).orElseThrow().branchSplineId().orElseThrow());
		assertThrows(TrackLayException.class, () -> registry.layBranch(placed.id, "world", null, 4, 64, 24));
		assertEquals(placed.id, registry.junctionByBranch(branch.getId()).orElseThrow().id);
	}

	@Test
	void layBranch_placesEvenWhenCrossingStem(@TempDir java.nio.file.Path dir) throws Exception {
		TrackRegistry registry = new TrackRegistry(dir.toFile());
		TrackSpline stem = registry.lay("world", 0, 64, 0, 0, 64, 40).spline();
		TrackJunction placed = registry.putJunction(node(stem, 10));
		TrackSpline branch = registry.layBranch(placed.id, "world", null, 0, 64, 28);
		assertTrue(branch.length() >= 8);
		assertEquals(branch.getId(), registry.getJunction(placed.id).orElseThrow().branchSplineId().orElseThrow());
	}

	@Test
	void layBranch_refusesLongerThanMax(@TempDir java.nio.file.Path dir) throws Exception {
		TrackRegistry registry = new TrackRegistry(dir.toFile());
		TrackSpline stem = registry.lay("world", 0, 64, 0, 0, 64, 50).spline();
		TrackJunction placed = registry.putJunction(node(stem, 10));
		TrackLayException thrown = assertThrows(
				TrackLayException.class,
				() -> registry.layBranch(placed.id, "world", null, 0, 64, 43));
		assertTrue(thrown.getMessage().contains("32"));
		assertTrue(registry.getJunction(placed.id).orElseThrow().branchSplineId().isEmpty());
	}

	@Test
	void layBranch_failDoesNotCreateJunction(@TempDir java.nio.file.Path dir) throws Exception {
		TrackRegistry registry = new TrackRegistry(dir.toFile());
		TrackSpline stem = registry.lay("world", 0, 64, 0, 0, 64, 50).spline();
		assertThrows(TrackLayException.class, () -> registry.layBranch(
				stem.getId(), 10, 1, "world", null, 0, 64, 43));
		assertEquals(0, registry.junctionsOn(stem.getId()).size());
	}

	@Test
	void prepend_shiftsJunctionS(@TempDir java.nio.file.Path dir) throws Exception {
		TrackRegistry registry = new TrackRegistry(dir.toFile());
		TrackSpline stem = registry.lay("world", 0, 64, 0, 0, 64, 40).spline();
		TrackJunction placed = registry.putJunction(node(stem, 20));
		double before = placed.s;
		registry.lay("world", 0, 64, 0, 0, 64, -12);
		TrackJunction after = registry.getJunction(placed.id).orElseThrow();
		assertEquals(stem.getId(), after.stemSplineId);
		assertTrue(after.s > before + 8, "prepend should push junction s, was " + before + " now " + after.s);
	}

	@Test
	void split_movesTailJunctionToNewSpline(@TempDir java.nio.file.Path dir) throws Exception {
		TrackRegistry registry = new TrackRegistry(dir.toFile());
		TrackSpline stem = registry.lay("world", 0, 64, 0, 0, 64, 40).spline();
		TrackJunction placed = registry.putJunction(node(stem, 32));
		int mid = stem.getSamples().size() / 2;
		DigResult result = registry.digAt(stem, mid);
		assertEquals(DigResult.Kind.SPLIT, result.kind);
		TrackJunction after = registry.getJunction(placed.id).orElseThrow();
		assertEquals(result.tail.getId(), after.stemSplineId);
		assertTrue(after.s < 20);
	}

	@Test
	void setThrown_savesAndReloads(@TempDir java.nio.file.Path dir) throws Exception {
		TrackRegistry registry = new TrackRegistry(dir.toFile());
		TrackSpline stem = registry.lay("world", 0, 64, 0, 0, 64, 40).spline();
		TrackJunction placed = registry.putJunction(completed(stem, 8));
		assertFalse(placed.thrown);
		assertTrue(registry.setThrown(placed.id, true));
		assertTrue(registry.getJunction(placed.id).orElseThrow().thrown);
		TrackRegistry reloaded = new TrackRegistry(dir.toFile());
		reloaded.loadFromDisk();
		assertTrue(reloaded.getJunction(placed.id).orElseThrow().thrown);
	}

	private static TrackJunction completed(TrackSpline stem, double s) {
		return node(stem, s, UUID.randomUUID());
	}

	private static TrackJunction node(TrackSpline stem, double s) {
		return node(stem, s, null);
	}

	private static TrackJunction node(TrackSpline stem, double s, UUID branchId) {
		return new TrackJunction(
				UUID.randomUUID(),
				stem.getId(),
				s,
				1,
				TrackJunction.Side.RIGHT,
				branchId);
	}
}
