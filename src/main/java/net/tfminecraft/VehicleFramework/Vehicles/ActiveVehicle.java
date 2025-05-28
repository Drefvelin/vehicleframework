package net.tfminecraft.VehicleFramework.Vehicles;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;

import com.ticxo.modelengine.api.ModelEngineAPI;
import com.ticxo.modelengine.api.model.ActiveModel;
import com.ticxo.modelengine.api.model.ModeledEntity;

import net.Indyuce.mmoitems.api.interaction.weapon.Weapon;
import net.tfminecraft.VehicleFramework.VFLogger;
import net.tfminecraft.VehicleFramework.Bones.ConvertedAngle;
import net.tfminecraft.VehicleFramework.Cache.Cache;
import net.tfminecraft.VehicleFramework.Data.DeathData;
import net.tfminecraft.VehicleFramework.Data.DeathOverride;
import net.tfminecraft.VehicleFramework.Database.IncompleteVehicle;
import net.tfminecraft.VehicleFramework.Database.IncompleteWeapon;
import net.tfminecraft.VehicleFramework.Effects.CustomEffect;
import net.tfminecraft.VehicleFramework.Enums.Animation;
import net.tfminecraft.VehicleFramework.Enums.Component;
import net.tfminecraft.VehicleFramework.Enums.Keybind;
import net.tfminecraft.VehicleFramework.Enums.SeatType;
import net.tfminecraft.VehicleFramework.Enums.VehicleDeath;
import net.tfminecraft.VehicleFramework.Enums.CustomAction;
import net.tfminecraft.VehicleFramework.Managers.VehicleManager;
import net.tfminecraft.VehicleFramework.Util.ConditionChecker;
import net.tfminecraft.VehicleFramework.Vehicles.Component.Engine;
import net.tfminecraft.VehicleFramework.Vehicles.Component.SinkableHull;
import net.tfminecraft.VehicleFramework.Vehicles.Component.VehicleComponent;
import net.tfminecraft.VehicleFramework.Vehicles.Controller.ScoreboardController;
import net.tfminecraft.VehicleFramework.Vehicles.Controller.VehicleMovementController;
import net.tfminecraft.VehicleFramework.Vehicles.Handlers.BehaviourHandler;
import net.tfminecraft.VehicleFramework.Vehicles.Handlers.ComponentHandler;
import net.tfminecraft.VehicleFramework.Vehicles.Handlers.DeathHandler;
import net.tfminecraft.VehicleFramework.Vehicles.Handlers.EffectHandler;
import net.tfminecraft.VehicleFramework.Vehicles.Handlers.SeatHandler;
import net.tfminecraft.VehicleFramework.Vehicles.Handlers.SkinHandler;
import net.tfminecraft.VehicleFramework.Vehicles.Handlers.StateHandler;
import net.tfminecraft.VehicleFramework.Vehicles.Handlers.TowHandler;
import net.tfminecraft.VehicleFramework.Vehicles.Handlers.TrainHandler;
import net.tfminecraft.VehicleFramework.Vehicles.Handlers.UtilityHandler;
import net.tfminecraft.VehicleFramework.Vehicles.Handlers.WeaponHandler;
import net.tfminecraft.VehicleFramework.Vehicles.Handlers.State.AnimationHandler;
import net.tfminecraft.VehicleFramework.Vehicles.Seat.Seat;
import net.tfminecraft.VehicleFramework.Vehicles.State.VehicleState;
import net.tfminecraft.VehicleFramework.Vehicles.Util.AccessPanel;

public class ActiveVehicle {
	protected long spawnTime;
	
	protected VehicleManager vehicleManager;
	
	protected ActiveVehicle parent;
	
	protected String id;
	protected String uuid;
	protected String name;
	
	protected boolean fixed;
	
	protected boolean towable;
	
	protected ActiveModel model;
	protected Entity entity;
	
	//Booleans
	protected boolean initialized;
	protected boolean destroyed;
	
	//Data
	protected List<DeathData> deathData = new ArrayList<>();
	protected AccessPanel panel = new AccessPanel();
	
	//Components
	protected ComponentHandler componentHandler;
		
	//Seats
	protected SeatHandler seatHandler;
	
	//States
	protected StateHandler stateHandler;
	
	//Weapons
	protected WeaponHandler weaponHandler;
	
	//Effects
	protected EffectHandler effectHandler;
	
	//Behaviour
	protected BehaviourHandler behaviourHandler;
	
	//Death
	protected DeathHandler deathHandler;
	
	//Scoreboard
	protected ScoreboardController sb;
	
	//Skins
	protected SkinHandler skinHandler;
	
	//Towing
	protected TowHandler towHandler;
	
	//Utilities
	protected UtilityHandler utilityHandler;
	
	protected List<Player> nearby = new ArrayList<>();
	
	public ActiveVehicle(Vehicle stored, Entity e, ActiveModel m, VehicleManager manager, IncompleteVehicle i) {
		spawnTime = System.currentTimeMillis();
		vehicleManager = manager;
		skinHandler = new SkinHandler(this, stored.getModel(), stored.getSkinHandler());
		entity = e;
		model = m;
		fixed = stored.isFixed();
		towable = stored.isTowable();
		id = stored.getId();
		if(i != null) {
			name = i.getName();
			uuid = i.getId();
		} else {
			name = stored.getName();
			uuid = UUID.randomUUID().toString();
		}
		destroyed = false;
		initialized = false;
		
		//handlers
		behaviourHandler = new BehaviourHandler(this, e, m, stored.getBehaviourHandler());
		stateHandler = new StateHandler(this, stored.getStateHandler());
		componentHandler = new ComponentHandler(this, entity, model, i, stored.getComponentHandler());
		seatHandler = new SeatHandler(this, model, stored.getSeatHandler());
		weaponHandler = new WeaponHandler(model, this, stored.getWeapons(), seatHandler);
		effectHandler = new EffectHandler(stored.getEffectHandler());
		deathHandler = new DeathHandler(this);
		if(stored.getTowHandler() != null) towHandler = new TowHandler(this, stored.getTowHandler());
		if(stored.getUtilityHandler() != null) utilityHandler = new UtilityHandler(m, stored.getUtilityHandler());
		
		deathData = stored.getDeathData();
		sb = new ScoreboardController(this);
		if(i != null) {
			initialize(i.getWeapons());
		} else {
			initialize(null);
		}
	}

	//Getters and some Setters, low level stuff basically
	public Location getLocation() {
		return entity.getLocation();
	}
	
	public long getSpawnTime() {
		return spawnTime;
	}
	
	public VehicleManager getVehicleManager() {
		return vehicleManager;
	}
	
	public ActiveModel getModel() {
		return model;
	}

	public Entity getEntity() {
		return entity;
	}
	
	public boolean hasDeathData(VehicleDeath type) {
		for(DeathData d : deathData) {
			if(d.getType().equals(type)) return true;
		}
		return false;
	}
	public DeathData getDeathData(VehicleDeath type) {
		for(DeathData d : deathData) {
			if(d.getType().equals(type)) return d;
		}
		return null;
	}
	
	public boolean isFixed() {
		return fixed;
	}
	
	public boolean isTowable() {
		return towable;
	}
	
	public AccessPanel getAccessPanel() {
		return panel;
	}
	
	public boolean hasParent() {
		return parent != null;
	}
	public ActiveVehicle getParent() {
		return parent;
	}
	public void setParent(ActiveVehicle v) {
		if(this.equals(v)) return;
		parent = v;
	}
	
	public String getId() {
		return id;
	}
	public String getUUID() {
		return uuid;
	}
	
	public String getName() {
		return name;
	}
	
	
	public List<Player> getNearbyPlayers(){
		return nearby;
	}
	
	//Handlers
	public ComponentHandler getComponentHandler() {
		return componentHandler;
	}

	public SeatHandler getSeatHandler() {
		return seatHandler;
	}
	
	public StateHandler getStateHandler() {
		return stateHandler;
	}
	
	public EffectHandler getEffectHandler() {
		return effectHandler;
	}
	
	public BehaviourHandler getBehaviourHandler() {
		return behaviourHandler;
	}
	
	public DeathHandler getDeathHandler() {
		return deathHandler;
	}
	
	public SkinHandler getSkinHandler() {
		return skinHandler;
	}
	
	public boolean hasTowHandler() {
		return towHandler != null;
	}
	public TowHandler getTowHandler() {
		return towHandler;
	}
	
	public boolean hasUtilityHandler() {
		return utilityHandler != null;
	}
	public UtilityHandler getUtilityHandler() {
		return utilityHandler;
	}

	public WeaponHandler getWeaponHandler() {
		return weaponHandler;
	}
	
	//Model
	public boolean changeSkin(String id) {
		if(!skinHandler.canChangeSkin(id)) return false;
		String skinId = skinHandler.changeSkin(id);
		ModeledEntity modeledEntity = ModelEngineAPI.getModeledEntity(entity);
		model.destroy();
		modeledEntity.removeModel(model.getBlueprint().getName());
		ActiveModel m = ModelEngineAPI.createActiveModel(skinId);
		modeledEntity.addModel(m, true);
		m.getMountManager().get().setCanRide(true);
		model = m;
		behaviourHandler.updateModel(m);
		stateHandler.updateModel(m);
		componentHandler.updateModel(m);
		seatHandler.updateModel(m);
		weaponHandler.updateModel(m);
		return true;
	}
	
	//Spawn
	public boolean isInitialized() {
		return initialized;
	}
	public void initialize(List<IncompleteWeapon> incWeapons) {
		stateHandler.tick();
		updateNearby();
		getAnimationHandler().animate(Animation.DEFAULT);
		entity.setRotation(entity.getLocation().getYaw(), 0);
		initialized = true;
	}
	
	//Death
	public boolean isDestroyed() {
		return destroyed;
	}
	public void forceDestroy() {
		destroyed = true;
	}
	public void kill(VehicleDeath type) {
		if(!hasDeathData(type)) {
			VFLogger.log(name+" has no "+type.toString()+" data");
			return;
		}
		DeathData data = getDeathData(type);
		if(data.hasOverrides()) {
			for(DeathOverride o : data.getOverrides()) {
				if(!ConditionChecker.checkConditions(this, o.getConditions())) continue;
				kill(o.getDeath());
				return;
			}
		}
		destroyed = true;
		dismountAll();
		switch(type) {
		case CRASH:
			deathHandler.crash();
			break;
		case EXPLODE:
			deathHandler.explode(true);
			break;
		case SINK:
			deathHandler.sink();
			break;
		default:
			break;
		
		}
	}
	
	public void remove() {
		seatHandler.dismountAll();
		vehicleManager.unregister(entity);
		entity.remove();
	}
	//Tick
	public void slowTick() {
		for(VehicleComponent c : componentHandler.getComponents()) {
			c.slowTick(nearby);
			if(c instanceof SinkableHull) {
				SinkableHull sh = (SinkableHull) c;
				if(sh.hasSinkProgress() && sh.getSinkProgress() >= 100) kill(VehicleDeath.SINK);
			}
		}
		componentHandler.fireSpread();
		seatHandler.slowTick();
		updateNearby();
		updateBoard();
	}
	
	public void tick() {
		behaviourHandler.tick(this);
		stateHandler.tick();
		if(hasTowHandler()) towHandler.tick();
		if(hasUtilityHandler()) {
			utilityHandler.tick(nearby);
		}
		for(VehicleComponent c : componentHandler.getComponents()) {
			if(c.isFatal() && c.getHealthData().getHealthPercentage() == 0 && !destroyed) {
				kill(VehicleDeath.EXPLODE);
			}
			if(c.isOnFire() && !destroyed) {
				if(c.getFire().getProgress() == 100) {
					kill(VehicleDeath.EXPLODE);
				}
			}
			c.tick(nearby);
		}
		weaponHandler.tick();
	}
	
	
	public void updateNearby() {
		nearby.clear();
		for(Player p : Bukkit.getOnlinePlayers()) {
			if(p.getWorld() != entity.getWorld()) continue;
			if(p.getLocation().distanceSquared(entity.getLocation()) < Cache.despawnDistance) {
				nearby.add(p);
			}
		}
	}
	
	
	public void updateBoard() {
		for(Entity e : seatHandler.getPassengers()) {
			if(!(e instanceof Player)) continue;
			Player p = (Player) e;
			sb.scoreboard(p);
		}
	}
	public void removeBoard(Player p) {
		sb.removeScoreboard(p);
	}
	//Damage
	public void damage(String cause, double a) {
		if(cause.equalsIgnoreCase("suffocation")) return;
		weaponHandler.damage(cause, a);
		componentHandler.damage(cause, a);
		updateBoard();
	}
	
	public void randomFire() {
		componentHandler.randomFire();
	}
	
	public boolean isOnFire() {
		for(VehicleComponent c : componentHandler.getComponents()) {
			if(c.isOnFire()) return true;
		}
		return false;
	}
	//Input
	public void key(Player p, Keybind key) {
		stateHandler.key(p, key);
		weaponHandler.input(nearby, key, p);
		updateBoard();
	}
	
	public boolean shouldAutoMove() {
		return hasComponent(Component.ENGINE);
	}
	
	
	//Lower Level Getters (to avoid long lines of code)
	//Components
	public List<VehicleComponent> getComponents(){
		return componentHandler.getComponents();
	}
	public List<VehicleComponent> getComponents(Component c) {
		return componentHandler.getComponents(c);
	}
	public VehicleComponent getComponent(Component c) {
		return componentHandler.getComponent(c);
	}
	public boolean hasComponent(Component c) {
		return componentHandler.hasComponent(c);
	}
	public double getSpeed() {
		if(getComponent(Component.ENGINE) != null) {
			Engine engine = (Engine) getComponent(Component.ENGINE);
			return engine.getSpeed();
		}
		return 0.0;
	}
	//Animations
	public void animate(Animation a) {
		getAnimationHandler().animate(a);
	}
	public void stopAnimation(Animation a) {
		getAnimationHandler().stop(a);
	}
	//Behaviour
	public boolean shouldFloat() {
		return getBehaviourHandler().shouldFloat();
	}
	public boolean isTrain() {
		return getBehaviourHandler().isTrain();
	}
	public TrainHandler getTrainHandler() {
		return getBehaviourHandler().getTrainHandler();
	}
	//Seats
	public boolean isPassenger(Entity e, boolean checkAll) {
		if(!checkAll) return seatHandler.isPassenger(e);
		for(Seat s : panel.getSeats()) {
			if(!s.isOccupied()) continue;
			if(s.getEntity().equals(e)) return true;
		}
		return false;
	}
	public boolean isPassenger(Entity e, SeatType type) {
		for(Seat s : panel.getSeats()) {
			if(!s.isOccupied()) continue;
			if(!s.getType().equals(type)) continue;
			if(s.getEntity().equals(e)) return true;
		}
		return false;
	}
	public Seat getSeat(String seat) {
		return seatHandler.getSeat(seat);
	}
	public Seat getSeat(Entity e) {
		return seatHandler.getSeat(e);
	}
	public void dismountPassenger(Entity e, boolean change) {
		seatHandler.dismountPassenger(e, change);
	}
	public void addPassenger(Entity e, Seat s) {
		seatHandler.addPassenger(e, s);
		updateBoard();
	}
	public void changeSeat(Entity e, Seat s) {
		if(seatHandler.getSeat(e) == null) return; 
		seatHandler.changeSeat(e, s);
	}
	public void dismountAll() {
		seatHandler.dismountAll();
		for(Seat s : panel.getSeats()) {
			if(!s.isOccupied()) continue;
			s.dismount();
		}
	}
	//States
	public AnimationHandler getAnimationHandler() {
		return stateHandler.getAnimationHandler();
	}
	
	public VehicleMovementController getMoveControls() {
		return stateHandler.getMoveControls();
	}
	
	public VehicleState getCurrentState() {
		return stateHandler.getCurrentState();
	}
	
	//Effects
	public boolean hasEffect(CustomAction a) {
		return effectHandler.hasEffect(a);
	}
	public CustomEffect playEffect(CustomAction a) {
		return effectHandler.playEffect(nearby, this, a);
	}
	
	//Utilities
	public void toggleLights(Player p) {
		if(!hasUtilityHandler()) return;
		utilityHandler.toggleLights(p);
	}
	
	public void honk() {
		if(!hasUtilityHandler()) return;
		for(Player p : nearby) {
			p.sendMessage("honk");
		}
		utilityHandler.honk(entity.getLocation());
	}
	
	//Parameters
	public double getParameterValue(String parameter) {
		Vector velocity = entity.getVelocity();
		Quaternionf rotations = new Quaternionf(behaviourHandler.getRotator().getAngles());
		ConvertedAngle angles = new ConvertedAngle(rotations);
		
		if(parameter.equalsIgnoreCase("vX")) return velocity.getX();
		else if(parameter.equalsIgnoreCase("vY")) return velocity.getY();
		else if(parameter.equalsIgnoreCase("vZ")) return velocity.getZ();
		else if(parameter.equalsIgnoreCase("yaw")) return angles.getYaw();
		else if(parameter.equalsIgnoreCase("pitch")) return angles.getPitch();
		else if(parameter.equalsIgnoreCase("roll")) return angles.getRoll();
		
		return 0.0;
	}
}
