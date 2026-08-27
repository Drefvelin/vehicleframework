package net.tfminecraft.VehicleFramework.Vehicles.Controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import com.ticxo.modelengine.api.model.ActiveModel;
import com.ticxo.modelengine.api.model.bone.ModelBone;

import net.tfminecraft.VehicleFramework.Cache.Cache;
import net.tfminecraft.VehicleFramework.VFLogger;
import net.tfminecraft.VehicleFramework.Bones.VectorBone;
import net.tfminecraft.VehicleFramework.Enums.Direction;
import net.tfminecraft.VehicleFramework.Enums.State;
import net.tfminecraft.VehicleFramework.Vehicles.ActiveVehicle;
import net.tfminecraft.VehicleFramework.Vehicles.Handlers.State.TerrainFollowConfig;

public final class TerrainFollowEngine {

	private static final double DOWN_RAY_LENGTH = 2.5;
	private static final double GRAVITY = 0.08;
	private static final int AIR_MISS_TICKS = 2;
	private static final String BODY_BONE = "body";
	private static final Set<String> loggedMissingProbes = ConcurrentHashMap.newKeySet();
	private static final ConcurrentHashMap<String, FollowRuntime> runtime = new ConcurrentHashMap<>();

	private static final class FollowRuntime {
		int missCount;
		boolean airborne;
		double vx;
		double vy;
		double vz;
	}

	private TerrainFollowEngine() {
	}

	public static final class Result {
		public final Location location;
		public final Vector velocity;
		public final TerrainFollowMath.Tilt tilt;

		public Result(Location location, Vector velocity, TerrainFollowMath.Tilt tilt) {
			this.location = location;
			this.velocity = velocity;
			this.tilt = tilt;
		}
	}

	public static Result step(
			ActiveVehicle v,
			VectorBone vector,
			Direction dir,
			BaseController base,
			TerrainFollowConfig config) {
		Entity entity = v.getEntity();
		if (entity == null || !entity.isValid()) {
			if (v.getUUID() != null) {
				runtime.remove(v.getUUID());
			}
			return null;
		}
		FollowRuntime rt = runtime.computeIfAbsent(v.getUUID(), key -> new FollowRuntime());
		Location current = entity.getLocation().clone();
		BoundingBox box = entity.getBoundingBox();
		double footY = box.getMinY();
		double locOffset = current.getY() - footY;

		List<Location> probeStarts = resolveProbeStarts(v, config);
		List<Location> debugHits = new ArrayList<>();
		List<Location> debugStarts = new ArrayList<>();
		double[] hitY;
		boolean[] hit;
		int hitCount = 0;
		double wheelSupportY = Double.NaN;
		TerrainFollowMath.Tilt tilt = null;
		if (probeStarts.isEmpty()) {
			Location downStart = bodyDownStart(v, current, config);
			debugStarts.add(downStart);
			RayTraceResult down = rayDown(downStart);
			if (down != null && down.getHitPosition() != null) {
				hitCount = 1;
				wheelSupportY = down.getHitPosition().getY() + locOffset;
				if (downStart.getWorld() != null) {
					debugHits.add(down.getHitPosition().toLocation(downStart.getWorld()));
				}
			}
		} else {
			debugStarts.addAll(probeStarts);
			int n = probeStarts.size();
			hitY = new double[n];
			hit = new boolean[n];
			for (int i = 0; i < n; i++) {
				RayTraceResult down = rayDown(probeStarts.get(i));
				if (down != null && down.getHitPosition() != null) {
					hit[i] = true;
					hitY[i] = down.getHitPosition().getY();
					hitCount++;
					World world = probeStarts.get(i).getWorld();
					if (world != null) {
						debugHits.add(down.getHitPosition().toLocation(world));
					}
				}
			}
			if (hitCount > 0) {
				wheelSupportY = TerrainFollowMath.supportY(hitY, hit) + locOffset;
			}
			if (n == 4 && hitCount == 4) {
				Location flLoc = probeStarts.get(0);
				Location frLoc = probeStarts.get(1);
				Location blLoc = probeStarts.get(2);
				Location brLoc = probeStarts.get(3);
				Vector fl = new Vector(flLoc.getX(), hitY[0], flLoc.getZ());
				Vector fr = new Vector(frLoc.getX(), hitY[1], frLoc.getZ());
				Vector bl = new Vector(blLoc.getX(), hitY[2], blLoc.getZ());
				Vector br = new Vector(brLoc.getX(), hitY[3], brLoc.getZ());
				tilt = TerrainFollowMath.tiltFromWorldHits(fl, fr, bl, br);
			}
		}

		boolean grounded = hitCount > 0;
		if (grounded) {
			rt.missCount = 0;
			rt.airborne = false;
			rt.vy = 0;
		} else {
			rt.missCount++;
			if (rt.missCount >= AIR_MISS_TICKS) {
				if (!rt.airborne) {
					rt.airborne = true;
					rt.vy = 0;
				}
			}
		}

		if (rt.airborne && !grounded) {
			spawnTerrainDebug(v, debugStarts, debugHits);
			return airborneStep(v, entity, current, dir, config, rt, footY, hitCount);
		}

		Vector horizontal = TerrainFollowMath.flattenHorizontal(
				base.horizontalMoveVector(v, vector, dir));
		double speed = horizontal.length();
		Vector heading = speed < 1e-6 ? new Vector(0, 0, 0) : horizontal.clone().normalize();
		double effectiveSnap = TerrainFollowMath.effectiveSnap(
				config.getSnapSpeed(), speed, config.getClimbLeadFactor(), config.getStepHeight());

		double destX = current.getX();
		double destZ = current.getZ();
		double destY = current.getY();
		double proposedX = destX;
		double proposedZ = destZ;
		String fwdLog = "none";
		double stepTopY = Double.NaN;

		if (speed >= 1e-6) {
			proposedX = current.getX() + heading.getX() * speed;
			proposedZ = current.getZ() + heading.getZ() * speed;
			double look = speed + Math.max(box.getWidthX(), box.getWidthZ()) * 0.5 + 0.25;
			Location foot = new Location(current.getWorld(), current.getX(), footY + 0.08, current.getZ());
			RayTraceResult forward = current.getWorld().rayTraceBlocks(
					foot,
					heading,
					look,
					FluidCollisionMode.NEVER,
					true);
			boolean blocked = false;
			if (forward != null && forward.getHitBlock() != null) {
				double top = collisionTopY(forward.getHitBlock());
				TerrainFollowMath.ForwardObstacle kind = TerrainFollowMath.classifyForward(
						footY,
						top,
						config.getStepHeight());
				fwdLog = kind.name() + " top=" + String.format("%.3f", top);
				if (kind == TerrainFollowMath.ForwardObstacle.WALL) {
					blocked = true;
				} else if (kind == TerrainFollowMath.ForwardObstacle.STEP) {
					stepTopY = top + locOffset;
				}
			}
			if (!blocked) {
				destX = proposedX;
				destZ = proposedZ;
			}
		}

		double lookaheadY = Double.NaN;
		if (speed >= 1e-6) {
			Location lookOrigin = probeStarts.isEmpty()
					? bodyDownStart(v, current, config)
					: new Location(current.getWorld(), current.getX(), footY + 0.2, current.getZ());
			lookaheadY = sampleLookaheadY(
					lookOrigin, heading, speed * config.getClimbLeadTicks(), locOffset, debugHits);
		}

		double destYRaw = destY;
		double contactY = Double.NaN;
		if (grounded) {
			contactY = TerrainFollowMath.mergeClimbSupport(wheelSupportY, lookaheadY);
			destYRaw = contactY;
			destY = destYFromContact(current.getY(), contactY, effectiveSnap);
		} else {
			destY = destY - GRAVITY;
			destYRaw = destY;
		}

		spawnTerrainDebug(v, debugStarts, debugHits);
		DestResolve resolved = resolveDest(
				entity, current, destX, destY, destZ, contactY, stepTopY, effectiveSnap, config.getStepHeight(), dir);
		storeGroundedMomentum(rt, current, resolved.dest, heading, speed);
		Result result = interpolResult(current, resolved.dest, speed, tilt, false);
		logStep(
				v, dir, speed, footY, current, proposedX, proposedZ, fwdLog, destYRaw, destY, hitCount,
				resolved, result, lookaheadY, effectiveSnap, false, rt);
		return result;
	}

	/** Climb uses snap-speed; downhill snaps to probe support in one tick. */
	private static double destYFromContact(double currentY, double contactY, double snapSpeed) {
		if (contactY < currentY) {
			return contactY;
		}
		return TerrainFollowMath.approachY(currentY, contactY, snapSpeed);
	}

	private static Result interpolResult(
			Location current, Location dest, double speed, TerrainFollowMath.Tilt tilt, boolean airborne) {
		Vector interpol = dest.toVector().subtract(current.toVector());
		if (!airborne) {
			interpol.setY(0);
			if (interpol.lengthSquared() > 1.0) {
				interpol.normalize().multiply(speed);
			}
		}
		return new Result(dest, interpol, tilt);
	}

	private static final class DestResolve {
		final Location dest;
		final String path;
		final boolean aabbBlocked;

		DestResolve(Location dest, String path, boolean aabbBlocked) {
			this.dest = dest;
			this.path = path;
			this.aabbBlocked = aabbBlocked;
		}
	}

	private static DestResolve resolveDest(
			Entity entity,
			Location current,
			double destX,
			double destY,
			double destZ,
			double contactY,
			double stepTopY,
			double snapSpeed,
			double stepHeight,
			Direction dir) {
		Location unstuck = null;
		boolean reverse = dir == Direction.BACKWARD;
		boolean climbUnstick = !reverse && !Double.isNaN(contactY) && contactY > current.getY();
		if (climbUnstick && destinationBlocked(entity, current, current)) {
			Location nudged = current.clone();
			double maxY = current.getY() + stepHeight;
			for (double y = current.getY() + 0.05; y <= maxY + 1e-6; y += 0.05) {
				nudged.setY(y);
				if (!destinationBlocked(entity, current, nudged)) {
					unstuck = nudged.clone();
					break;
				}
			}
		}
		DestResolve moved = raiseThenSlide(entity, current, destX, destY, destZ, contactY, stepTopY, snapSpeed, reverse);
		if (!"stay".equals(moved.path)) {
			if (unstuck != null) {
				return new DestResolve(moved.dest, "unstick+" + moved.path, moved.aabbBlocked);
			}
			return moved;
		}
		if (unstuck != null) {
			return new DestResolve(unstuck, "unstick", true);
		}
		return moved;
	}

	private static DestResolve raiseThenSlide(
			Entity entity,
			Location current,
			double destX,
			double destY,
			double destZ,
			double contactY,
			double stepTopY,
			double snapSpeed,
			boolean reverse) {
		double dx = destX - current.getX();
		double dz = destZ - current.getZ();
		TerrainFollowMath.KinematicMove move = TerrainFollowMath.raiseThenSlide(
				dx,
				dz,
				current.getY(),
				destY,
				contactY,
				stepTopY,
				snapSpeed,
				(ox, oz, y) -> destinationBlocked(
						entity,
						current,
						locAt(current, current.getX() + ox, y, current.getZ() + oz)),
				reverse);
		Location dest = locAt(
				current,
				current.getX() + move.offsetX,
				move.y,
				current.getZ() + move.offsetZ);
		return new DestResolve(dest, move.path, move.aabbBlocked);
	}

	private static Result airborneStep(
			ActiveVehicle v,
			Entity entity,
			Location current,
			Direction dir,
			TerrainFollowConfig config,
			FollowRuntime rt,
			double footY,
			int hitCount) {
		TerrainFollowMath.AirborneTick tick = TerrainFollowMath.airborneTick(
				rt.vx, rt.vy, rt.vz, config.getAirDrag(), config.getAirGravity());
		rt.vx = tick.vx;
		rt.vy = tick.vy;
		rt.vz = tick.vz;
		double dx = rt.vx;
		double dz = rt.vz;
		double destY = current.getY() + rt.vy;
		boolean yBlocked = destinationBlocked(entity, current, locAt(current, current.getX(), destY, current.getZ()));
		if (yBlocked) {
			destY = current.getY();
			rt.vy = 0;
		}
		final double slideY = destY;
		double dist = Math.hypot(dx, dz);
		double ox = 0;
		double oz = 0;
		boolean aabbBlocked = false;
		if (dist > 1e-9) {
			double far = TerrainFollowMath.farthestUnblocked(
					dist,
					TerrainFollowMath.SLIDE_STEP,
					d -> destinationBlocked(
							entity,
							current,
							locAt(current, current.getX() + dx * d / dist, slideY, current.getZ() + dz * d / dist)));
			ox = dx * far / dist;
			oz = dz * far / dist;
			aabbBlocked = far + 1e-6 < dist;
			rt.vx = ox;
			rt.vz = oz;
		}
		Location dest = locAt(current, current.getX() + ox, destY, current.getZ() + oz);
		if (destinationBlocked(entity, current, dest)) {
			dest = current.clone();
			rt.vx = 0;
			rt.vz = 0;
			aabbBlocked = true;
		}
		DestResolve resolved = new DestResolve(dest, "air", aabbBlocked);
		Vector vel = new Vector(rt.vx, rt.vy, rt.vz);
		Result result = new Result(dest, vel, null);
		logStep(
				v, dir, Math.hypot(rt.vx, rt.vz), footY, current, current.getX() + dx, current.getZ() + dz,
				"none", destY, destY, hitCount, resolved, result, Double.NaN, 0, true, rt);
		return result;
	}

	private static void storeGroundedMomentum(
			FollowRuntime rt, Location current, Location dest, Vector heading, double speed) {
		double gx = dest.getX() - current.getX();
		double gz = dest.getZ() - current.getZ();
		if (Math.hypot(gx, gz) < 1e-6 && speed >= 1e-6) {
			gx = heading.getX() * speed;
			gz = heading.getZ() * speed;
		}
		rt.vx = gx;
		rt.vz = gz;
	}

	private static double sampleLookaheadY(
			Location origin, Vector heading, double lookahead, double locOffset, List<Location> debugHits) {
		if (origin == null || origin.getWorld() == null || heading == null || lookahead < 1e-6) {
			return Double.NaN;
		}
		double[] hitY = new double[2];
		boolean[] hit = new boolean[2];
		double[] fractions = {0.5, 1.0};
		for (int i = 0; i < fractions.length; i++) {
			double dist = lookahead * fractions[i];
			Location start = origin.clone().add(heading.getX() * dist, 0, heading.getZ() * dist);
			RayTraceResult down = rayDown(start);
			if (down != null && down.getHitPosition() != null) {
				hit[i] = true;
				hitY[i] = down.getHitPosition().getY();
				if (debugHits != null && start.getWorld() != null) {
					debugHits.add(down.getHitPosition().toLocation(start.getWorld()));
				}
			}
		}
		double support = TerrainFollowMath.supportY(hitY, hit);
		return Double.isNaN(support) ? Double.NaN : support + locOffset;
	}

	private static Location locAt(Location current, double x, double y, double z) {
		return new Location(current.getWorld(), x, y, z, current.getYaw(), current.getPitch());
	}

	private static void logStep(
			ActiveVehicle v,
			Direction dir,
			double speed,
			double footY,
			Location current,
			double proposedX,
			double proposedZ,
			String fwdLog,
			double destYRaw,
			double destYSnap,
			int hitCount,
			DestResolve resolved,
			Result result,
			double lookaheadY,
			double effectiveSnap,
			boolean airborne,
			FollowRuntime rt) {
		if (!GroundEngineLog.isEnabled()) {
			return;
		}
		Vector vel = result.velocity;
		State state = v.getCurrentState() == null ? null : v.getCurrentState().getType();
		String line = String.format(
				"id=%s state=%s dir=%s speed=%.3f footY=%.3f cur=%.3f,%.3f,%.3f proposedXZ=%.3f,%.3f fwd=%s destYraw=%.3f destYsnap=%.3f hitCount=%d aabbBlocked=%s resolve=%s dest=%.3f,%.3f,%.3f vel=%.3f,%.3f,%.3f lookaheadY=%s effSnap=%.3f air=%s vx=%.3f,%.3f,%.3f",
				v.getId(),
				state == null ? "null" : state.name(),
				dir == null ? "null" : dir.name(),
				speed,
				footY,
				current.getX(), current.getY(), current.getZ(),
				proposedX, proposedZ,
				fwdLog,
				destYRaw,
				destYSnap,
				hitCount,
				resolved.aabbBlocked,
				resolved.path,
				resolved.dest.getX(), resolved.dest.getY(), resolved.dest.getZ(),
				vel.getX(), vel.getY(), vel.getZ(),
				Double.isNaN(lookaheadY) ? "nan" : String.format("%.3f", lookaheadY),
				effectiveSnap,
				airborne,
				rt.vx, rt.vy, rt.vz);
		String engineFrag = GroundEngineLog.formatEngineFragment(v);
		if (!engineFrag.isEmpty()) {
			line += " " + engineFrag;
		}
		GroundEngineLog.append(line);
	}

	private static void spawnTerrainDebug(ActiveVehicle v, List<Location> starts, List<Location> hits) {
		if (!Cache.terrainFollowDebug || v == null) {
			return;
		}
		List<Player> nearby = v.getNearbyPlayers();
		if (nearby == null || nearby.isEmpty()) {
			return;
		}
		if (starts != null) {
			for (Location loc : starts) {
				spawnEndRod(nearby, loc);
			}
		}
		if (hits != null) {
			for (Location loc : hits) {
				spawnEndRod(nearby, loc);
			}
		}
	}

	private static void spawnEndRod(List<Player> nearby, Location loc) {
		if (loc == null || loc.getWorld() == null) {
			return;
		}
		for (Player p : nearby) {
			if (p == null || !p.isOnline() || p.getWorld() == null) {
				continue;
			}
			if (!p.getWorld().equals(loc.getWorld())) {
				continue;
			}
			p.spawnParticle(Particle.END_ROD, loc, 1, 0, 0, 0, 0);
		}
	}

	private static RayTraceResult rayDown(Location start) {
		if (start == null || start.getWorld() == null) {
			return null;
		}
		return start.getWorld().rayTraceBlocks(
				start,
				new Vector(0, -1, 0),
				DOWN_RAY_LENGTH,
				FluidCollisionMode.ALWAYS,
				true);
	}

	static List<Location> resolveProbeStarts(ActiveVehicle v, TerrainFollowConfig config) {
		List<Location> starts = new ArrayList<>();
		if (config == null || !config.hasGroundProbes()) {
			return starts;
		}
		ActiveModel model = v.getModel();
		if (model == null) {
			return starts;
		}
		for (String id : config.getGroundProbes()) {
			if (id == null || id.isBlank()) {
				continue;
			}
			if (model.getBone(id).isEmpty()) {
				String key = v.getUUID() + ":" + id;
				if (loggedMissingProbes.add(key)) {
					VFLogger.log("Vehicle " + v.getId() + " ground-probe bone '" + id + "' is missing, skipping");
				}
				continue;
			}
			ModelBone bone = model.getBone(id).get();
			Location loc = bone.getLocation();
			if (loc == null || loc.getWorld() == null) {
				continue;
			}
			starts.add(loc.clone().add(0, 0.2, 0));
		}
		return starts;
	}

	static Location bodyDownStart(ActiveVehicle v, Location entityLoc, TerrainFollowConfig config) {
		Location body = entityLoc.clone().add(0, 0.5, 0);
		ActiveModel model = v.getModel();
		if (model != null && model.getBone(BODY_BONE).isPresent()) {
			ModelBone bone = model.getBone(BODY_BONE).get();
			Location boneLoc = bone.getLocation();
			if (boneLoc != null && boneLoc.getWorld() != null) {
				body = boneLoc.clone().add(0, 0.2, 0);
			}
		}
		return TerrainFollowMath.bodySampleOrigin(config.getGroundProbes(), body);
	}

	private static double collisionTopY(Block block) {
		double top = block.getY();
		var shape = block.getCollisionShape();
		if (shape.getBoundingBoxes().isEmpty()) {
			return block.getY() + 1.0;
		}
		Location base = block.getLocation();
		for (BoundingBox local : shape.getBoundingBoxes()) {
			top = Math.max(top, base.getY() + local.getMaxY());
		}
		return top;
	}

	private static boolean destinationBlocked(Entity entity, Location from, Location dest) {
		BoundingBox box = entity.getBoundingBox().clone();
		double dx = dest.getX() - from.getX();
		double dy = dest.getY() - from.getY();
		double dz = dest.getZ() - from.getZ();
		BoundingBox shifted = box.shift(dx, dy, dz);
		shifted.expand(-0.05, 0, -0.05);
		double minY = shifted.getMinY() + 0.08;
		World world = dest.getWorld();
		int minBx = (int) Math.floor(shifted.getMinX());
		int maxBx = (int) Math.floor(shifted.getMaxX());
		int minBy = (int) Math.floor(minY);
		int maxBy = (int) Math.floor(shifted.getMaxY());
		int minBz = (int) Math.floor(shifted.getMinZ());
		int maxBz = (int) Math.floor(shifted.getMaxZ());
		for (int x = minBx; x <= maxBx; x++) {
			for (int y = minBy; y <= maxBy; y++) {
				for (int z = minBz; z <= maxBz; z++) {
					Block block = world.getBlockAt(x, y, z);
					if (block.isPassable()) {
						continue;
					}
					var shape = block.getCollisionShape();
					Location base = block.getLocation();
					for (BoundingBox local : shape.getBoundingBoxes()) {
						double bminX = base.getX() + local.getMinX();
						double bmaxX = base.getX() + local.getMaxX();
						double bminY = base.getY() + local.getMinY();
						double bmaxY = base.getY() + local.getMaxY();
						double bminZ = base.getZ() + local.getMinZ();
						double bmaxZ = base.getZ() + local.getMaxZ();
						if (TerrainFollowMath.destinationOverlaps(
								shifted.getMinX(), shifted.getMaxX(),
								minY, shifted.getMaxY(),
								shifted.getMinZ(), shifted.getMaxZ(),
								bminX, bmaxX, bminY, bmaxY, bminZ, bmaxZ)) {
							return true;
						}
					}
				}
			}
		}
		return false;
	}
}
