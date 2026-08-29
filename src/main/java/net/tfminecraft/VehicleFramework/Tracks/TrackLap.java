package net.tfminecraft.VehicleFramework.Tracks;

public final class TrackLap {
	private TrackLap() {
	}

	public static double wrapDelta(double prevS, double s, double length) {
		if (length <= 1e-9) {
			return 0;
		}
		double ds = s - prevS;
		if (ds < -length / 2) {
			ds += length;
		}
		if (ds > length / 2) {
			ds -= length;
		}
		return Math.abs(ds);
	}

	public static boolean complete(double traveled, double length) {
		return length > 1.0 && traveled + 1e-6 >= length;
	}
}
