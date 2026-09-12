package net.tfminecraft.VehicleFramework.Tracks;

import net.tfminecraft.VehicleFramework.Bones.ConvertedAngle;

public final class TrackSwitchPose {
	public final double x;
	public final double y;
	public final double z;
	public final float throughYaw;
	public final float divergeYaw;
	public final float targetYaw;

	public TrackSwitchPose(
			double x,
			double y,
			double z,
			float throughYaw,
			float divergeYaw,
			float targetYaw) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.throughYaw = throughYaw;
		this.divergeYaw = divergeYaw;
		this.targetYaw = targetYaw;
	}

	public static TrackSwitchPose of(
			TrackPose frog,
			TrackJunction junction,
			double offsetAlong,
			double offsetOut,
			double offsetY,
			float yawInward,
			float throwDegrees) {
		if (frog == null || junction == null) {
			throw new IllegalArgumentException("switch pose needs frog and junction");
		}
		float facingYaw = ConvertedAngle.wrapDegrees(frog.yaw + (junction.facingSign < 0 ? 180f : 0f));
		double yawRad = Math.toRadians(facingYaw);
		double fx = -Math.sin(yawRad);
		double fz = Math.cos(yawRad);
		double rx = -Math.cos(yawRad);
		double rz = -Math.sin(yawRad);
		double outSign = junction.side == TrackJunction.Side.LEFT ? 1 : -1;
		double x = frog.x + offsetAlong * fx + offsetOut * outSign * rx;
		double z = frog.z + offsetAlong * fz + offsetOut * outSign * rz;
		double y = frog.y + offsetY;
		float throughYaw = ConvertedAngle.wrapDegrees(facingYaw + yawInward);
		float throwSign = junction.side == TrackJunction.Side.LEFT ? -1f : 1f;
		float divergeYaw = ConvertedAngle.wrapDegrees(throughYaw + throwDegrees * throwSign);
		float target = junction.thrown ? divergeYaw : throughYaw;
		return new TrackSwitchPose(x, y, z, throughYaw, divergeYaw, target);
	}

	public static float stepYaw(float from, float to, float maxDelta) {
		float delta = ConvertedAngle.shortestDelta(from, to);
		if (Math.abs(delta) <= maxDelta + 1e-4f) {
			return ConvertedAngle.wrapDegrees(to);
		}
		return ConvertedAngle.wrapDegrees(from + Math.copySign(maxDelta, delta));
	}
}
