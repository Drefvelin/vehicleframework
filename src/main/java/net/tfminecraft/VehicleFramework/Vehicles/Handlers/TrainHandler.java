package net.tfminecraft.VehicleFramework.Vehicles.Handlers;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import com.ticxo.modelengine.api.model.ActiveModel;

import net.tfminecraft.VehicleFramework.Bones.BoneRotator;
import net.tfminecraft.VehicleFramework.Bones.ConvertedAngle;
import net.tfminecraft.VehicleFramework.Cache.Cache;
import net.tfminecraft.VehicleFramework.Database.ConsistData;
import net.tfminecraft.VehicleFramework.Database.PersistenceLog;
import net.tfminecraft.VehicleFramework.Enums.Direction;
import net.tfminecraft.VehicleFramework.Tracks.ThrottleTape;
import net.tfminecraft.VehicleFramework.Tracks.ThrottleTapeItems;
import net.tfminecraft.VehicleFramework.Tracks.TrackAdvance;
import net.tfminecraft.VehicleFramework.Tracks.TrackFx;
import net.tfminecraft.VehicleFramework.Tracks.TrackJunction;
import net.tfminecraft.VehicleFramework.Tracks.TrackJunctionTravel;
import net.tfminecraft.VehicleFramework.Tracks.TrackSplineMotion;
import net.tfminecraft.VehicleFramework.Tracks.TrackTools;
import net.tfminecraft.VehicleFramework.Tracks.TrackConsistMath;
import net.tfminecraft.VehicleFramework.Tracks.TrackLap;
import net.tfminecraft.VehicleFramework.Tracks.TrackPose;
import net.tfminecraft.VehicleFramework.Tracks.TrackRegistry;
import net.tfminecraft.VehicleFramework.Tracks.TrackSpline;
import net.tfminecraft.VehicleFramework.Tracks.RecorderLog;
import net.tfminecraft.VehicleFramework.VehicleFramework;
import net.tfminecraft.VehicleFramework.Vehicles.ActiveVehicle;
import net.tfminecraft.VehicleFramework.Vehicles.Component.Fuel.FuelTank;
import net.tfminecraft.VehicleFramework.Vehicles.Handlers.Container.Container;
import net.tfminecraft.VehicleFramework.Vehicles.Handlers.Train.Connector;

public class TrainHandler {
	protected ActiveVehicle v;
	protected ActiveVehicle child;
	
	private Connector front;
	private Connector back;
	private String pendingParent;
	private String pendingChild;
	private UUID splineId;
	private double s;
	private int travelSign = 1;
	private UUID armedJunctionId;
	private TrackJunction.Side armedSide;
	private long lastNoFrogChatMs;
	private UUID routeJunctionId;
	private boolean takeBranch;
	private final List<String> fuelCars = new ArrayList<>();
	private ThrottleTape installedTape;
	private ThrottleTape recordingTape;
	private UUID recordingPlayer;
	private boolean recording;
	private double recordPrevS;
	private UUID recordPrevSpline;
	private double recordTraveled;
	private double recordLength;
	private double fxTraveled;
	private final ThrottleTape.DwellState tapeDwell = new ThrottleTape.DwellState();
	
	public TrainHandler(ConfigurationSection config) {
		if(config.contains("front-connector")) {
			front = new Connector(config.getString("front-connector"));
		}
		if(config.contains("back-connector")) {
			back = new Connector(config.getString("back-connector"));
		}
		if (config.contains("fuel-cars")) {
			for (String id : config.getStringList("fuel-cars")) {
				if (id != null && !id.isBlank()) {
					fuelCars.add(id);
				}
			}
		}
	}
	
	public TrainHandler(ActiveVehicle v, TrainHandler another) {
		this.v = v;
		if(another.isAttachable()) {
			front = new Connector(v, another.getFront());
		}
		if(another.canHaveAttached()) {
			back = new Connector(v, another.getBack());
		}
		travelSign = another.travelSign;
		armedJunctionId = another.armedJunctionId;
		armedSide = another.armedSide;
		routeJunctionId = another.routeJunctionId;
		takeBranch = another.takeBranch;
		fuelCars.addAll(another.fuelCars);
		if (another.installedTape != null) {
			installedTape = ThrottleTape.fromJson(another.installedTape.toJson());
		}
	}
	
	public void updateModel(ActiveModel m) {
		if(isAttachable()) {
			front.updateModel(m);
		}
		if(canHaveAttached()) {
			back.updateModel(m);
		}
	}
	
	public boolean attach(Player p, ActiveVehicle target) {
		if(!isAttachable()) {
			p.sendMessage("§cThis train car cannot be attached to anything");
			return false;
		}
		if(v.hasParent()) {
			p.sendMessage("§cThis vehicle is already attached to something!");
			return false;
		}
		if(!target.getBehaviourHandler().isTrain()) {
			p.sendMessage("§cTarget vehicle is not a train type");
			return false;
		}
		TrainHandler handler = target.getBehaviourHandler().getTrainHandler();
		if(!handler.canHaveAttached()) {
			p.sendMessage("§cTarget train cannot have any cars attached");
			return false;
		}
		if(target.getTrainHandler().hasChild() || target.getTrainHandler().getPendingChild() != null) {
			p.sendMessage("§cTarget train already has a car attached");
			return false;
		}
		target.getTrainHandler().setChild(v);
		v.setParent(target);
		p.sendMessage("§aConnected car to train");
		p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, 1f, 1f);
		ActiveVehicle loco = locoOf(target);
		if (loco.getTrainHandler().isBound()) {
			loco.getTrainHandler().placeLoadedCars();
		} else {
			Location loc = getOffsetPosition(target);
			v.getEntity().teleport(loc);
		}
		return true;
	}
	
	public boolean hasChild() {
		return child != null;
	}
	public ActiveVehicle getChild() {
		return child;
	}
	public void setChild(ActiveVehicle v) {
		if (v != null && this.v != null && this.v.equals(v)) {
			return;
		}
		child = v;
		if (v != null) {
			pendingChild = null;
		}
	}

	public String getPendingParent() {
		return pendingParent;
	}

	public void setPendingParent(String uuid) {
		pendingParent = blankToNull(uuid);
	}

	public String getPendingChild() {
		return pendingChild;
	}

	public void setPendingChild(String uuid) {
		pendingChild = blankToNull(uuid);
	}

	public UUID getSplineId() {
		return splineId;
	}

	public void setSplineId(UUID splineId) {
		this.splineId = splineId;
	}

	public double getS() {
		return s;
	}

	public void setS(double s) {
		this.s = s;
	}

	public void applyConsist(ConsistData consist) {
		if (consist == null) {
			return;
		}
		pendingParent = consist.getParent();
		pendingChild = consist.getChild();
		if (consist.getSplineId() != null) {
			try {
				splineId = UUID.fromString(consist.getSplineId());
			} catch (IllegalArgumentException e) {
				splineId = null;
			}
		} else {
			splineId = null;
		}
		s = consist.getS() == null ? 0 : consist.getS();
		travelSign = consist.getTravelSign();
		routeJunctionId = null;
		takeBranch = consist.isDiverge();
		if (consist.getJunctionId() != null) {
			try {
				routeJunctionId = UUID.fromString(consist.getJunctionId());
			} catch (IllegalArgumentException e) {
				routeJunctionId = null;
				takeBranch = false;
			}
		}
		PersistenceLog.append("APPLY_CONSIST " + PersistenceLog.vehicle(v));
	}

	public ConsistData toConsistData() {
		String childId = hasChild() ? child.getUUID() : pendingChild;
		String parentId = v != null && v.hasParent() ? v.getParent().getUUID() : pendingParent;
		String spline = splineId == null ? null : splineId.toString();
		Double arc = splineId == null ? null : s;
		boolean loco = pendingParent == null && (v == null || !v.hasParent());
		String junction = loco && routeJunctionId != null ? routeJunctionId.toString() : null;
		Boolean diverge = junction == null ? null : takeBranch;
		return new ConsistData(parentId, childId, spline, arc, splineId == null ? null : travelSign, junction, diverge);
	}

	public void holdJunction(TrackJunction.Side side) {
		if (side == null || v == null || v.hasParent()) {
			return;
		}
		if (v.getAccessPanel() != null && v.getAccessPanel().getSpeed() < 0) {
			clearArm();
			tellCaptain("Junction arm cleared: reverse");
			RecorderLog.arm(v, "clear-reverse", side, null, 0);
			return;
		}
		TrackSpline spline = boundSpline();
		if (spline == null) {
			tellCaptain("Junction: not on a track");
			return;
		}
		TrackRegistry registry = VehicleFramework.getTrackRegistry();
		if (registry == null) {
			return;
		}
		if (armedJunctionId != null) {
			TrackJunction armed = registry.getJunction(armedJunctionId).orElse(null);
			if (armed != null && armed.stemSplineId.equals(splineId)
					&& TrackJunctionTravel.facing(travelSign, armed.facingSign)) {
				double ahead = TrackJunctionTravel.ahead(
						s, armed.s, travelSign, spline.isLoop(), spline.length());
				boolean stillBefore = ahead >= -1e-9
						&& ahead <= Cache.trackJunctionArmDistance + 1e-9
						&& (!spline.isLoop() || ahead <= spline.length() * 0.5 + 1e-9);
				if (stillBefore) {
					throwSwitch(registry, armed, side, ahead);
					return;
				}
			}
			clearArm();
		}
		TrackJunction next = nextFrogInWindow(registry, spline);
		if (next == null) {
			TrackJunction far = nextFrogAhead(registry, spline, Double.POSITIVE_INFINITY);
			long now = System.currentTimeMillis();
			if (now - lastNoFrogChatMs >= 1500) {
				lastNoFrogChatMs = now;
				if (far == null) {
					tellCaptain("Junction: no turnout ahead on this track");
					RecorderLog.arm(v, "no-frog", side, null, 0);
				} else {
					double ahead = TrackJunctionTravel.ahead(
							s, far.s, travelSign, spline.isLoop(), spline.length());
					tellCaptain("Junction: press A/D within "
							+ (int) Cache.trackJunctionArmDistance
							+ " of the frog. Next is "
							+ far.side.name().toLowerCase()
							+ " at s="
							+ String.format(java.util.Locale.US, "%.0f", far.s)
							+ " ("
							+ String.format(java.util.Locale.US, "%.0f", ahead)
							+ " ahead)");
					RecorderLog.arm(v, "too-far", side, far, ahead);
				}
			}
			return;
		}
		double ahead = TrackJunctionTravel.ahead(
				s, next.s, travelSign, spline.isLoop(), spline.length());
		throwSwitch(registry, next, side, ahead);
	}

	private void throwSwitch(TrackRegistry registry, TrackJunction frog, TrackJunction.Side side, double ahead) {
		boolean first = armedJunctionId == null || !armedJunctionId.equals(frog.id);
		boolean diverge = frog.side == side;
		boolean changed = registry.setThrown(frog.id, diverge);
		armedJunctionId = frog.id;
		armedSide = side;
		if (!first && !changed) {
			return;
		}
		TrackJunction live = registry.getJunction(frog.id).orElse(frog);
		tellArm(live, ahead, diverge ? "diverge" : "through");
	}

	private void tellArm(TrackJunction frog, double ahead, String status) {
		RecorderLog.arm(v, status, armedSide, frog, ahead);
		tellCaptain("Switch: "
				+ (frog.thrown ? "diverge" : "through")
				+ " ("
				+ status
				+ "), "
				+ String.format(java.util.Locale.US, "%.0f", ahead)
				+ " ahead");
	}

	private void tellCaptain(String message) {
		if (v == null || v.getSeatHandler() == null || message == null) {
			return;
		}
		Player captain = v.getSeatHandler().captainPlayer();
		if (captain != null) {
			captain.sendMessage("§e" + message);
		}
	}

	private TrackJunction nextFrogInWindow(TrackRegistry registry, TrackSpline spline) {
		return nextFrogAhead(registry, spline, Cache.trackJunctionArmDistance);
	}

	private TrackJunction nextFrogAhead(TrackRegistry registry, TrackSpline spline, double maxAhead) {
		TrackJunction best = null;
		double bestAhead = Double.POSITIVE_INFINITY;
		for (TrackJunction junction : registry.junctionsOn(splineId)) {
			if (junction.branchSplineId == null) {
				continue;
			}
			if (!TrackJunctionTravel.facing(travelSign, junction.facingSign)) {
				continue;
			}
			double ahead = TrackJunctionTravel.ahead(
					s, junction.s, travelSign, spline.isLoop(), spline.length());
			if (ahead < -1e-9 || ahead > maxAhead) {
				continue;
			}
			if (ahead < bestAhead) {
				bestAhead = ahead;
				best = junction;
			}
		}
		return best;
	}

	private void clearArm() {
		armedJunctionId = null;
		armedSide = null;
	}

	private static String blankToNull(String uuid) {
		if (uuid == null || uuid.isBlank()) {
			return null;
		}
		return uuid;
	}
	
	public int getTravelSign() {
		return travelSign;
	}

	public boolean hasInstalledTape() {
		return installedTape != null && !installedTape.isEmpty();
	}

	public ThrottleTape getInstalledTape() {
		return installedTape;
	}

	public void setInstalledTape(ThrottleTape tape) {
		installedTape = tape;
		tapeDwell.left = 0;
		tapeDwell.atS = null;
		clearRecording();
		RecorderLog.append("TAPE_INSTALL samples=" + (tape == null || tape.isEmpty() ? 0 : tape.getSamples().size())
				+ " " + RecorderLog.train(v));
	}

	public boolean isRecording() {
		return recording;
	}

	public int recordingSampleCount() {
		return recordingTape == null ? 0 : recordingTape.getSamples().size();
	}

	public boolean canRecordCircuit() {
		TrackSpline spline = boundSpline();
		return spline != null && spline.isLoop();
	}

	public void startRecording(Player player) {
		TrackSpline spline = boundSpline();
		if (spline == null || !spline.isLoop()) {
			RecorderLog.append("RECORD_START_FAIL not-circuit " + RecorderLog.train(v));
			return;
		}
		recording = true;
		recordingTape = new ThrottleTape(splineId.toString());
		recordingPlayer = player == null ? null : player.getUniqueId();
		recordPrevS = s;
		recordPrevSpline = splineId;
		recordTraveled = 0;
		recordLength = spline.length();
		int throttle = v != null && v.getThrottle() != null ? v.getThrottle().getCurrent() : 0;
		recordingTape.tryAppend(s, travelSign, throttle, splineId.toString(), null);
		RecorderLog.append("RECORD_START player=" + (player == null ? "none" : player.getName())
				+ " length=" + recordLength + " " + RecorderLog.train(v));
	}

	public void stopRecording(ItemStack hand) {
		RecorderLog.append("RECORD_STOP samples=" + recordingSampleCount() + " traveled=" + recordTraveled
				+ " " + RecorderLog.train(v));
		if (recordingTape != null && hand != null) {
			ThrottleTapeItems.write(hand, recordingTape);
		}
		clearRecording();
	}

	public void maybeRecordSample(int throttle) {
		if (!recording || recordingTape == null) {
			return;
		}
		TrackSpline spline = boundSpline();
		TrackRegistry registry = VehicleFramework.getTrackRegistry();
		if (spline == null || registry == null || !onRecordedCircuit(registry, spline)) {
			finishRecordingCappedOrUnbound("Recording stopped: locomotive left the circuit");
			return;
		}
		boolean onOrigin = recordingTape.matchesSpline(spline.getId());
		if (onOrigin && recordPrevSpline != null && recordPrevSpline.equals(spline.getId())) {
			recordTraveled += TrackLap.wrapDelta(recordPrevS, s, recordLength);
		}
		recordPrevS = s;
		recordPrevSpline = spline.getId();
		String junction = null;
		if (!onOrigin && routeJunctionId != null) {
			junction = routeJunctionId.toString();
		}
		ThrottleTape.AppendResult result = recordingTape.tryAppend(
				s, travelSign, throttle, spline.getId().toString(), junction);
		int hold = recordingTape.getSamples().isEmpty()
				? 0
				: recordingTape.getSamples().get(recordingTape.getSamples().size() - 1).holdTicks;
		RecorderLog.sample(v, result, throttle, hold);
		if (result == ThrottleTape.AppendResult.CAPPED) {
			finishRecordingCappedOrUnbound("Recording stopped: tape is full");
			return;
		}
		if (TrackLap.complete(recordTraveled, recordLength)) {
			finishRecordingCappedOrUnbound("Recording complete");
		}
	}

	private boolean onRecordedCircuit(TrackRegistry registry, TrackSpline spline) {
		if (recordingTape == null || spline == null) {
			return false;
		}
		if (recordingTape.matchesSpline(spline.getId())) {
			return true;
		}
		TrackJunction branch = registry.junctionByBranch(spline.getId()).orElse(null);
		if (branch == null) {
			return false;
		}
		return recordingTape.matchesSpline(branch.stemSplineId);
	}

	private void clearRecording() {
		recording = false;
		recordingTape = null;
		recordingPlayer = null;
		recordPrevS = 0;
		recordPrevSpline = null;
		recordTraveled = 0;
		recordLength = 0;
	}

	private void finishRecordingCappedOrUnbound(String message) {
		RecorderLog.append("RECORD_FINISH " + message + " samples=" + recordingSampleCount()
				+ " traveled=" + recordTraveled + " " + RecorderLog.train(v));
		Player player = recordingPlayer == null ? null : Bukkit.getPlayer(recordingPlayer);
		if (player != null && player.isOnline()) {
			ItemStack hand = player.getInventory().getItemInMainHand();
			if (TrackTools.isRecorder(hand) && recordingTape != null) {
				ThrottleTapeItems.write(hand, recordingTape);
			} else if (recordingTape != null && !recordingTape.isEmpty()) {
				installedTape = recordingTape;
			}
			player.sendMessage("§e" + message);
		} else if (recordingTape != null && !recordingTape.isEmpty()) {
			installedTape = recordingTape;
		}
		clearRecording();
	}

	public Integer playbackThrottle(FuelTank tank) {
		if (recording || !isBound() || !hasInstalledTape()) {
			return null;
		}
		if (tank != null && tank.useFuel() && tank.getCurrent() <= 0) {
			RecorderLog.playback(v, "no-fuel", null, tapeDwell);
			return null;
		}
		if (v != null && v.getSeatHandler() != null && v.getSeatHandler().hasCaptain()) {
			RecorderLog.playback(v, "captain", null, tapeDwell);
			return null;
		}
		int target = installedTape.targetWithDwell(s, travelSign, tapeDwell, splineId);
		RecorderLog.playback(v, "ok", target, tapeDwell);
		return target;
	}

	public boolean isBound() {
		return splineId != null;
	}

	public void unbind() {
		PersistenceLog.append("UNBIND " + PersistenceLog.vehicle(v));
		setConsistGravity(true);
		splineId = null;
		s = 0;
		travelSign = 1;
		clearArm();
		routeJunctionId = null;
		takeBranch = false;
		ActiveVehicle car = v;
		while (car != null && car.getTrainHandler().hasChild()) {
			car = car.getTrainHandler().getChild();
			car.getTrainHandler().splineId = null;
			car.getTrainHandler().s = 0;
			car.getTrainHandler().travelSign = 1;
		}
	}

	public boolean bind(TrackSpline spline) {
		if (spline == null || v == null || v.getEntity() == null) {
			return false;
		}
		Location loc = v.getEntity().getLocation();
		splineId = spline.getId();
		s = spline.nearestS(loc.getX(), loc.getY(), loc.getZ());
		travelSign = facingSign(loc, spline.sampleAt(s));
		applyPose(v, spline.sampleAt(s));
		placeLoadedCars();
		PersistenceLog.append("BIND " + PersistenceLog.vehicle(v));
		return true;
	}

	public boolean isAttachable() {
		return front != null;
	}
	
	public Connector getFront() {
		return front;
	}
	
	public boolean canHaveAttached() {
		return back != null;
	}
	
	public Connector getBack() {
		return back;
	}

	public boolean acceptsFuelCar(ActiveVehicle car) {
		return car != null && childIdAllowed(fuelCars, car.getId());
	}

	static boolean childIdAllowed(List<String> fuelCars, String childId) {
		if (fuelCars == null || fuelCars.isEmpty() || childId == null || childId.isBlank()) {
			return false;
		}
		for (String id : fuelCars) {
			if (id != null && id.equalsIgnoreCase(childId)) {
				return true;
			}
		}
		return false;
	}

	public void drainFromChild(FuelTank tank) {
		if (v == null || v.hasParent() || !hasChild() || tank == null || !tank.useFuel() || !tank.hasInput()) {
			return;
		}
		if (tank.getCurrent() >= tank.getCapacity()) {
			return;
		}
		if (!acceptsFuelCar(child) || !child.hasContainers()) {
			return;
		}
		String path = tank.getInput().getItem();
		int amount = tank.getInput().getAmount();
		for (Container c : child.getContainerHandler().getContainers().values()) {
			if (c.takeOneMatching(path) == null) {
				continue;
			}
			tank.addFuel(amount);
			return;
		}
	}

	static boolean shouldDrain(List<String> fuelCars, String childId, boolean tankHasSpace, boolean matchingItemTaken) {
		return childIdAllowed(fuelCars, childId) && tankHasSpace && matchingItemTaken;
	}
	
	public void clear() {
		if(hasChild()) child.getTrainHandler().clear();
	}
	
	public Location getOffsetPosition(ActiveVehicle parent) {
		Location loc = parent.getEntity().getLocation().clone();
		loc.add(parent.getTrainHandler().getBack().getOffset());
		loc.add(v.getTrainHandler().getFront().getOffset().multiply(-1));
		return loc;
	}

	public void animateMove(Direction dir) {
		v.getMoveControls().animateMove(dir);
    	if(hasChild()) child.getTrainHandler().animateMove(dir);
	}
	
	public void splineTick() {
		if (v.hasParent()) {
			return;
		}
		double speed = v.getAccessPanel() == null ? 0 : v.getAccessPanel().getSpeed();
		if (speed < 0) {
			clearArm();
		}
		if (TrackSplineMotion.stopped(speed)) {
			animateMove(Direction.STILL);
			if (keepBound()) {
				placeLoadedCars();
			} else {
				still();
			}
			return;
		}
		boolean reverse = v.getAccessPanel() != null && v.getAccessPanel().isReverse();
		animateMove(reverse ? Direction.BACKWARD : Direction.FORWARD);
		if (!tryBindOrKeep()) {
			still();
			return;
		}
		travelSign = speed < 0 ? -1 : 1;
		splineStep(speed);
	}

	public void placeLoadedCars() {
		TrackSpline spline = boundSpline();
		if (spline == null) {
			return;
		}
		PersistenceLog.placeCars(v);
		applyPose(v, spline.sampleAt(s));
		TrackRegistry registry = VehicleFramework.getTrackRegistry();
		if (registry == null) {
			return;
		}
		TrackJunction route = routeJunction();
		UUID stemId = route == null ? null : route.stemSplineId;
		UUID branchId = route == null ? null : route.branchSplineId;
		double junctionS = route == null ? 0 : route.s;
		int facingSign = route == null ? 1 : route.facingSign;
		TrackSpline stem = stemId == null ? null : registry.get(stemId).orElse(null);
		TrackSpline branch = branchId == null ? null : registry.get(branchId).orElse(null);
		double stemLength = stem == null ? spline.length() : stem.length();
		boolean stemLoop = stem != null ? stem.isLoop() : spline.isLoop();
		double branchLength = branch == null ? 0 : branch.length();
		UUID parentSpline = splineId;
		double parentS = s;
		int parentTravelSign = travelSign;
		ActiveVehicle parentCar = v;
		ActiveVehicle car = child;
		while (car != null) {
			TrainHandler parentTrain = parentCar.getTrainHandler();
			TrainHandler carTrain = car.getTrainHandler();
			double gap = spacing(parentTrain, carTrain);
			TrackJunctionTravel.Pose pose = TrackJunctionTravel.rewind(
					parentSpline,
					parentS,
					parentTravelSign,
					gap,
					takeBranch && route != null,
					stemId == null ? splineId : stemId,
					branchId,
					junctionS,
					facingSign,
					stemLength,
					stemLoop,
					branchLength);
			carTrain.splineId = pose.splineId;
			carTrain.s = pose.s;
			if (branchId != null && branchId.equals(pose.splineId)) {
				carTrain.travelSign = 1;
			} else {
				carTrain.travelSign = travelSign;
				if (takeBranch && route != null && splineId != null && splineId.equals(branchId)) {
					carTrain.travelSign = facingSign;
				}
			}
			TrackSpline carSpline = pose.splineId == null ? null : registry.get(pose.splineId).orElse(null);
			if (carSpline != null) {
				applyPose(car, carSpline.sampleAt(pose.s));
			}
			parentSpline = pose.splineId;
			parentS = pose.s;
			parentCar = car;
			parentTravelSign = carTrain.travelSign;
			car = carTrain.child;
		}
	}

	private boolean keepBound() {
		if (splineId == null) {
			return false;
		}
		if (boundSpline() == null) {
			unbind();
			return false;
		}
		return true;
	}

	private boolean tryBindOrKeep() {
		if (keepBound()) {
			return true;
		}
		if (v == null || v.getEntity() == null || v.getEntity().getWorld() == null) {
			return false;
		}
		TrackRegistry registry = VehicleFramework.getTrackRegistry();
		if (registry == null) {
			return false;
		}
		Location loc = v.getEntity().getLocation();
		return registry.nearest(loc.getWorld().getName(), loc.getX(), loc.getY(), loc.getZ(), Cache.trackSnapDistance)
				.map(this::bind)
				.orElse(false);
	}

	private void splineStep(double ds) {
		TrackSpline spline = boundSpline();
		if (spline == null) {
			unbind();
			still();
			return;
		}
		double from = s;
		TrackAdvance advance = spline.advance(s, ds);
		TrackRegistry registry = VehicleFramework.getTrackRegistry();
		if (registry != null && applyJunctionStep(registry, spline, from, advance.s, ds)) {
			maybeClack(ds);
			placeLoadedCars();
			if (advance.stoppedAtBreak) {
				still();
			}
			return;
		}
		s = advance.s;
		applyPose(v, spline.sampleAt(s));
		maybeClack(ds);
		placeLoadedCars();
		if (advance.stoppedAtBreak) {
			still();
		}
	}

	private boolean applyJunctionStep(
			TrackRegistry registry,
			TrackSpline spline,
			double from,
			double to,
			double ds) {
		TrackJunction asBranch = registry.junctionByBranch(splineId).orElse(null);
		if (asBranch != null && !spline.isLoop() && ds < 0 && to <= 1e-6) {
			TrackSpline stem = registry.get(asBranch.stemSplineId).orElse(null);
			if (stem == null) {
				return false;
			}
			splineId = stem.getId();
			s = asBranch.s;
			travelSign = -asBranch.facingSign;
			routeJunctionId = asBranch.id;
			takeBranch = true;
			applyPose(v, stem.sampleAt(s));
			return true;
		}
		if (asBranch != null) {
			return false;
		}
		for (TrackJunction junction : registry.junctionsOn(splineId)) {
			if (junction.branchSplineId == null) {
				continue;
			}
			if (!TrackJunctionTravel.crosses(from, to, junction.s, travelSign, spline.isLoop(), spline.length())) {
				continue;
			}
			if (!TrackJunctionTravel.facing(travelSign, junction.facingSign)) {
				continue;
			}
			TrackJunction live = registry.getJunction(junction.id).orElse(junction);
			boolean diverge = live.thrown;
			String reason = diverge ? "switch-diverge" : "switch-through";
			if (armedJunctionId != null && armedJunctionId.equals(junction.id)) {
				clearArm();
			}
			routeJunctionId = junction.id;
			takeBranch = diverge;
			String detail = reason
					+ " thrown="
				+ live.thrown
				+ " frog=" + live.side.name()
					+ " facing=" + junction.facingSign
					+ " travel=" + travelSign;
			if (RecorderLog.throttle("junc:" + v.getUUID() + ":" + junction.id, 2000)) {
				RecorderLog.junction(v, diverge, junction.id, detail);
				if (diverge) {
					tellCaptain("Junction: diverge (" + reason.replace('-', ' ') + ")");
				} else {
					tellCaptain("Junction: through (" + reason.replace('-', ' ') + ")");
				}
			}
			if (!diverge) {
				continue;
			}
			TrackSpline branch = registry.get(junction.branchSplineId).orElse(null);
			if (branch == null) {
				takeBranch = false;
				continue;
			}
			splineId = branch.getId();
			s = 0;
			travelSign = 1;
			applyPose(v, branch.sampleAt(0));
			return true;
		}
		return false;
	}

	private TrackJunction routeJunction() {
		TrackRegistry registry = VehicleFramework.getTrackRegistry();
		if (registry == null) {
			return null;
		}
		if (routeJunctionId != null) {
			TrackJunction stored = registry.getJunction(routeJunctionId).orElse(null);
			if (stored != null) {
				return stored;
			}
		}
		if (splineId == null) {
			return null;
		}
		return registry.junctionByBranch(splineId).orElse(null);
	}

	private void maybeClack(double ds) {
		fxTraveled += Math.abs(ds);
		if (fxTraveled < Cache.trackFxSoundInterval) {
			return;
		}
		fxTraveled = 0;
		TrackSpline spline = boundSpline();
		if (spline == null || v == null || v.getEntity() == null || v.getEntity().getWorld() == null) {
			return;
		}
		TrackFx.clack(v.getEntity().getWorld(), spline.sampleAt(s));
	}

	private TrackSpline boundSpline() {
		if (splineId == null) {
			return null;
		}
		TrackRegistry registry = VehicleFramework.getTrackRegistry();
		if (registry == null) {
			return null;
		}
		return registry.get(splineId).orElse(null);
	}

	private void applyPose(ActiveVehicle vehicle, TrackPose pose) {
		if (vehicle == null || vehicle.getEntity() == null || pose == null) {
			return;
		}
		Location loc = vehicle.getEntity().getLocation();
		Location next = loc.clone();
		next.setX(pose.x);
		next.setY(pose.y + Cache.trackVehicleYOffset);
		next.setZ(pose.z);
		Vector move = new Vector(
				next.getX() - loc.getX(),
				next.getY() - loc.getY(),
				next.getZ() - loc.getZ());
		Vector xz = move.clone();
		xz.setY(0);
		boolean idle = xz.lengthSquared() <= TrackSplineMotion.MOVE_EPS_SQ;
		ConvertedAngle world = TrackSplineMotion.worldHeading(move, pose, 1);
		Entity entity = vehicle.getEntity();
		entity.setGravity(false);
		entity.teleport(next);
		entity.setVelocity(new Vector(0, 0, 0));
		if (vehicle.getBehaviourHandler() != null) {
			BoneRotator rotator = vehicle.getBehaviourHandler().getRotator();
			if (rotator != null) {
				float boneYaw = TrackSplineMotion.boneYaw(world.getYaw(), loc.getYaw());
				rotator.rotateToTarget(
						boneYaw,
						TrackSplineMotion.bonePitch(world.getPitch()),
						0f,
						1f,
						true,
						true,
						false);
			}
		}
		RecorderLog.pose(vehicle, loc, next, pose, idle ? null : world);
		PersistenceLog.applyPose(vehicle, "spline", loc, next);
		if (!idle && vehicle.getEntity() != null && vehicle.getEntity().getWorld() != null) {
			TrackFx.crumbs(vehicle.getEntity().getWorld(), pose);
		}
	}

	private static int facingSign(Location loc, TrackPose pose) {
		if (loc == null || pose == null) {
			return 1;
		}
		Vector look = loc.getDirection().clone();
		look.setY(0);
		if (look.lengthSquared() < 1e-8) {
			return 1;
		}
		look.normalize();
		double yawRad = Math.toRadians(pose.yaw);
		Vector tangent = new Vector(-Math.sin(yawRad), 0, Math.cos(yawRad));
		return look.dot(tangent) < -1e-4 ? -1 : 1;
	}

	private void setConsistGravity(boolean gravity) {
		ActiveVehicle car = v;
		while (car != null) {
			if (car.getEntity() != null) {
				car.getEntity().setGravity(gravity);
			}
			if (!car.getTrainHandler().hasChild()) {
				break;
			}
			car = car.getTrainHandler().getChild();
		}
	}

	private void still() {
		if (v != null && v.getEntity() != null) {
			v.getEntity().setVelocity(new Vector(0, 0, 0));
		}
	}

	private static ActiveVehicle locoOf(ActiveVehicle car) {
		ActiveVehicle loco = car;
		while (loco != null && loco.hasParent()) {
			loco = loco.getParent();
		}
		return loco;
	}

	private static double spacing(TrainHandler parent, TrainHandler child) {
		double back = 0;
		double front = 0;
		try {
			if (parent != null && parent.canHaveAttached()) {
				back = parent.getBack().getOffset().length();
			}
			if (child != null && child.isAttachable()) {
				front = child.getFront().getOffset().length();
			}
		} catch (Exception ignored) {
			return TrackConsistMath.connectorSpacing(back, front);
		}
		return TrackConsistMath.connectorSpacing(back, front);
	}
}
