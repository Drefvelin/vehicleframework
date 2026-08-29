package net.tfminecraft.VehicleFramework.Tracks;

import org.bukkit.util.Vector;

import net.tfminecraft.VehicleFramework.Bones.ConvertedAngle;

public final class TrackSplineMotion {
	public static final double STOP_EPS = 0.005;
	public static final double MOVE_EPS_SQ = 1e-8;

	private TrackSplineMotion() {
	}

	public static ConvertedAngle worldHeading(Vector move, TrackPose pose, int travelSign) {
		return ConvertedAngle.fromDirection(tangentFromPose(pose, 1));
	}

	public static float boneYaw(float worldYaw, float entityYaw) {
		return ConvertedAngle.wrapDegrees(-(worldYaw - entityYaw));
	}

	public static float bonePitch(float worldPitch) {
		return worldPitch;
	}

	public static Vector tangentFromPose(TrackPose pose, int travelSign) {
		if (pose == null) {
			return new Vector(0, 0, 1);
		}
		double yawRad = Math.toRadians(pose.yaw);
		double pitchRad = Math.toRadians(pose.pitch);
		double horiz = Math.cos(pitchRad);
		double x = -Math.sin(yawRad) * horiz;
		double y = -Math.sin(pitchRad);
		double z = Math.cos(yawRad) * horiz;
		if (travelSign < 0) {
			x = -x;
			z = -z;
		}
		return new Vector(x, y, z);
	}

	public static boolean stopped(double speed) {
		return Math.abs(speed) < STOP_EPS;
	}
}
