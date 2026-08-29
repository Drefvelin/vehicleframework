package net.tfminecraft.VehicleFramework.Tracks;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Display.Billboard;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import me.Plugins.TLibs.TLibs;
import net.tfminecraft.VehicleFramework.VFLogger;
import net.tfminecraft.VehicleFramework.VehicleFramework;
import net.tfminecraft.VehicleFramework.Cache.Cache;

public final class TrackDisplayManager implements Listener {
	public static final String KEY_ID = "track_id";
	public static final String KEY_EDGE = "track_edge";
	public static final String KEY_SPAN = "track_span";
	public static final String KEY_SWITCH = "junction_switch";

	private final List<ItemDisplay> live = new ArrayList<>();
	private final Set<TrackVisual.Type> missingLogged = new HashSet<>();
	private boolean missingSwitchLogged;

	public TrackDisplayManager() {
		VehicleFramework plugin = VehicleFramework.getInstance();
		if (plugin != null) {
			plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
				tickSwitches();
				TrackBuildAnimator.tick();
			}, 1L, 1L);
		}
	}

	public NamespacedKey idKey() {
		return new NamespacedKey(VehicleFramework.getInstance(), KEY_ID);
	}

	public NamespacedKey edgeKey() {
		return new NamespacedKey(VehicleFramework.getInstance(), KEY_EDGE);
	}

	public NamespacedKey switchKey() {
		return new NamespacedKey(VehicleFramework.getInstance(), KEY_SWITCH);
	}

	public NamespacedKey spanKey() {
		return new NamespacedKey(VehicleFramework.getInstance(), KEY_SPAN);
	}

	public void spawnLoadedChunks() {
		for (World world : Bukkit.getWorlds()) {
			for (Chunk chunk : world.getLoadedChunks()) {
				spawnChunk(chunk);
			}
		}
	}

	public void spawnSpline(TrackSpline spline) {
		World world = Bukkit.getWorld(spline.getWorld());
		if (world == null) {
			return;
		}
		Set<Long> seen = new HashSet<>();
		for (TrackVisual visual : spline.visuals()) {
			int cx = TrackChunks.chunkCoord(visual.x);
			int cz = TrackChunks.chunkCoord(visual.z);
			long key = chunkKey(cx, cz);
			if (!seen.add(key)) {
				continue;
			}
			if (!world.isChunkLoaded(cx, cz)) {
				continue;
			}
			spawnSplineInChunk(spline, world.getChunkAt(cx, cz));
		}
		spawnSwitchesForSpline(spline);
	}

	public void despawnTrackDisplays(UUID id) {
		Iterator<ItemDisplay> it = live.iterator();
		while (it.hasNext()) {
			ItemDisplay display = it.next();
			if (id.equals(readId(display))) {
				display.remove();
				it.remove();
			}
		}
	}

	public void spawnGhost(UUID splineId, List<double[]> points, World world, Chunk chunk) {
		if (splineId == null || points == null || points.size() < 2 || world == null) {
			return;
		}
		TrackSpline ghost = TrackSpline.fromPoints(splineId, world.getName(), false, points);
		replaceFrom(splineId, List.of(), ghost.visuals(), world, chunk);
	}

	public void replaceFrom(
			UUID splineId,
			List<TrackVisual> previous,
			List<TrackVisual> next,
			World world,
			Chunk chunk) {
		if (splineId == null || world == null) {
			return;
		}
		List<TrackVisual> old = previous == null ? List.of() : previous;
		List<TrackVisual> neu = next == null ? List.of() : next;
		int d = TrackVisualDiff.firstChange(old, neu);
		for (int i = d; i < old.size(); i++) {
			despawnVisual(splineId, old.get(i));
		}
		for (int i = d; i < neu.size(); i++) {
			TrackVisual visual = neu.get(i);
			if (chunk != null && !TrackChunks.inChunk(visual.x, visual.z, chunk.getX(), chunk.getZ())) {
				continue;
			}
			spawnDisplay(world, splineId, visual);
		}
	}

	public void spawnVisuals(UUID splineId, List<TrackVisual> visuals, World world, Chunk chunk) {
		if (splineId == null || visuals == null || world == null) {
			return;
		}
		for (TrackVisual visual : visuals) {
			if (chunk != null && !TrackChunks.inChunk(visual.x, visual.z, chunk.getX(), chunk.getZ())) {
				continue;
			}
			spawnDisplay(world, splineId, visual);
		}
	}

	public void despawnSpline(UUID id) {
		Iterator<ItemDisplay> it = live.iterator();
		while (it.hasNext()) {
			ItemDisplay display = it.next();
			if (id.equals(readId(display))) {
				display.remove();
				it.remove();
			}
		}
		TrackRegistry registry = VehicleFramework.getTrackRegistry();
		if (registry == null) {
			return;
		}
		for (TrackJunction junction : registry.junctionsOn(id)) {
			despawnSwitch(junction.id);
		}
	}

	public void despawnAll() {
		for (ItemDisplay display : live) {
			if (display != null && !display.isDead()) {
				display.remove();
			}
		}
		live.clear();
	}

	public void spawnChunk(Chunk chunk) {
		removeTaggedInChunk(chunk);
		TrackRegistry registry = VehicleFramework.getTrackRegistry();
		if (registry == null) {
			return;
		}
		for (TrackSpline spline : registry.inWorld(chunk.getWorld().getName())) {
			if (TrackBuildAnimator.isBuilding(spline.getId())) {
				TrackBuildAnimator.spawnIntoChunk(spline, chunk);
			} else {
				spawnSplineInChunk(spline, chunk);
			}
		}
		spawnSwitchesInChunk(chunk);
	}

	public void breakInRadius(Location center, double radius) {
		if (center == null || center.getWorld() == null || radius <= 0) {
			return;
		}
		TrackRegistry registry = VehicleFramework.getTrackRegistry();
		if (registry == null) {
			return;
		}
		double r2 = radius * radius;
		String world = center.getWorld().getName();
		double x = center.getX();
		double y = center.getY();
		double z = center.getZ();
		for (TrackSpline spline : registry.inWorld(world)) {
			TrackSpline current = spline;
			Set<Integer> edges = new HashSet<>();
			List<TrackSample> samples = spline.getSamples();
			for (int i = 0; i < samples.size(); i++) {
				TrackSample sample = samples.get(i);
				double dx = sample.x - x;
				double dy = sample.y - y;
				double dz = sample.z - z;
				if (dx * dx + dy * dy + dz * dz > r2) {
					continue;
				}
				int edge = TrackChunks.edgeIndexForSample(i, samples.size(), spline.isLoop());
				if (current.segment(edge).broken) {
					continue;
				}
				current = current.withSegment(edge, current.segment(edge).withBroken(true));
				edges.add(edge);
			}
			if (edges.isEmpty()) {
				continue;
			}
			registry.replace(current);
			rebakeSpline(spline.getId());
		}
	}

	public void rebakeSpline(UUID id) {
		despawnSpline(id);
		TrackRegistry registry = VehicleFramework.getTrackRegistry();
		if (registry == null) {
			return;
		}
		registry.get(id).ifPresent(this::spawnSpline);
	}

	@EventHandler
	public void onChunkLoad(ChunkLoadEvent event) {
		spawnChunk(event.getChunk());
	}

	@EventHandler
	public void onChunkUnload(ChunkUnloadEvent event) {
		removeTaggedInChunk(event.getChunk());
	}

	@EventHandler
	public void onPunch(EntityDamageByEntityEvent event) {
		if (!(event.getDamager() instanceof Player)) {
			return;
		}
		if (!(event.getEntity() instanceof ItemDisplay display)) {
			return;
		}
		if (isSwitchDisplay(display)) {
			event.setCancelled(true);
			return;
		}
		if (!isTrackDisplay(display)) {
			return;
		}
		event.setCancelled(true);
		Player player = (Player) event.getDamager();
		ItemStack hand = player.getInventory().getItemInMainHand();
		if (TrackTools.isLayer(hand)) {
			if (!TrackCommands.skipDuplicateToolUse(player)) {
				TrackCommands.markStart(player, aimedLocation(player, display));
			}
			return;
		}
		if (TrackTools.isJunction(hand)) {
			return;
		}
		if (TrackTools.isRemover(hand)) {
			if (!TrackCommands.skipDuplicateToolUse(player)) {
				TrackCommands.digAt(player, display.getLocation());
			}
			return;
		}
		breakFromDisplay(display);
	}

	@EventHandler
	public void onInteract(PlayerInteractEntityEvent event) {
		if (event.getHand() != EquipmentSlot.HAND) {
			return;
		}
		if (!(event.getRightClicked() instanceof ItemDisplay display)) {
			return;
		}
		if (isSwitchDisplay(display)) {
			event.setCancelled(true);
			return;
		}
		if (!isTrackDisplay(display)) {
			return;
		}
		event.setCancelled(true);
		Player player = event.getPlayer();
		ItemStack hand = player.getInventory().getItemInMainHand();
		if (TrackTools.isLayer(hand)) {
			TrackCommands.markEnd(player, aimedLocation(player, display));
			return;
		}
		if (TrackTools.isJunction(hand)) {
			if (!TrackCommands.skipDuplicateToolUse(player)) {
				TrackCommands.startJunction(player, aimedLocation(player, display));
			}
		}
	}

	private static Location aimedLocation(Player player, ItemDisplay display) {
		Location at = display.getLocation();
		at.setYaw(player.getLocation().getYaw());
		at.setPitch(player.getLocation().getPitch());
		return at;
	}

	@EventHandler
	public void onEntityExplode(EntityExplodeEvent event) {
		breakInRadius(event.getLocation(), Math.max(2.0, event.getYield()));
	}

	@EventHandler
	public void onBlockExplode(BlockExplodeEvent event) {
		breakInRadius(event.getBlock().getLocation(), Math.max(2.0, event.getYield()));
	}

	private void spawnSplineInChunk(TrackSpline spline, Chunk chunk) {
		if (TrackBuildAnimator.isBuilding(spline.getId())) {
			TrackBuildAnimator.spawnIntoChunk(spline, chunk);
			return;
		}
		int cx = chunk.getX();
		int cz = chunk.getZ();
		World world = chunk.getWorld();
		for (TrackVisual visual : spline.visuals()) {
			if (!TrackChunks.inChunk(visual.x, visual.z, cx, cz)) {
				continue;
			}
			spawnDisplay(world, spline.getId(), visual);
		}
	}

	private void spawnSwitchesInChunk(Chunk chunk) {
		TrackRegistry registry = VehicleFramework.getTrackRegistry();
		if (registry == null) {
			return;
		}
		for (TrackSpline spline : registry.inWorld(chunk.getWorld().getName())) {
			for (TrackJunction junction : registry.junctionsOn(spline.getId())) {
				spawnSwitchInChunk(junction, spline, chunk);
			}
		}
	}

	public void spawnSwitchesForSpline(TrackSpline spline) {
		if (spline == null) {
			return;
		}
		TrackRegistry registry = VehicleFramework.getTrackRegistry();
		if (registry == null) {
			return;
		}
		World world = Bukkit.getWorld(spline.getWorld());
		if (world == null) {
			return;
		}
		for (TrackJunction junction : registry.junctionsOn(spline.getId())) {
			TrackSwitchPose pose = switchPose(spline, junction);
			if (pose == null) {
				continue;
			}
			int cx = TrackChunks.chunkCoord(pose.x);
			int cz = TrackChunks.chunkCoord(pose.z);
			if (!world.isChunkLoaded(cx, cz)) {
				continue;
			}
			spawnSwitchInChunk(junction, spline, world.getChunkAt(cx, cz));
		}
	}

	public void refreshSwitch(TrackJunction junction) {
		if (junction == null || junction.branchSplineId == null) {
			if (junction != null) {
				despawnSwitch(junction.id);
			}
			return;
		}
		TrackRegistry registry = VehicleFramework.getTrackRegistry();
		if (registry == null) {
			return;
		}
		TrackSpline stem = registry.get(junction.stemSplineId).orElse(null);
		if (stem == null) {
			despawnSwitch(junction.id);
			return;
		}
		World world = Bukkit.getWorld(stem.getWorld());
		if (world == null) {
			return;
		}
		TrackSwitchPose pose = switchPose(stem, junction);
		if (pose == null) {
			return;
		}
		int cx = TrackChunks.chunkCoord(pose.x);
		int cz = TrackChunks.chunkCoord(pose.z);
		if (!world.isChunkLoaded(cx, cz)) {
			despawnSwitch(junction.id);
			return;
		}
		spawnSwitchInChunk(junction, stem, world.getChunkAt(cx, cz));
	}

	public void despawnSwitch(UUID junctionId) {
		if (junctionId == null) {
			return;
		}
		Iterator<ItemDisplay> it = live.iterator();
		while (it.hasNext()) {
			ItemDisplay display = it.next();
			if (junctionId.equals(readSwitchId(display))) {
				display.remove();
				it.remove();
			}
		}
	}

	private void spawnSwitchInChunk(TrackJunction junction, TrackSpline stem, Chunk chunk) {
		if (junction == null || junction.branchSplineId == null || stem == null || chunk == null) {
			return;
		}
		TrackSwitchPose pose = switchPose(stem, junction);
		if (pose == null) {
			return;
		}
		if (!TrackChunks.inChunk(pose.x, pose.z, chunk.getX(), chunk.getZ())) {
			return;
		}
		despawnSwitch(junction.id);
		ItemStack item = itemFromPath(Cache.trackSwitchItem);
		if (item == null) {
			if (!missingSwitchLogged) {
				missingSwitchLogged = true;
				VFLogger.log("Track switch item is missing: " + Cache.trackSwitchItem);
			}
			return;
		}
		World world = chunk.getWorld();
		Location loc = new Location(
				world,
				pose.x,
				pose.y + Cache.trackDisplayYOffset,
				pose.z,
				pose.targetYaw,
				0);
		ItemDisplay display = world.spawn(loc, ItemDisplay.class, spawned -> {
			spawned.setPersistent(false);
			spawned.setGravity(false);
			spawned.setInvulnerable(false);
			spawned.setBillboard(Billboard.FIXED);
			spawned.setItemStack(item);
			spawned.getPersistentDataContainer().set(
					switchKey(), PersistentDataType.STRING, junction.id.toString());
		});
		live.add(display);
	}

	private static TrackSwitchPose switchPose(TrackSpline stem, TrackJunction junction) {
		if (stem == null || junction == null) {
			return null;
		}
		return TrackSwitchPose.of(
				stem.sampleAt(junction.s),
				junction,
				Cache.trackSwitchOffsetAlong,
				Cache.trackSwitchOffsetOut,
				Cache.trackSwitchOffsetY,
				Cache.trackSwitchYawInward,
				Cache.trackSwitchThrowDegrees);
	}

	private void tickSwitches() {
		TrackRegistry registry = VehicleFramework.getTrackRegistry();
		if (registry == null) {
			return;
		}
		float maxDelta = Cache.trackSwitchThrowDegreesPerSecond / 20f;
		Iterator<ItemDisplay> it = live.iterator();
		while (it.hasNext()) {
			ItemDisplay display = it.next();
			if (display == null || display.isDead() || !display.isValid()) {
				it.remove();
				continue;
			}
			UUID junctionId = readSwitchId(display);
			if (junctionId == null) {
				continue;
			}
			TrackJunction junction = registry.getJunction(junctionId).orElse(null);
			if (junction == null || junction.branchSplineId == null) {
				display.remove();
				it.remove();
				continue;
			}
			TrackSpline stem = registry.get(junction.stemSplineId).orElse(null);
			TrackSwitchPose pose = switchPose(stem, junction);
			if (pose == null) {
				continue;
			}
			float next = TrackSwitchPose.stepYaw(display.getLocation().getYaw(), pose.targetYaw, maxDelta);
			Location loc = display.getLocation();
			loc.setYaw(next);
			display.teleport(loc);
		}
	}

	private void spawnDisplay(World world, UUID splineId, TrackVisual visual) {
		ItemStack item = trackItem(visual.type);
		if (item == null) {
			return;
		}
		Location loc = new Location(world, visual.x, visual.y + Cache.trackDisplayYOffset, visual.z, visual.yaw, visual.pitch);
		ItemDisplay display = world.spawn(loc, ItemDisplay.class, spawned -> {
			spawned.setPersistent(false);
			spawned.setGravity(false);
			spawned.setInvulnerable(false);
			spawned.setBillboard(Billboard.FIXED);
			spawned.setItemStack(item);
			spawned.getPersistentDataContainer().set(idKey(), PersistentDataType.STRING, splineId.toString());
			spawned.getPersistentDataContainer().set(edgeKey(), PersistentDataType.INTEGER, visual.fromEdge);
			spawned.getPersistentDataContainer().set(spanKey(), PersistentDataType.INTEGER, visual.span);
		});
		live.add(display);
	}

	private ItemStack trackItem(TrackVisual.Type type) {
		String path = pathFor(type);
		ItemStack item = itemFromPath(path);
		if (item != null) {
			return item;
		}
		logMissing(type, path);
		if (type != TrackVisual.Type.SMALL) {
			ItemStack small = itemFromPath(Cache.trackItemSmall);
			if (small != null) {
				return small;
			}
			logMissing(TrackVisual.Type.SMALL, Cache.trackItemSmall);
		}
		return null;
	}

	private static String pathFor(TrackVisual.Type type) {
		if (type == TrackVisual.Type.LARGE) {
			return Cache.trackItemLarge;
		}
		if (type == TrackVisual.Type.MEDIUM) {
			return Cache.trackItemMedium;
		}
		return Cache.trackItemSmall;
	}

	private ItemStack itemFromPath(String path) {
		if (path == null || path.isBlank()) {
			return null;
		}
		try {
			ItemStack item = TLibs.getItemAPI().getCreator().getItemFromPath(path);
			if (item == null || item.getType().isAir()) {
				return null;
			}
			return item;
		} catch (Exception e) {
			return null;
		}
	}

	private void logMissing(TrackVisual.Type type, String path) {
		if (!missingLogged.add(type)) {
			return;
		}
		VFLogger.log("Track display item is missing: " + path);
	}

	private void breakFromDisplay(ItemDisplay display) {
		UUID id = readId(display);
		Integer edge = readEdge(display);
		if (id == null || edge == null) {
			return;
		}
		TrackRegistry registry = VehicleFramework.getTrackRegistry();
		if (registry == null) {
			return;
		}
		Optional<TrackSpline> found = registry.get(id);
		if (found.isEmpty()) {
			despawnSpline(id);
			return;
		}
		TrackSpline spline = found.get();
		int span = readSpan(display);
		int edgeCount = Math.max(1, spline.edgeCount());
		TrackSpline current = spline;
		boolean changed = false;
		for (int k = 0; k < span; k++) {
			int covered = Math.floorMod(edge + k, edgeCount);
			if (current.segment(covered).broken) {
				continue;
			}
			current = current.withSegment(covered, current.segment(covered).withBroken(true));
			changed = true;
		}
		if (changed) {
			registry.replace(current);
		}
		rebakeSpline(id);
	}

	private int readSpan(ItemDisplay display) {
		Integer span = display.getPersistentDataContainer().get(spanKey(), PersistentDataType.INTEGER);
		if (span == null || span < 1) {
			return 1;
		}
		return span;
	}

	private void removeTaggedInChunk(Chunk chunk) {
		for (Entity entity : chunk.getEntities()) {
			if (!(entity instanceof ItemDisplay display)) {
				continue;
			}
			if (isTrackDisplay(display) || isSwitchDisplay(display)) {
				display.remove();
			}
		}
		live.removeIf(display -> display == null || display.isDead() || !display.isValid());
	}

	private boolean isTrackDisplay(ItemDisplay display) {
		return display.getPersistentDataContainer().has(idKey(), PersistentDataType.STRING);
	}

	private boolean isSwitchDisplay(ItemDisplay display) {
		return display.getPersistentDataContainer().has(switchKey(), PersistentDataType.STRING);
	}

	private UUID readSwitchId(ItemDisplay display) {
		String raw = display.getPersistentDataContainer().get(switchKey(), PersistentDataType.STRING);
		if (raw == null) {
			return null;
		}
		try {
			return UUID.fromString(raw);
		} catch (IllegalArgumentException e) {
			return null;
		}
	}

	private void despawnVisual(UUID splineId, TrackVisual visual) {
		if (visual == null) {
			return;
		}
		Iterator<ItemDisplay> it = live.iterator();
		while (it.hasNext()) {
			ItemDisplay display = it.next();
			if (!splineId.equals(readId(display)) || isSwitchDisplay(display)) {
				continue;
			}
			Integer edge = readEdge(display);
			if (edge == null || edge != visual.fromEdge) {
				continue;
			}
			double dx = display.getLocation().getX() - visual.x;
			double dz = display.getLocation().getZ() - visual.z;
			if (dx * dx + dz * dz > 1.0) {
				continue;
			}
			display.remove();
			it.remove();
			return;
		}
	}

	private UUID readId(ItemDisplay display) {
		String raw = display.getPersistentDataContainer().get(idKey(), PersistentDataType.STRING);
		if (raw == null) {
			return null;
		}
		try {
			return UUID.fromString(raw);
		} catch (IllegalArgumentException e) {
			return null;
		}
	}

	private Integer readEdge(ItemDisplay display) {
		return display.getPersistentDataContainer().get(edgeKey(), PersistentDataType.INTEGER);
	}

	private static long chunkKey(int cx, int cz) {
		return ((long) cx << 32) ^ (cz & 0xffffffffL);
	}
}
