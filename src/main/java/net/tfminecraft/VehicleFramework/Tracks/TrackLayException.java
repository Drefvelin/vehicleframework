package net.tfminecraft.VehicleFramework.Tracks;

public final class TrackLayException extends Exception {
	public final Integer blockX;
	public final Integer blockY;
	public final Integer blockZ;

	public TrackLayException(String message) {
		this(message, null, null, null);
	}

	public TrackLayException(String message, int x, int y, int z) {
		this(message, Integer.valueOf(x), Integer.valueOf(y), Integer.valueOf(z));
	}

	private TrackLayException(String message, Integer x, Integer y, Integer z) {
		super(message);
		this.blockX = x;
		this.blockY = y;
		this.blockZ = z;
	}

	public boolean hasBlock() {
		return blockX != null && blockY != null && blockZ != null;
	}
}
