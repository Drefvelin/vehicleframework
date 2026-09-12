package net.tfminecraft.VehicleFramework.Vehicles.Handlers.Train;

import net.tfminecraft.VehicleFramework.VehicleFramework;
import net.tfminecraft.VehicleFramework.Data.VehicleRemovePayload;
import net.tfminecraft.VehicleFramework.Enums.VehicleRemoveReason;
import net.tfminecraft.VehicleFramework.Managers.VehicleManager;
import net.tfminecraft.VehicleFramework.Vehicles.ActiveVehicle;
import net.tfminecraft.VehicleFramework.Vehicles.Handlers.TrainHandler;
import net.tfminecraft.VehicleFramework.Database.PersistenceLog;

public final class ConsistRelinker {
	private ConsistRelinker() {
	}

	public static void tryLink(ActiveVehicle vehicle) {
		if (vehicle == null || !vehicle.isTrain()) {
			return;
		}
		PersistenceLog.tryLink("before", vehicle);
		VehicleManager manager = VehicleFramework.getVehicleManager();
		if (manager == null) {
			PersistenceLog.tryLink("no-manager", vehicle);
			return;
		}
		TrainHandler train = vehicle.getTrainHandler();
		String childId = train.getPendingChild();
		if (childId != null) {
			ActiveVehicle child = manager.getByUUID(childId);
			if (child == null) {
				PersistenceLog.append("TRY_LINK child-missing pending=" + childId + " uuid=" + vehicle.getUUID());
			}
			if (child != null && child.isTrain() && !child.equals(vehicle)) {
				train.setChild(child);
				child.setParent(vehicle);
				train.setPendingChild(null);
				child.getTrainHandler().setPendingParent(null);
			}
		}
		String parentId = train.getPendingParent();
		if (parentId != null) {
			ActiveVehicle parent = manager.getByUUID(parentId);
			if (parent == null) {
				PersistenceLog.append("TRY_LINK parent-missing pending=" + parentId + " uuid=" + vehicle.getUUID());
			}
			if (parent != null && parent.isTrain() && !parent.equals(vehicle)) {
				parent.getTrainHandler().setChild(vehicle);
				vehicle.setParent(parent);
				train.setPendingParent(null);
				parent.getTrainHandler().setPendingChild(null);
			}
		}
		ActiveVehicle loco = vehicle;
		while (loco != null && loco.hasParent()) {
			loco = loco.getParent();
		}
		if (loco != null && loco.isTrain() && loco.getTrainHandler().isBound()) {
			loco.getTrainHandler().placeLoadedCars();
		}
		PersistenceLog.tryLink("after", vehicle);
	}

	public static void onRemove(ActiveVehicle vehicle, VehicleRemovePayload payload) {
		if (vehicle == null || !vehicle.isTrain()) {
			return;
		}
		boolean unload = payload != null
				&& payload.getRemoveReason().orElse(null) == VehicleRemoveReason.UNLOAD;
		if (unload) {
			splitForUnload(vehicle);
		} else {
			dropLinks(vehicle);
		}
	}

	private static void splitForUnload(ActiveVehicle vehicle) {
		TrainHandler train = vehicle.getTrainHandler();
		String uuid = vehicle.getUUID();
		if (train.hasChild()) {
			ActiveVehicle child = train.getChild();
			child.getTrainHandler().setPendingParent(uuid);
			child.setParent(null);
			train.setChild(null);
		}
		if (vehicle.hasParent()) {
			ActiveVehicle parent = vehicle.getParent();
			parent.getTrainHandler().setPendingChild(uuid);
			parent.getTrainHandler().setChild(null);
			vehicle.setParent(null);
		}
	}

	private static void dropLinks(ActiveVehicle vehicle) {
		TrainHandler train = vehicle.getTrainHandler();
		String uuid = vehicle.getUUID();
		if (train.hasChild()) {
			ActiveVehicle child = train.getChild();
			if (uuid.equals(child.getTrainHandler().getPendingParent())
					|| (child.hasParent() && child.getParent().equals(vehicle))) {
				child.getTrainHandler().setPendingParent(null);
				child.setParent(null);
			}
			train.setChild(null);
		}
		if (vehicle.hasParent()) {
			ActiveVehicle parent = vehicle.getParent();
			if (uuid.equals(parent.getTrainHandler().getPendingChild())
					|| (parent.getTrainHandler().hasChild() && parent.getTrainHandler().getChild().equals(vehicle))) {
				parent.getTrainHandler().setPendingChild(null);
				parent.getTrainHandler().setChild(null);
			}
			vehicle.setParent(null);
		}
		ActiveVehicle pendingChild = loaded(train.getPendingChild());
		if (pendingChild != null && pendingChild.isTrain()) {
			if (uuid.equals(pendingChild.getTrainHandler().getPendingParent())) {
				pendingChild.getTrainHandler().setPendingParent(null);
			}
			if (pendingChild.hasParent() && pendingChild.getParent().equals(vehicle)) {
				pendingChild.setParent(null);
			}
		}
		ActiveVehicle pendingParent = loaded(train.getPendingParent());
		if (pendingParent != null && pendingParent.isTrain()) {
			if (uuid.equals(pendingParent.getTrainHandler().getPendingChild())) {
				pendingParent.getTrainHandler().setPendingChild(null);
			}
			if (pendingParent.getTrainHandler().hasChild() && pendingParent.getTrainHandler().getChild().equals(vehicle)) {
				pendingParent.getTrainHandler().setChild(null);
			}
		}
		train.setPendingChild(null);
		train.setPendingParent(null);
		train.setSplineId(null);
	}

	private static ActiveVehicle loaded(String uuid) {
		if (uuid == null) {
			return null;
		}
		VehicleManager manager = VehicleFramework.getVehicleManager();
		if (manager == null) {
			return null;
		}
		return manager.getByUUID(uuid);
	}
}
