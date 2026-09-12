package net.tfminecraft.VehicleFramework.Tracks;

public final class TrackSegment {
	public final int fromIndex;
	public final boolean broken;
	public final double health;

	public TrackSegment(int fromIndex, boolean broken, double health) {
		this.fromIndex = fromIndex;
		this.broken = broken;
		this.health = health;
	}

	public TrackSegment withBroken(boolean broken) {
		return new TrackSegment(fromIndex, broken, health);
	}
}
