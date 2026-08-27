package net.tfminecraft.VehicleFramework.Weapons;

import org.bukkit.util.Vector;

import net.tfminecraft.VehicleFramework.Bones.ConvertedAngle;

public final class WeaponAimAligner {

	public static final float SETTLE_STOP_DEG = 1.0f;
	public static final float SETTLE_START_DEG = 6.0f;
	private static final float RATE_TAU_DEG = 14f;

	private WeaponAimAligner() {
	}

	public static float yawError(Vector current, Vector desired) {
		return yawError(current, ConvertedAngle.fromDirection(desired));
	}

	public static float yawError(Vector current, ConvertedAngle desired) {
		if (current == null || desired == null) {
			return 0f;
		}
		ConvertedAngle currentAngles = ConvertedAngle.fromDirection(current);
		return ConvertedAngle.shortestDelta(currentAngles.getYaw(), desired.getYaw());
	}

	public static float elevationError(Vector current, Vector desired, String headAxis) {
		return elevationError(current, ConvertedAngle.fromDirection(desired), headAxis);
	}

	public static float elevationError(Vector current, ConvertedAngle desired, String headAxis) {
		if (current == null || desired == null) {
			return 0f;
		}
		ConvertedAngle currentAngles = ConvertedAngle.fromDirection(current);
		return ConvertedAngle.shortestDelta(currentAngles.getPitch(), desired.getPitch());
	}

	public static float toBoneElevationStep(float worldPitchError) {
		return -worldPitchError;
	}

	public static float toBoneYawStep(float worldYawError) {
		return -worldYawError;
	}

	public static boolean updateSettled(float errorDeg, boolean currentlySettled) {
		float absError = Math.abs(errorDeg);
		if (currentlySettled) {
			return absError < SETTLE_START_DEG;
		}
		return absError <= SETTLE_STOP_DEG;
	}

	public static float followStep(float errorDeg, float maxRateDeg, boolean settled) {
		float absError = Math.abs(errorDeg);
		if (maxRateDeg <= 0f) {
			return 0f;
		}
		if (settled) {
			if (absError < SETTLE_START_DEG) {
				return 0f;
			}
		} else if (absError <= SETTLE_STOP_DEG) {
			return 0f;
		}
		float factor = (float) (1.0 - Math.exp(-absError / RATE_TAU_DEG));
		float step = Math.min(maxRateDeg, maxRateDeg * factor);
		step = Math.min(step, absError);
		return Math.copySign(step, errorDeg);
	}
}
