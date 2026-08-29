package net.tfminecraft.VehicleFramework.Tracks;

import java.util.List;

public final class TrackVisualDiff {
	private TrackVisualDiff() {
	}

	public static int firstChange(List<TrackVisual> previous, List<TrackVisual> next) {
		List<TrackVisual> old = previous == null ? List.of() : previous;
		List<TrackVisual> neu = next == null ? List.of() : next;
		int n = Math.min(old.size(), neu.size());
		for (int i = 0; i < n; i++) {
			if (!same(old.get(i), neu.get(i))) {
				return i;
			}
		}
		return n;
	}

	public static boolean same(TrackVisual a, TrackVisual b) {
		if (a == b) {
			return true;
		}
		if (a == null || b == null) {
			return false;
		}
		return a.type == b.type
				&& a.startIndex == b.startIndex
				&& a.length == b.length
				&& a.fromEdge == b.fromEdge
				&& a.span == b.span
				&& Math.abs(a.x - b.x) < 0.01
				&& Math.abs(a.y - b.y) < 0.01
				&& Math.abs(a.z - b.z) < 0.01;
	}
}
