package net.tfminecraft.VehicleFramework.Tracks;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import net.tfminecraft.VehicleFramework.Bones.ConvertedAngle;

public final class TrackVisualBake {
	public static final float COLLINEAR_DEGREES = 8f;

	private TrackVisualBake() {
	}

	public static List<TrackVisual> bake(TrackSpline spline) {
		if (spline == null) {
			return List.of();
		}
		List<TrackSample> samples = spline.getSamples();
		int n = samples.size();
		boolean loop = spline.isLoop();
		boolean[] used = new boolean[n];
		List<TrackVisual> out = new ArrayList<>();
		for (int i = 0; i < n; i++) {
			if (used[i]) {
				continue;
			}
			if (mappedEdgeBroken(spline, i)) {
				used[i] = true;
				continue;
			}
			int take = 1;
			if (canTake(spline, used, i, 3)) {
				take = 3;
			} else if (canTake(spline, used, i, 2)) {
				take = 2;
			}
			out.add(piece(spline, i, take));
			mark(used, i, take, n, loop);
		}
		return out;
	}

	private static boolean canTake(TrackSpline spline, boolean[] used, int start, int length) {
		List<TrackSample> samples = spline.getSamples();
		int n = samples.size();
		boolean loop = spline.isLoop();
		if (length > n) {
			return false;
		}
		if (!loop && start + length > n) {
			return false;
		}
		TrackSample first = samples.get(start);
		for (int k = 0; k < length; k++) {
			int idx = index(start, k, n, loop);
			if (used[idx] || mappedEdgeBroken(spline, idx)) {
				return false;
			}
			if (k > 0 && !collinear(first, samples.get(idx))) {
				return false;
			}
			if (k > 1 && !collinear(samples.get(index(start, k - 1, n, loop)), samples.get(idx))) {
				return false;
			}
		}
		return true;
	}

	private static boolean collinear(TrackSample a, TrackSample b) {
		return Math.abs(ConvertedAngle.shortestDelta(a.yaw, b.yaw)) <= COLLINEAR_DEGREES
				&& Math.abs(a.pitch - b.pitch) <= COLLINEAR_DEGREES;
	}

	private static boolean mappedEdgeBroken(TrackSpline spline, int sampleIndex) {
		int edge = TrackChunks.edgeIndexForSample(sampleIndex, spline.getSamples().size(), spline.isLoop());
		return spline.segment(edge).broken;
	}

	private static TrackVisual piece(TrackSpline spline, int start, int length) {
		List<TrackSample> samples = spline.getSamples();
		int n = samples.size();
		boolean loop = spline.isLoop();
		double x = 0;
		double y = 0;
		double z = 0;
		Set<Integer> edges = new LinkedHashSet<>();
		for (int k = 0; k < length; k++) {
			TrackSample sample = samples.get(index(start, k, n, loop));
			x += sample.x;
			y += sample.y;
			z += sample.z;
			edges.add(TrackChunks.edgeIndexForSample(index(start, k, n, loop), n, loop));
		}
		x /= length;
		y /= length;
		z /= length;
		TrackSample first = samples.get(start);
		int fromEdge = edges.iterator().next();
		TrackVisual.Type type = length >= 3
				? TrackVisual.Type.LARGE
				: length == 2 ? TrackVisual.Type.MEDIUM : TrackVisual.Type.SMALL;
		return new TrackVisual(type, start, length, fromEdge, edges.size(), x, y, z, first.yaw, first.pitch);
	}

	private static void mark(boolean[] used, int start, int length, int n, boolean loop) {
		for (int k = 0; k < length; k++) {
			used[index(start, k, n, loop)] = true;
		}
	}

	private static int index(int start, int offset, int n, boolean loop) {
		int idx = start + offset;
		if (loop) {
			idx %= n;
			if (idx < 0) {
				idx += n;
			}
			return idx;
		}
		return idx;
	}
}
