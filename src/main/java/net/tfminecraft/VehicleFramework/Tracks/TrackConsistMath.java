package net.tfminecraft.VehicleFramework.Tracks;

public final class TrackConsistMath {
	private TrackConsistMath() {
	}

	public static double connectorSpacing(double parentBackLength, double childFrontLength) {
		return Math.max(0, parentBackLength) + Math.max(0, childFrontLength);
	}

	public static double[] carS(double locoS, double[] spacings) {
		if (spacings == null || spacings.length == 0) {
			return new double[0];
		}
		double[] out = new double[spacings.length];
		double acc = 0;
		for (int i = 0; i < spacings.length; i++) {
			acc += Math.max(0, spacings[i]);
			out[i] = locoS - acc;
		}
		return out;
	}
}
