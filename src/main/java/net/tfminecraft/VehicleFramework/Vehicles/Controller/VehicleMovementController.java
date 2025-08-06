package net.tfminecraft.VehicleFramework.Vehicles.Controller;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import net.tfminecraft.VehicleFramework.VehicleFramework;
import net.tfminecraft.VehicleFramework.Bones.BoneRotator;
import net.tfminecraft.VehicleFramework.Bones.VectorBone;
import net.tfminecraft.VehicleFramework.Enums.Animation;
import net.tfminecraft.VehicleFramework.Enums.Component;
import net.tfminecraft.VehicleFramework.Enums.Direction;
import net.tfminecraft.VehicleFramework.Enums.Input;
import net.tfminecraft.VehicleFramework.Enums.SeatType;
import net.tfminecraft.VehicleFramework.Enums.State;
import net.tfminecraft.VehicleFramework.Interface.MovementInterface;
import net.tfminecraft.VehicleFramework.Managers.InventoryManager;
import net.tfminecraft.VehicleFramework.Managers.VehicleManager;
import net.tfminecraft.VehicleFramework.Vehicles.ActiveVehicle;
import net.tfminecraft.VehicleFramework.Vehicles.Component.Balloon;
import net.tfminecraft.VehicleFramework.Vehicles.Component.Wings;
import net.tfminecraft.VehicleFramework.Vehicles.Handlers.TowHandler;
import net.tfminecraft.VehicleFramework.Vehicles.Seat.Seat;
import net.tfminecraft.VehicleFramework.Vehicles.State.VehicleState;

public class VehicleMovementController implements MovementInterface{
	protected VehicleState state;
	
	protected ActiveVehicle v;
	protected BoneRotator rotator;
	protected VectorBone vector;
	protected VehicleManager vehicleManager;
	//protected WeaponManager weaponManager;
	protected InventoryManager inv = new InventoryManager();
	protected Entity e;
	
	//Methods distributed to subcontrollers to avoid one massive class
	protected BaseController baseController = new BaseController();
	protected LiftController liftController = new LiftController();
	protected FloatController floatController = new FloatController();
	protected ThrottleController throttleController = new ThrottleController();
	protected RotateController rotateController = new RotateController();
	
	
	
	public VehicleMovementController(ActiveVehicle v, VehicleState state) {
		rotator = v.getBehaviourHandler().getRotator();
		vector = v.getBehaviourHandler().getVector();
		this.state = state;
		this.v = v;
		this.e = v.getEntity();
		vehicleManager = v.getVehicleManager();
	}
	public void input(Player p, Input i) {
		switch(i) {
			case THROTTLE_UP:
				throttleUp(p);
				break;
			case THROTTLE_DOWN:
				throttleDown(p);
				break;
			case TURN_LEFT:
				turnLeft(p);
				break;
			case TURN_RIGHT:
				turnRight(p);
				break;
			case TURN_LEFT_LOCAL:
				turnLeftLocal(p);
				break;
			case TURN_RIGHT_LOCAL:
				turnRightLocal(p);
				break;
			case SEAT_SELECTION:
				seatSelection(p);
				break;
			case MOVE:
				move();
				break;
			case PITCH_UP:
				pitchUp(p);
				break;
			case PITCH_DOWN:
				pitchDown(p);
				break;
			case ROLL_LEFT:
				rollLeft(p);
				break;
			case ROLL_RIGHT:
				rollRight(p);
				break;
			case FORWARD:
				forward(p);
				break;
			case BACKWARD:
				backward(p);
				break;
			case LIGHTS:
				v.toggleLights(p);
				break;
			case HORN:
				if(v.getSeat(p).getType().equals(SeatType.CAPTAIN)) v.honk(p);
				break;
			case UP:
				up(p);
				break;
			case DOWN:
				down(p);
				break;
			default:
				break;
			
		}
	}
	
	public void update(ActiveVehicle v) {
		rotator = v.getBehaviourHandler().getRotator();
		vector = v.getBehaviourHandler().getVector();
	}

	private void throttleUp(Player p) {
		throttleController.throttle(v, p, false);
	}

	private void throttleDown(Player p) {
		throttleController.throttle(v, p, true);
	}
	
	private void turnLeft(Player p) {
		rotateController.turnLeft(rotator, v, p);
	}

	private void turnRight(Player p) {
		rotateController.turnRight(rotator, v, p);
	}
	
	private void turnLeftLocal(Player p) {
		rotateController.turnLeftLocal(rotator, v, p);
	}

	private void turnRightLocal(Player p) {
		rotateController.turnRightLocal(rotator, v, p);
	}
	private void pitchUp(Player p) {
		rotateController.pitchUp(rotator, v, p, getPitchRollRate());
	}

	private void pitchDown(Player p) {
		rotateController.pitchDown(rotator, v, p, getPitchRollRate());
	}
	private void rollLeft(Player p) {
		rotateController.rollLeft(rotator, v, p, getPitchRollRate());
	}

	private void rollRight(Player p) {
		rotateController.rollRight(rotator, v, p, getPitchRollRate());
	}
	 
	private float getPitchRollRate() {
		float rate = 0;
		if(v.hasComponent(Component.WINGS)) {
			Wings wings = (Wings) v.getComponent(Component.WINGS);
			rate = wings.getTurnRate();
		}
		return rate;
	}

	private void seatSelection(Player p) {
        try {
            inv.seatSelection(null, p, v, true); // Trigger inventory action
        } catch (Exception ex) {
            p.sendMessage("An error occurred while opening inventory: " + ex.getMessage());
            ex.printStackTrace();
        }
		
	}
	
	private void forward(Player p) {
		if(!v.getSeat(p).getType().equals(SeatType.CAPTAIN)) return;
		if(baseController.getDirection(v).equals(Direction.STILL)) return;
		Vector velocity = getSimpleMovements(Direction.FORWARD);
		apply(velocity, Direction.FORWARD);
	}
	private void backward(Player p) {
		if(!v.getSeat(p).getType().equals(SeatType.CAPTAIN)) return;
		if(baseController.getDirection(v).equals(Direction.STILL)) return;
		Vector velocity = getSimpleMovements(Direction.BACKWARD);
		apply(velocity, Direction.BACKWARD);
	}

	private void move() {
		if(v.hasParent()) return;
		liftController.checkHitWall(v);
		Vector velocity = getSimpleMovements(Direction.STILL);
		if(v.shouldFloat()) velocity = floatController.calculateFloat(v, velocity);
		if(v.hasComponent(Component.WINGS) || v.hasComponent(Component.BALLOON)) velocity= liftController.calculateLift(rotator, v, velocity);
		apply(velocity, baseController.getDirection(v));
		rotator.rotateSmoothed(0, 0, 0);
	}

	private void up(Player p) {
		if(!v.getSeat(p).getType().equals(SeatType.CAPTAIN)) return;
		if (!v.hasComponent(Component.BALLOON)) return;
		Balloon balloon = (Balloon) v.getComponent(Component.BALLOON);
		double lift = balloon.getLift();
		if (lift < 0) return;
		Vector velocity = v.getEntity().getVelocity();
		velocity.setY(lift);
		v.getEntity().setVelocity(velocity);
		balloon.setDelta(lift); // Set delta instead of directly setting velocity
	}

	private void down(Player p) {
		if(!v.getSeat(p).getType().equals(SeatType.CAPTAIN)) return;
		if (!v.hasComponent(Component.BALLOON)) return;
		Balloon balloon = (Balloon) v.getComponent(Component.BALLOON);
		double lift = balloon.getBaseLift();
		if (lift < 0) return;
		Vector velocity = v.getEntity().getVelocity();
		if(velocity.getY() < -lift) return;
		velocity.setY(-lift);
		v.getEntity().setVelocity(velocity);
		balloon.setDelta(-lift); // Set delta to negative for downward force
	}
	
	private Vector getSimpleMovements(Direction dir) {
		Vector velocity = baseController.calculateMoveVector(v, vector, dir);
		velocity = baseController.climbVector(v, velocity);
		velocity = flatten(velocity);
		return velocity;
	}

	private Vector flatten(Vector velocity) {
		if(!v.hasComponent(Component.WINGS)) {
			if(v.getCurrentState().getType().equals(State.FLOATING)) velocity.setY(0);
			return velocity;
		}
		if(!v.hasComponent(Component.ENGINE)) return velocity;
		if(v.getThrottle().getCurrent() < 20) velocity.setY(0);
		return velocity;
	}
	
	public void setAnimation() {
		if(v.shouldAutoMove()) return;
		if(v.hasParent()) return;
		if(e.getVelocity().length() < 0.08) {
			animateMove(Direction.STILL);
			propagate(e.getVelocity(), Direction.STILL);
		}
	}
	
	private void apply(Vector velocity, Direction dir) {
		if(state.isDefault()) return;
		if(System.currentTimeMillis()-5000 > v.getSpawnTime()) {
			if(v.isTrain()) {
				v.getTrainHandler().retarget(velocity);
			} else {
				e.setVelocity(velocity);
			}
		} else {
			e.setVelocity(velocity);
		}
		propagate(velocity, dir);
		
		if(!v.isTrain()) animateMove(dir);
	}
	
	private void propagate(Vector velocity, Direction dir) {
		if(v.hasTowHandler()) {
			TowHandler h = v.getTowHandler();
			Seat s = h.getTowPoint();
			if(s.isOccupied()) {
				h.animate(dir);
				s.getEntity().setVelocity(velocity);
			}
		}
	}
	
	public void animateMove(Direction dir) {
		if(dir.equals(Direction.STILL)) {
			v.stopAnimation(Animation.FORWARD);
			v.stopAnimation(Animation.BACKWARD);
			return;
		}
		if(dir.equals(Direction.FORWARD)) {
			v.animate(Animation.FORWARD);
		}
		if(dir.equals(Direction.BACKWARD)) {
			v.animate(Animation.BACKWARD);
		}
	}
	
	public BaseController getBaseController() {
		return baseController;
	}
	public LiftController getLiftController() {
		return liftController;
	}
	public FloatController getFloatController() {
		return floatController;
	}
	public ThrottleController getThrottleController() {
		return throttleController;
	}
	public RotateController getRotateController() {
		return rotateController;
	}
	
	

}
