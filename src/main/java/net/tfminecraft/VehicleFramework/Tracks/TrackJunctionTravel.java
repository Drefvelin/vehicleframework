package net.tfminecraft.VehicleFramework.Tracks;

import java.util.UUID;

public final class TrackJunctionTravel {
	public enum Choice {
		THROUGH,
		DIVERGE
	}

	public static final class Pose {
		public final UUID splineId;
		public final double s;

		public Pose(UUID splineId, double s) {
			this.splineId = splineId;
			this.s = s;
		}
	}

	private TrackJunctionTravel() {
	}

	public static boolean diverge(TrackJunction.Side side, TrackJunction.Side hold) {
		if (side == null || hold == null) {
			return false;
		}
		return side == hold;
	}

	public static boolean diverge(TrackJunction.Side side, float strafe) {
		return diverge(side, holdFromStrafe(strafe));
	}

	public static TrackJunction.Side holdFromStrafe(float strafe) {
		if (strafe > 1e-6) {
			return TrackJunction.Side.LEFT;
		}
		if (strafe < -1e-6) {
			return TrackJunction.Side.RIGHT;
		}
		return null;
	}

	public static Choice choice(TrackJunction.Side side, TrackJunction.Side hold) {
		return diverge(side, hold) ? Choice.DIVERGE : Choice.THROUGH;
	}

	public static Choice choice(TrackJunction.Side side, float strafe) {
		return choice(side, holdFromStrafe(strafe));
	}

	public static final double ARM_WINDOW = 16.0;

	public static double ahead(
			double from,
			double junctionS,
			int travelSign,
			boolean loop,
			double length) {
		if (length <= 1e-12) {
			return Double.POSITIVE_INFINITY;
		}
		int sign = travelSign < 0 ? -1 : 1;
		double a = TrackJunction.wrapS(from, length, loop);
		double j = TrackJunction.wrapS(junctionS, length, loop);
		if (!loop) {
			return (j - a) * sign;
		}
		if (sign > 0) {
			if (j >= a - 1e-12) {
				return j - a;
			}
			return length - a + j;
		}
		if (j <= a + 1e-12) {
			return a - j;
		}
		return a + (length - j);
	}

	public static boolean inArmWindow(
			double from,
			double junctionS,
			int travelSign,
			boolean loop,
			double length) {
		return inArmWindow(from, junctionS, travelSign, loop, length, ARM_WINDOW);
	}

	public static boolean inArmWindow(
			double from,
			double junctionS,
			int travelSign,
			boolean loop,
			double length,
			double window) {
		double dist = ahead(from, junctionS, travelSign, loop, length);
		return dist >= -1e-9 && dist <= window;
	}

	public static boolean facing(int travelSign, int facingSign) {
		int travel = travelSign < 0 ? -1 : 1;
		int facing = facingSign < 0 ? -1 : 1;
		return travel == facing;
	}

	public static boolean crosses(
			double from,
			double to,
			double junctionS,
			int travelSign,
			boolean loop,
			double length) {
		if (length <= 1e-12) {
			return false;
		}
		int sign = travelSign < 0 ? -1 : 1;
		double a = TrackJunction.wrapS(from, length, loop);
		double b = TrackJunction.wrapS(to, length, loop);
		double j = TrackJunction.wrapS(junctionS, length, loop);
		if (Math.abs(a - j) <= 1e-9) {
			return false;
		}
		if (!loop) {
			if (sign > 0) {
				return a < j && b + 1e-12 >= j;
			}
			return a > j && b - 1e-12 <= j;
		}
		if (sign > 0) {
			boolean wrapped = b + 1e-9 < a;
			if (!wrapped) {
				return a < j && b + 1e-12 >= j;
			}
			return j >= a || j <= b;
		}
		boolean wrapped = b > a + 1e-9;
		if (!wrapped) {
			return a > j && b - 1e-12 <= j;
		}
		return j <= a || j >= b;
	}

	public static Pose rewind(
			UUID parentSplineId,
			double parentS,
			int travelSign,
			double spacing,
			boolean takeBranch,
			UUID stemId,
			UUID branchId,
			double junctionS,
			int facingSign,
			double stemLength,
			boolean stemLoop,
			double branchLength) {
		if (parentSplineId == null) {
			return new Pose(null, parentS);
		}
		double behind = Math.max(0, spacing);
		if (behind <= 1e-12) {
			return new Pose(parentSplineId, parentS);
		}
		int travel = travelSign < 0 ? -1 : 1;
		int facing = facingSign < 0 ? -1 : 1;
		boolean onBranch = takeBranch && branchId != null && parentSplineId.equals(branchId);
		if (onBranch) {
			if (parentS >= behind) {
				return new Pose(branchId, parentS - behind);
			}
			double leftover = behind - Math.max(0, parentS);
			double stemS = junctionS - facing * leftover;
			stemS = TrackJunction.wrapS(stemS, stemLength, stemLoop);
			return new Pose(stemId != null ? stemId : parentSplineId, stemS);
		}
		UUID stem = stemId != null ? stemId : parentSplineId;
		if (!takeBranch || branchId == null) {
			double childS = parentS - travel * behind;
			return new Pose(stem, TrackJunction.wrapS(childS, stemLength, stemLoop));
		}
		double alongBehind = (parentS - junctionS) * travel;
		if (alongBehind >= behind) {
			double childS = parentS - travel * behind;
			return new Pose(stem, TrackJunction.wrapS(childS, stemLength, stemLoop));
		}
		if (alongBehind <= 1e-9) {
			double childS = parentS - travel * behind;
			return new Pose(stem, TrackJunction.wrapS(childS, stemLength, stemLoop));
		}
		double leftover = behind - alongBehind;
		double branchS = Math.min(leftover, Math.max(0, branchLength));
		return new Pose(branchId, branchS);
	}
}
