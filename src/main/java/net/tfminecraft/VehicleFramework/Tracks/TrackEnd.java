package net.tfminecraft.VehicleFramework.Tracks;

public final class TrackEnd {
	public final TrackSpline spline;
	public final boolean prepend;

	public TrackEnd(TrackSpline spline, boolean prepend) {
		this.spline = spline;
		this.prepend = prepend;
	}
}
