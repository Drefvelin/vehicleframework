package net.tfminecraft.VehicleFramework.Tracks;

import java.util.UUID;

public final class DigResult {
	public enum Kind {
		NONE,
		DELETED,
		UPDATED,
		SPLIT
	}

	public final Kind kind;
	public final UUID deletedId;
	public final TrackSpline kept;
	public final TrackSpline tail;

	private DigResult(Kind kind, UUID deletedId, TrackSpline kept, TrackSpline tail) {
		this.kind = kind;
		this.deletedId = deletedId;
		this.kept = kept;
		this.tail = tail;
	}

	public static DigResult none() {
		return new DigResult(Kind.NONE, null, null, null);
	}

	public static DigResult deleted(UUID id) {
		return new DigResult(Kind.DELETED, id, null, null);
	}

	public static DigResult updated(TrackSpline spline) {
		return new DigResult(Kind.UPDATED, spline.getId(), spline, null);
	}

	public static DigResult updated(UUID previousId, TrackSpline spline) {
		return new DigResult(Kind.UPDATED, previousId, spline, null);
	}

	public static DigResult split(TrackSpline start, TrackSpline tail) {
		return new DigResult(Kind.SPLIT, null, start, tail);
	}
}
