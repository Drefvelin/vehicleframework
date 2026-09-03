package net.tfminecraft.VehicleFramework.Tracks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import net.tfminecraft.VehicleFramework.Cache.Cache;

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
		assertTrue(registry.getJunction(placed.id).orElseThrow().turnoutEndS > 0);
	}

	@Test
	void layBranch_setsTurnoutEndSToBranchLength(@TempDir java.nio.file.Path dir) throws Exception {
		TrackRegistry registry = new TrackRegistry(dir.toFile());
		TrackSpline stem = registry.lay("world", 0, 64, 0, 0, 64, 40).spline();
		TrackJunction placed = registry.putJunction(node(stem, 10));
		TrackSpline branch = registry.layBranch(placed.id, "world", null, 2, 64, 28);
		TrackJunction junction = registry.getJunction(placed.id).orElseThrow();
		assertTrue(junction.turnoutEndS > 0);
		assertEquals(branch.length(), junction.turnoutEndS, 1e-9);
	}

	@Test
	void layBranch_extendDoesNotChangeTurnoutEndS(@TempDir java.nio.file.Path dir) throws Exception {
		TrackRegistry registry = new TrackRegistry(dir.toFile());
		TrackSpline stem = registry.lay("world", 0, 64, 0, 0, 64, 40).spline();
		TrackJunction placed = registry.putJunction(node(stem, 10));
		TrackSpline branch = registry.layBranch(placed.id, "world", null, 2, 64, 28);
		double originalTurnout = registry.getJunction(placed.id).orElseThrow().turnoutEndS;
		TrackSample tip = branch.last();
		TrackSample prev = branch.getSamples().get(branch.getSamples().size() - 2);
		double dx = tip.x - prev.x;
		double dy = tip.y - prev.y;
		double dz = tip.z - prev.z;
		double seg = Math.sqrt(dx * dx + dy * dy + dz * dz);
		double scale = 35.0 / seg;
		TrackSpline extended = registry.lay(
				"world",
				tip.x, tip.y, tip.z,
				tip.x + dx * scale, tip.y + dy * scale, tip.z + dz * scale).spline();
		assertEquals(branch.getId(), extended.getId());
		TrackJunction junction = registry.getJunction(placed.id).orElseThrow();
		assertEquals(originalTurnout, junction.turnoutEndS, 1e-9);
		assertTrue(extended.length() > originalTurnout + 1e-9);
	}

	@Test
	void loadFromDisk_preservesTurnoutS(@TempDir java.nio.file.Path dir) throws Exception {
		TrackRegistry registry = new TrackRegistry(dir.toFile());
		TrackSpline stem = registry.lay("world", 0, 64, 0, 0, 64, 40).spline();
		TrackJunction placed = registry.putJunction(node(stem, 10));
		TrackSpline branch = registry.layBranch(placed.id, "world", null, 2, 64, 28);
		double turnoutEndS = registry.getJunction(placed.id).orElseThrow().turnoutEndS;
		assertEquals(branch.length(), turnoutEndS, 1e-9);
		TrackRegistry reloaded = new TrackRegistry(dir.toFile());
		reloaded.loadFromDisk();
		TrackJunction loaded = reloaded.getJunction(placed.id).orElseThrow();
		assertEquals(turnoutEndS, loaded.turnoutEndS, 1e-9);
		assertEquals(branch.length(), loaded.turnoutEndS, 1e-9);
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

	@Test
	void putJunction_rejectsShortStem(@TempDir java.nio.file.Path dir) throws Exception {
		TrackRegistry registry = new TrackRegistry(dir.toFile());
		TrackSpline shortStem = registry.replace(TrackSpline.fromPoints(
				UUID.randomUUID(), "world", false,
				List.of(new double[] {0, 64, 0}, new double[] {0, 64, 1})));
		TrackLayException thrown = assertThrows(
				TrackLayException.class, () -> registry.putJunction(node(shortStem, 0.5)));
		assertTrue(thrown.getMessage().contains("too short"));
	}

	@Test
	void digBranch_removesJunctionAndKeepsStem(@TempDir java.nio.file.Path dir) throws Exception {
		TrackRegistry registry = new TrackRegistry(dir.toFile());
		TrackSpline stem = registry.lay("world", 0, 64, 0, 0, 64, 40).spline();
		TrackJunction placed = registry.putJunction(node(stem, 10));
		TrackPose frog = stem.sampleAt(10);
		double yawRad = Math.toRadians(frog.yaw);
		double fx = -Math.sin(yawRad);
		double fz = Math.cos(yawRad);
		List<double[]> points = new ArrayList<>();
		for (int i = 0; i <= 6; i++) {
			double t = i * 2.0;
			points.add(new double[] {frog.x + fx * t, frog.y, frog.z + fz * t});
		}
		TrackSpline branch = registry.replace(
				TrackSpline.fromPoints(UUID.randomUUID(), "world", false, points));
		registry.attachBranch(placed.id, branch.getId());
		registry.putJunction(registry.getJunction(placed.id).orElseThrow()
				.withTurnoutEndS(branch.length()));
		UUID stemId = stem.getId();
		UUID branchId = branch.getId();
		assertTrue(registry.getJunction(placed.id).orElseThrow().turnoutEndS > 0);
		DigResult result = registry.digAt(branch, 0);
		assertEquals(DigResult.Kind.DELETED, result.kind);
		assertTrue(result.removedJunctionTurnout);
		assertTrue(registry.get(stemId).isPresent());
		assertFalse(registry.get(branchId).isPresent());
		assertFalse(registry.getJunction(placed.id).isPresent());
	}

	@Test
	void digBranch_extended_keepsTail(@TempDir java.nio.file.Path dir) throws Exception {
		TrackRegistry registry = new TrackRegistry(dir.toFile());
		TrackSpline stem = registry.lay("world", 0, 64, 0, 0, 64, 40).spline();
		TrackJunction placed = registry.putJunction(node(stem, 10));
		TrackSpline branch = registry.layBranch(placed.id, "world", null, 2, 64, 28);
		TrackSample tip = branch.last();
		TrackSample prev = branch.getSamples().get(branch.getSamples().size() - 2);
		double dx = tip.x - prev.x;
		double dy = tip.y - prev.y;
		double dz = tip.z - prev.z;
		double seg = Math.sqrt(dx * dx + dy * dy + dz * dz);
		double scale = 35.0 / seg;
		TrackSpline extended = registry.lay(
				"world",
				tip.x, tip.y, tip.z,
				tip.x + dx * scale, tip.y + dy * scale, tip.z + dz * scale).spline();
		UUID branchId = branch.getId();
		assertEquals(branchId, extended.getId());
		double turnoutEnd = registry.getJunction(placed.id).orElseThrow().turnoutEndS;
		assertTrue(extended.length() > turnoutEnd + 1e-9);
		DigResult result = registry.digAt(extended, 0);
		assertEquals(DigResult.Kind.UPDATED, result.kind);
		assertTrue(result.removedJunctionTurnout);
		assertTrue(registry.get(stem.getId()).isPresent());
		assertTrue(registry.get(branchId).isPresent());
		assertTrue(registry.get(branchId).orElseThrow().length() > 1e-9);
		assertFalse(registry.getJunction(placed.id).isPresent());
	}

	@Test
	void digBranch_farFromFrog_normalDig(@TempDir java.nio.file.Path dir) throws Exception {
		TrackRegistry registry = new TrackRegistry(dir.toFile());
		TrackSpline stem = registry.lay("world", 0, 64, 0, 0, 64, 40).spline();
		TrackJunction placed = registry.putJunction(node(stem, 10));
		TrackSpline branch = registry.layBranch(placed.id, "world", null, 2, 64, 28);
		TrackSample tip = branch.last();
		TrackSample prev = branch.getSamples().get(branch.getSamples().size() - 2);
		double dx = tip.x - prev.x;
		double dy = tip.y - prev.y;
		double dz = tip.z - prev.z;
		double seg = Math.sqrt(dx * dx + dy * dy + dz * dz);
		double scale = 35.0 / seg;
		TrackSpline extended = registry.lay(
				"world",
				tip.x, tip.y, tip.z,
				tip.x + dx * scale, tip.y + dy * scale, tip.z + dz * scale).spline();
		int farIndex = -1;
		double turnoutEnd = registry.getJunction(placed.id).orElseThrow().turnoutEndS;
		for (int i = 0; i < extended.getSamples().size(); i++) {
			if (extended.getSamples().get(i).s > turnoutEnd + 1e-9) {
				farIndex = i;
				break;
			}
		}
		assertTrue(farIndex > 0);
		DigResult result = registry.digAt(extended, farIndex);
		assertFalse(result.removedJunctionTurnout);
		assertTrue(registry.getJunction(placed.id).isPresent());
		assertNotEquals(DigResult.Kind.NONE, result.kind);
	}

	@Test
	void digBranch_middleOfStub_removesWholeStub(@TempDir java.nio.file.Path dir) throws Exception {
		TrackRegistry registry = new TrackRegistry(dir.toFile());
		TrackSpline stem = registry.lay("world", 0, 64, 0, 0, 64, 40).spline();
		TrackJunction placed = registry.putJunction(node(stem, 10));
		TrackSpline branch = registry.layBranch(placed.id, "world", null, 2, 64, 28);
		double turnoutEnd = registry.getJunction(placed.id).orElseThrow().turnoutEndS;
		int midIndex = middleStubIndex(branch, turnoutEnd);
		assertTrue(midIndex > 0);
		UUID stemId = stem.getId();
		UUID branchId = branch.getId();
		DigResult result = registry.digAt(branch, midIndex);
		assertTrue(result.removedJunctionTurnout);
		assertTrue(registry.get(stemId).isPresent());
		assertFalse(registry.get(branchId).isPresent());
		assertFalse(registry.getJunction(placed.id).isPresent());
	}

	@Test
	void digBranch_longTurnout_removesFullLayNotCappedAt16(@TempDir java.nio.file.Path dir) throws Exception {
		TrackRegistry registry = new TrackRegistry(dir.toFile());
		TrackSpline stem = registry.lay("world", 0, 64, 0, 0, 64, 50).spline();
		TrackJunction placed = registry.putJunction(node(stem, 10));
		TrackSpline branch = registry.layBranch(placed.id, "world", null, 0, 64, 38);
		TrackJunction junction = registry.getJunction(placed.id).orElseThrow();
		assertTrue(junction.turnoutEndS > Cache.trackJunctionArmDistance + 1e-9);
		UUID branchId = branch.getId();
		DigResult result = registry.digAt(branch, 0);
		assertTrue(result.removedJunctionTurnout);
		assertFalse(registry.getJunction(placed.id).isPresent());
		assertFalse(registry.get(branchId).isPresent());
		assertTrue(registry.get(stem.getId()).isPresent());
	}

	@Test
	void digBranch_middleOfStubOnExtendedBranch_keepsTail(@TempDir java.nio.file.Path dir) throws Exception {
		TrackRegistry registry = new TrackRegistry(dir.toFile());
		TrackSpline stem = registry.lay("world", 0, 64, 0, 0, 64, 40).spline();
		TrackJunction placed = registry.putJunction(node(stem, 10));
		TrackSpline branch = registry.layBranch(placed.id, "world", null, 2, 64, 28);
		double turnoutEnd = registry.getJunction(placed.id).orElseThrow().turnoutEndS;
		TrackSample tip = branch.last();
		TrackSample prev = branch.getSamples().get(branch.getSamples().size() - 2);
		double dx = tip.x - prev.x;
		double dy = tip.y - prev.y;
		double dz = tip.z - prev.z;
		double seg = Math.sqrt(dx * dx + dy * dy + dz * dz);
		double scale = 35.0 / seg;
		TrackSpline extended = registry.lay(
				"world",
				tip.x, tip.y, tip.z,
				tip.x + dx * scale, tip.y + dy * scale, tip.z + dz * scale).spline();
		UUID branchId = branch.getId();
		assertEquals(branchId, extended.getId());
		int midIndex = middleStubIndex(extended, turnoutEnd);
		assertTrue(midIndex > 0);
		double extendedLength = extended.length();
		DigResult result = registry.digAt(extended, midIndex);
		assertEquals(DigResult.Kind.UPDATED, result.kind);
		assertTrue(result.removedJunctionTurnout);
		assertTrue(registry.get(stem.getId()).isPresent());
		assertFalse(registry.getJunction(placed.id).isPresent());
		TrackSpline kept = registry.get(branchId).orElseThrow();
		assertTrue(kept.length() > 1e-9);
		assertTrue(kept.length() < extendedLength - 1e-9);
	}

	@Test
	void layBranch_stemPath_setsTurnoutEndSAndDigRemovesStub(@TempDir java.nio.file.Path dir) throws Exception {
		TrackRegistry registry = new TrackRegistry(dir.toFile());
		TrackSpline stem = registry.lay("world", 0, 64, 0, 0, 64, 50).spline();
		TrackSpline branch = registry.layBranch(stem.getId(), 10, 1, "world", null, 2, 64, 28);
		TrackJunction junction = registry.junctionByBranch(branch.getId()).orElseThrow();
		assertTrue(junction.turnoutEndS > 0);
		assertEquals(branch.length(), junction.turnoutEndS, 1e-9);
		DigResult result = registry.digAt(branch, 0);
		assertTrue(result.removedJunctionTurnout);
		assertFalse(registry.getJunction(junction.id).isPresent());
		assertFalse(registry.get(branch.getId()).isPresent());
		assertTrue(registry.get(stem.getId()).isPresent());
	}

	@Test
	void split_dropsJunctionOnTooShortTail(@TempDir java.nio.file.Path dir) throws Exception {
		TrackRegistry registry = new TrackRegistry(dir.toFile());
		TrackSpline stem = registry.lay("world", 0, 64, 0, 0, 64, 40).spline();
		TrackJunction placed = registry.putJunction(completed(stem, 0.5));
		DigResult result = registry.digAt(stem, 2);
		assertEquals(DigResult.Kind.SPLIT, result.kind);
		assertTrue(result.kept.length() < 8);
		assertFalse(registry.getJunction(placed.id).isPresent());
	}

	@Test
	void pruneNestedShortTracks_removesOverlay(@TempDir java.nio.file.Path dir) throws Exception {
		TrackRegistry registry = new TrackRegistry(dir.toFile());
		TrackSpline stem = registry.lay("world", 0, 64, 0, 0, 64, 40).spline();
		TrackPose pose = stem.sampleAt(20);
		UUID overlayId = UUID.randomUUID();
		registry.replace(TrackSpline.fromPoints(
				overlayId, "world", false,
				List.of(
						new double[] {pose.x, pose.y, pose.z},
						new double[] {pose.x + 0.1, pose.y, pose.z + 0.1})));
		registry.pruneNestedShortTracks();
		assertFalse(registry.get(overlayId).isPresent());
		assertTrue(registry.get(stem.getId()).isPresent());
	}

	@Test
	void pruneNestedShortTracks_keepsNormalBranch(@TempDir java.nio.file.Path dir) throws Exception {
		TrackRegistry registry = new TrackRegistry(dir.toFile());
		TrackSpline stem = registry.lay("world", 0, 64, 0, 0, 64, 40).spline();
		TrackJunction placed = registry.putJunction(node(stem, 10));
		TrackSpline branch = registry.layBranch(placed.id, "world", null, 2, 64, 28);
		registry.pruneNestedShortTracks();
		assertTrue(registry.get(branch.getId()).isPresent());
		assertTrue(registry.getJunction(placed.id).isPresent());
	}

	@Test
	void loadFromDisk_prunesNestedOverlay(@TempDir java.nio.file.Path dir) throws Exception {
		TrackRegistry registry = new TrackRegistry(dir.toFile());
		TrackSpline stem = registry.lay("world", 0, 64, 0, 0, 64, 40).spline();
		TrackPose pose = stem.sampleAt(20);
		UUID overlayId = UUID.randomUUID();
		registry.replace(TrackSpline.fromPoints(
				overlayId, "world", false,
				List.of(
						new double[] {pose.x, pose.y, pose.z},
						new double[] {pose.x + 0.1, pose.y, pose.z + 0.1})));
		TrackRegistry reloaded = new TrackRegistry(dir.toFile());
		reloaded.loadFromDisk();
		assertFalse(reloaded.get(overlayId).isPresent());
		assertTrue(reloaded.get(stem.getId()).isPresent());
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

	private static int middleStubIndex(TrackSpline branch, double turnoutEnd) {
		for (int i = 0; i < branch.getSamples().size(); i++) {
			double s = branch.getSamples().get(i).s;
			if (s > 1e-9 && s < turnoutEnd - 1e-9) {
				return i;
			}
		}
		return -1;
	}
}
