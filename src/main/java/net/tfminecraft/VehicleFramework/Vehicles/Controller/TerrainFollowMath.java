package net.tfminecraft.VehicleFramework.Vehicles.Controller;

import java.util.List;
import java.util.function.DoublePredicate;

import org.bukkit.Location;
import org.bukkit.util.Vector;

import net.tfminecraft.VehicleFramework.Bones.ConvertedAngle;

public final class TerrainFollowMath {

	public enum ForwardObstacle {
		NONE,
		STEP,
		WALL
	}

	private TerrainFollowMath() {
	}

	public static ForwardObstacle classifyForward(double footY, double obstacleTopY, double stepHeight) {
		double rise = obstacleTopY - footY;
		if (rise <= 0.05) {
			return ForwardObstacle.NONE;
		}
		if (rise <= stepHeight + 0.05) {
			return ForwardObstacle.STEP;
		}
		return ForwardObstacle.WALL;
	}

	public static boolean usesBodyFallback(List<String> groundProbes) {
		return groundProbes == null || groundProbes.isEmpty();
	}

	public static Location bodySampleOrigin(List<String> groundProbes, Location bodyFallback) {
		if (bodyFallback == null) {
			return null;
		}
		return bodyFallback.clone();
	}

	public static boolean destinationOverlaps(
			double destMinX, double destMaxX,
			double destMinY, double destMaxY,
			double destMinZ, double destMaxZ,
			double blockMinX, double blockMaxX,
			double blockMinY, double blockMaxY,
			double blockMinZ, double blockMaxZ) {
		return destMinX < blockMaxX && destMaxX > blockMinX
				&& destMinY < blockMaxY && destMaxY > blockMinY
				&& destMinZ < blockMaxZ && destMaxZ > blockMinZ;
	}

	public static final double SLIDE_STEP = 0.02;

	/**
	 * Minecraft yaw: 0 looks +Z (south), 90 looks -X (west).
	 */
	public static Vector headingFromYaw(float yawDeg) {
		double rad = Math.toRadians(yawDeg);
		return new Vector(-Math.sin(rad), 0, Math.cos(rad));
	}

	/**
	 * Farthest distance in [0, maxDist] that is not blocked, sampled every {@code step}.
	 * Full {@code maxDist} is used when it is clear. {@code blockedAtDist} is true when that distance clips.
	 */
	public static double farthestUnblocked(double maxDist, double step, DoublePredicate blockedAtDist) {
		if (maxDist <= 1e-9 || blockedAtDist == null) {
			return 0;
		}
		if (!blockedAtDist.test(maxDist)) {
			return maxDist;
		}
		double lastClear = 0;
		double increment = Math.max(1e-6, step);
		for (double d = increment; d < maxDist - 1e-12; d += increment) {
			if (blockedAtDist.test(d)) {
				return lastClear;
			}
			lastClear = d;
		}
		return lastClear;
	}

	public static double approachY(double currentY, double targetY, double maxDelta) {
		double cap = Math.max(0.0, maxDelta);
		double delta = targetY - currentY;
		if (delta > cap) {
			return currentY + cap;
		}
		if (delta < -cap) {
			return currentY - cap;
		}
		return targetY;
	}

	@FunctionalInterface
	public interface OffsetYBlocked {
		boolean test(double offsetX, double offsetZ, double y);
	}

	public static final class KinematicMove {
		public final double offsetX;
		public final double offsetZ;
		public final double y;
		public final String path;
		public final boolean aabbBlocked;

		public KinematicMove(double offsetX, double offsetZ, double y, String path, boolean aabbBlocked) {
			this.offsetX = offsetX;
			this.offsetZ = offsetZ;
			this.y = y;
			this.path = path;
			this.aabbBlocked = aabbBlocked;
		}

		public double slideDist() {
			return Math.hypot(offsetX, offsetZ);
		}
	}

	public static double effectiveSnap(double snapSpeed, double speed, double climbLeadFactor, double stepHeight) {
		double cap = Math.max(0.0, snapSpeed) + Math.max(0.0, speed) * Math.max(0.0, climbLeadFactor);
		double max = Math.max(0.0, stepHeight);
		return Math.min(max, cap);
	}

	/**
	 * Climb support is the higher of wheel probes and lookahead. Lookahead cannot pull dest Y down.
	 */
	public static double mergeClimbSupport(double wheelSupportY, double lookaheadY) {
		if (Double.isNaN(wheelSupportY)) {
			return wheelSupportY;
		}
		if (Double.isNaN(lookaheadY) || lookaheadY <= wheelSupportY) {
			return wheelSupportY;
		}
		return lookaheadY;
	}

	public static double climbTarget(double currentY, double contactY, double stepTopY) {
		double target = Double.NaN;
		if (!Double.isNaN(stepTopY) && stepTopY > currentY + 0.05) {
			target = stepTopY;
		}
		if (!Double.isNaN(contactY) && contactY > currentY + 0.05) {
			target = Double.isNaN(target) ? contactY : Math.max(target, contactY);
		}
		return target;
	}

	public static final class AirborneTick {
		public final double vx;
		public final double vy;
		public final double vz;

		public AirborneTick(double vx, double vy, double vz) {
			this.vx = vx;
			this.vy = vy;
			this.vz = vz;
		}
	}

	public static AirborneTick airborneTick(double vx, double vy, double vz, double drag, double gravity) {
		double d = Math.max(0.0, drag);
		return new AirborneTick(vx * d, vy - gravity, vz * d);
	}

	/**
	 * Raise Y at the current XZ when climbing. No XZ until dest Y is at STEP top.
	 * Then slide along heading; if blocked, slides X then Z. Does not drop while XZ is blocked.
	 */
	public static KinematicMove raiseThenSlide(
			double dx,
			double dz,
			double currentY,
			double destY,
			double contactY,
			double stepTopY,
			double snapSpeed,
			OffsetYBlocked blocked) {
		return raiseThenSlide(dx, dz, currentY, destY, contactY, stepTopY, snapSpeed, blocked, false);
	}

	public static KinematicMove raiseThenSlide(
			double dx,
			double dz,
			double currentY,
			double destY,
			double contactY,
			double stepTopY,
			double snapSpeed,
			OffsetYBlocked blocked,
			boolean reverse) {
		double dist = Math.hypot(dx, dz);
		double y = currentY;
		String path = "stay";
		double climbTo = climbTarget(currentY, contactY, stepTopY);
		boolean climbed = false;
		double ox = 0;
		double oz = 0;
		double raisedY = Double.isNaN(climbTo) ? currentY : approachY(currentY, climbTo, snapSpeed);
		boolean raiseOk = !Double.isNaN(climbTo) && (blocked == null || !blocked.test(0, 0, raisedY));
		if (raiseOk) {
			y = raisedY;
			path = "up";
			climbed = true;
		} else if (!reverse && blocked != null && !Double.isNaN(stepTopY) && dist > 1e-9) {
			boolean xzBlocked = farthestUnblocked(
					dist, SLIDE_STEP, d -> blocked.test(dx * d / dist, dz * d / dist, currentY)) <= 1e-6;
			if (xzBlocked) {
				double hx = dx / dist;
				double hz = dz / dist;
				double backFar = farthestUnblocked(
						0.2, SLIDE_STEP, d -> blocked.test(-hx * d, -hz * d, currentY));
				double backUse = 0;
				if (backFar >= 0.1 - 1e-9) {
					backUse = 0.1;
				} else if (backFar > 1e-6) {
					backUse = backFar;
				}
				if (backUse > 1e-6) {
					ox = -hx * backUse;
					oz = -hz * backUse;
					path = "back";
					if (!Double.isNaN(climbTo) && !blocked.test(ox, oz, raisedY)) {
						y = raisedY;
						path = "back+up";
						climbed = true;
					}
				}
			}
		}

		boolean wantedMove = dist > 1e-6;
		boolean headingFull = false;
		boolean holdForStep = !Double.isNaN(stepTopY) && y < stepTopY - 0.02;
		if (dist > 1e-9 && blocked != null && !holdForStep) {
			final double baseX = ox;
			final double baseZ = oz;
			final double slideY = y;
			double far = farthestUnblocked(
					dist, SLIDE_STEP, d -> blocked.test(baseX + dx * d / dist, baseZ + dz * d / dist, slideY));
			if (far > 1e-6) {
				ox = baseX + dx * far / dist;
				oz = baseZ + dz * far / dist;
				headingFull = far + 1e-6 >= dist;
				String slidePart = headingFull ? "slide" : "slidePartial";
				if (climbed) {
					path = path.startsWith("back") ? "back+up+" + slidePart : "up+" + slidePart;
				} else if ("back".equals(path)) {
					path = "back+" + slidePart;
				} else {
					path = slidePart;
				}
			} else {
				double sx = Math.signum(dx);
				double sz = Math.signum(dz);
				double farX = Math.abs(dx) < 1e-9 ? 0
						: farthestUnblocked(Math.abs(dx), SLIDE_STEP, d -> blocked.test(baseX + sx * d, baseZ, slideY));
				final double axisX = baseX + sx * farX;
				ox = axisX;
				double farZ = Math.abs(dz) < 1e-9 ? 0
						: farthestUnblocked(Math.abs(dz), SLIDE_STEP, d -> blocked.test(axisX, baseZ + sz * d, slideY));
				oz = baseZ + sz * farZ;
				boolean movedX = Math.abs(ox - baseX) > 1e-6;
				boolean movedZ = Math.abs(oz - baseZ) > 1e-6;
				if (Math.abs(ox - baseX) + 1e-6 >= Math.abs(dx) && Math.abs(oz - baseZ) + 1e-6 >= Math.abs(dz)) {
					headingFull = true;
				}
				if (movedX || movedZ) {
					String axis = movedX && movedZ ? "slideX+slideZ" : (movedX ? "slideX" : "slideZ");
					if (climbed) {
						path = path.startsWith("back") ? "back+up+" + axis : "up+" + axis;
					} else if ("back".equals(path)) {
						path = "back+" + axis;
					} else {
						path = axis;
					}
				}
			}
		} else if (dist > 1e-9 && blocked == null && !holdForStep) {
			ox = dx;
			oz = dz;
			headingFull = true;
			path = climbed ? "up+slide" : "slide";
		}

		double moved = Math.hypot(ox, oz);
		boolean noSlide = wantedMove && moved <= 1e-6;
		boolean aabbBlocked = wantedMove && !headingFull;
		boolean xzBlocked = noSlide && wantedMove;
		if (blocked == null) {
			return new KinematicMove(ox, oz, y, path, aabbBlocked);
		}

		boolean canDrop = !xzBlocked && !climbed;
		boolean downhill = canDrop && !Double.isNaN(contactY) && contactY < currentY - 1e-6
				&& (Double.isNaN(stepTopY) || stepTopY <= contactY + 0.05)
				&& Double.isNaN(climbTo);
		if (downhill) {
			double dropped = contactY;
			if (!blocked.test(ox, oz, dropped)) {
				y = dropped;
				if ("stay".equals(path)) {
					path = "down";
				} else {
					path = path + "+down";
				}
			}
		} else if (canDrop && Math.abs(destY - currentY) > 1e-6) {
			if (!blocked.test(ox, oz, destY)) {
				y = destY;
				boolean falling = destY < currentY;
				String yPart = falling ? "fall" : "y";
				if ("stay".equals(path)) {
					path = yPart;
				} else {
					path = path + "+" + yPart;
				}
			}
		}

		if ("stay".equals(path) && xzBlocked && !climbed && Math.abs(y - currentY) <= 1e-6) {
			return new KinematicMove(0, 0, currentY, "stay", true);
		}
		return new KinematicMove(ox, oz, y, path, aabbBlocked && wantedMove);
	}

	public static Vector flattenHorizontal(Vector velocity) {
		if (velocity == null) {
			return new Vector(0, 0, 0);
		}
		Vector flat = velocity.clone();
		flat.setY(0);
		return flat;
	}

	public static final class Tilt {
		public final float pitchDeg;
		public final float rollDeg;

		public Tilt(float pitchDeg, float rollDeg) {
			this.pitchDeg = pitchDeg;
			this.rollDeg = rollDeg;
		}
	}

	public static final float TILT_CLAMP = 25f;

	public static float clampTilt(double degrees) {
		return (float) Math.max(-TILT_CLAMP, Math.min(TILT_CLAMP, degrees));
	}

	public static double supportY(double[] hitY, boolean[] hit) {
		if (hitY == null || hit == null) {
			return Double.NaN;
		}
		int n = Math.min(hitY.length, hit.length);
		double max = Double.NEGATIVE_INFINITY;
		boolean any = false;
		for (int i = 0; i < n; i++) {
			if (!hit[i]) {
				continue;
			}
			any = true;
			if (hitY[i] > max) {
				max = hitY[i];
			}
		}
		return any ? max : Double.NaN;
	}

	public static Tilt tiltFromWorldHits(Vector fl, Vector fr, Vector bl, Vector br) {
		if (fl == null || fr == null || bl == null || br == null) {
			return new Tilt(0f, 0f);
		}
		Vector midFront = fl.clone().add(fr).multiply(0.5);
		Vector midBack = bl.clone().add(br).multiply(0.5);
		Vector midRight = fr.clone().add(br).multiply(0.5);
		Vector midLeft = fl.clone().add(bl).multiply(0.5);
		return tiltFromWorldAxes(midFront.subtract(midBack), midRight.subtract(midLeft));
	}

	public static Tilt tiltFromWorldAxes(Vector forward, Vector right) {
		float pitch = ConvertedAngle.fromDirection(forward).getPitch();
		float roll = ConvertedAngle.fromDirection(right).getPitch();
		return new Tilt(clampTilt(pitch), clampTilt(roll));
	}
}
