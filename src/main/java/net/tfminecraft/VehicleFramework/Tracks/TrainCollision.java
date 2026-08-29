package net.tfminecraft.VehicleFramework.Tracks;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.bukkit.entity.Entity;
import org.bukkit.util.BoundingBox;

import net.tfminecraft.VehicleFramework.Database.PersistenceLog;
import net.tfminecraft.VehicleFramework.Enums.VehicleDeath;
import net.tfminecraft.VehicleFramework.Vehicles.ActiveVehicle;

public final class TrainCollision {
	private TrainCollision() {
	}

	public static void tick(Collection<ActiveVehicle> vehicles) {
		if (vehicles == null || vehicles.isEmpty()) {
			return;
		}
		List<ActiveVehicle> list = new ArrayList<>(vehicles);
		Set<String> exploding = new HashSet<>();
		for (ActiveVehicle loco : list) {
			if (!isBoundHead(loco) || exploding.contains(key(loco))) {
				continue;
			}
			for (ActiveVehicle other : list) {
				if (other == loco || exploding.contains(key(other))) {
					continue;
				}
				if (!other.isTrain() || other.isDestroyed()) {
					continue;
				}
				if (isBoundHead(other) && key(loco).compareToIgnoreCase(key(other)) >= 0) {
					continue;
				}
				if (!sameSpline(loco, other) || sameConsist(loco, other)) {
					continue;
				}
				if (!overlaps(loco, other)) {
					continue;
				}
				explodePair(loco, other);
				exploding.add(key(loco));
				exploding.add(key(other));
				break;
			}
		}
	}

	public static boolean sameConsist(ActiveVehicle a, ActiveVehicle b) {
		if (a == null || b == null) {
			return false;
		}
		String ha = consistKey(a);
		String hb = consistKey(b);
		return ha != null && ha.equalsIgnoreCase(hb);
	}

	public static String consistKey(ActiveVehicle v) {
		if (v == null) {
			return null;
		}
		ActiveVehicle cur = v;
		int guard = 0;
		while (cur.hasParent() && guard++ < 64) {
			cur = cur.getParent();
		}
		if (cur.isTrain()) {
			String pending = cur.getTrainHandler().getPendingParent();
			if (pending != null && !pending.isBlank()) {
				return pending;
			}
		}
		return cur.getUUID();
	}

	static String consistHead(String uuid, String liveParent, String pendingParent) {
		if (liveParent != null && !liveParent.isBlank()) {
			return liveParent;
		}
		if (pendingParent != null && !pendingParent.isBlank()) {
			return pendingParent;
		}
		return uuid;
	}

	static boolean sameConsistIds(
			String aUuid, String aLiveParent, String aPendingParent,
			String bUuid, String bLiveParent, String bPendingParent) {
		String a = consistHead(aUuid, aLiveParent, aPendingParent);
		String b = consistHead(bUuid, bLiveParent, bPendingParent);
		return a != null && a.equalsIgnoreCase(b);
	}

	private static boolean isBoundHead(ActiveVehicle v) {
		return v != null
				&& v.isTrain()
				&& !v.isDestroyed()
				&& !v.hasParent()
				&& v.getTrainHandler().isBound()
				&& v.getEntity() != null
				&& !v.getEntity().isDead();
	}

	private static boolean sameSpline(ActiveVehicle a, ActiveVehicle b) {
		UUID sa = a.getTrainHandler().getSplineId();
		UUID sb = b.getTrainHandler().getSplineId();
		return sa != null && sa.equals(sb);
	}

	private static boolean overlaps(ActiveVehicle a, ActiveVehicle b) {
		Entity ea = a.getEntity();
		Entity eb = b.getEntity();
		if (ea == null || eb == null || ea.getWorld() == null || eb.getWorld() == null) {
			return false;
		}
		if (!ea.getWorld().equals(eb.getWorld())) {
			return false;
		}
		BoundingBox ba = ea.getBoundingBox();
		BoundingBox bb = eb.getBoundingBox();
		return ba != null && bb != null && ba.overlaps(bb);
	}

	private static void explodePair(ActiveVehicle a, ActiveVehicle b) {
		PersistenceLog.append("COLLIDE a=" + a.getUUID()
				+ " b=" + b.getUUID()
				+ " spline=" + a.getTrainHandler().getSplineId()
				+ " sA=" + a.getTrainHandler().getS()
				+ " sB=" + b.getTrainHandler().getS());
		explode(a);
		explode(b);
	}

	private static void explode(ActiveVehicle v) {
		if (v == null || v.isDestroyed()) {
			return;
		}
		if (!v.hasDeathData(VehicleDeath.EXPLODE)) {
			return;
		}
		v.kill(VehicleDeath.EXPLODE);
	}

	private static String key(ActiveVehicle v) {
		return v.getUUID() == null ? "" : v.getUUID();
	}
}
