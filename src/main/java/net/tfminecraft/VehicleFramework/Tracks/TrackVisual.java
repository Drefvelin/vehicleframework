package net.tfminecraft.VehicleFramework.Tracks;

public final class TrackVisual {
	public enum Type {
		SMALL,
		MEDIUM,
		LARGE
	}

	public final Type type;
	public final int startIndex;
	public final int length;
	public final int fromEdge;
	public final int span;
	public final double x;
	public final double y;
	public final double z;
	public final float yaw;
	public final float pitch;

	public TrackVisual(
			Type type,
			int startIndex,
			int length,
			int fromEdge,
			int span,
			double x,
			double y,
			double z,
			float yaw,
			float pitch) {
		this.type = type;
		this.startIndex = startIndex;
		this.length = length;
		this.fromEdge = fromEdge;
		this.span = Math.max(1, span);
		this.x = x;
		this.y = y;
		this.z = z;
		this.yaw = yaw;
		this.pitch = pitch;
	}

	public int coveredEdge(int offset, int edgeCount) {
		if (edgeCount <= 0) {
			return fromEdge;
		}
		int edge = fromEdge + offset;
		if (edge < 0) {
			edge = edgeCount + (edge % edgeCount);
		}
		return edge % edgeCount;
	}
}
