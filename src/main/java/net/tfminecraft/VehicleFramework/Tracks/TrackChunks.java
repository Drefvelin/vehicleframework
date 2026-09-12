package net.tfminecraft.VehicleFramework.Tracks;

public final class TrackChunks {
	private TrackChunks() {
	}

	public static int chunkCoord(double block) {
		return (int) Math.floor(block / 16.0);
	}

	public static boolean inChunk(double x, double z, int chunkX, int chunkZ) {
		return chunkCoord(x) == chunkX && chunkCoord(z) == chunkZ;
	}

	public static int edgeIndexForSample(int sampleIndex, int sampleCount, boolean loop) {
		int edges = TrackSpline.edgeCount(sampleCount, loop);
		if (edges <= 0) {
			return 0;
		}
		if (sampleIndex < 0) {
			return 0;
		}
		return Math.min(sampleIndex, edges - 1);
	}
}
