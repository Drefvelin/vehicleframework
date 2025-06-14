package net.tfminecraft.VehicleFramework.Vehicles.Component;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import com.ticxo.modelengine.api.model.ActiveModel;

import net.tfminecraft.VehicleFramework.VehicleFramework;
import net.tfminecraft.VehicleFramework.Bones.VectorBone;
import net.tfminecraft.VehicleFramework.Data.ParticleData;
import net.tfminecraft.VehicleFramework.Data.SoundData;
import net.tfminecraft.VehicleFramework.Database.IncompleteComponent;
import net.tfminecraft.VehicleFramework.Effects.CustomEffect;
import net.tfminecraft.VehicleFramework.Enums.Animation;
import net.tfminecraft.VehicleFramework.Enums.Component;
import net.tfminecraft.VehicleFramework.Enums.CustomAction;
import net.tfminecraft.VehicleFramework.Enums.Input;
import net.tfminecraft.VehicleFramework.Enums.State;
import net.tfminecraft.VehicleFramework.Util.ParticleLoader;
import net.tfminecraft.VehicleFramework.Util.SoundLoader;
import net.tfminecraft.VehicleFramework.Vehicles.ActiveVehicle;
import net.tfminecraft.VehicleFramework.Vehicles.Component.Fuel.FuelTank;
import net.tfminecraft.VehicleFramework.Vehicles.Component.Propulsion.Throttle;
import net.tfminecraft.VehicleFramework.Vehicles.Util.AccessPanel;

public class Engine extends VehicleComponent{
	
	private boolean checkingStop = false;
	
	private Throttle throttle;
	
	//Engine start
	private boolean requireStart;
	private boolean started;
	private boolean starting;
	
	private List<VectorBone> bones = new ArrayList<>();
	private Entity entity;
	
	private double speed;
	private double turnrate;
	
	private List<SoundData> sounds = new ArrayList<>();
	private List<ParticleData> particles = new ArrayList<>();
	private List<String> boneList = new ArrayList<>();

	private FuelTank tank;
	
	@SuppressWarnings("unchecked")
	public Engine(ConfigurationSection config) {
		super(Component.ENGINE, config);
		requireStart = config.getBoolean("requires-start", false);
		throttle = new Throttle(config.getInt("max"), config.getInt("min"), this);
		speed = config.getDouble("speed");
		turnrate = config.getDouble("turn-rate");
		if(config.isConfigurationSection("sounds")) {
			ConfigurationSection soundConfig = config.getConfigurationSection("sounds");
			sounds = SoundLoader.getSoundsFromConfig(soundConfig);
		}
		if(config.isConfigurationSection("particles")) {
			ConfigurationSection particleConfig = config.getConfigurationSection("particles");
			particles = ParticleLoader.getParticlesFromConfig(particleConfig);
		}
		boneList = (List<String>) config.getList("particle-bones", new ArrayList<String>());
		tank = new FuelTank(config);
	}

	public Engine(ActiveVehicle v, Engine another, Entity e, ActiveModel m, IncompleteComponent ic) {
		super(another, v, m, ic);
		entity = e;
		requireStart = another.requiresStart();
		started = !requireStart;
		starting = false;
		throttle = new Throttle(another.getThrottle().getMax(), another.getThrottle().getMin(), this);
		speed = another.getBaseSpeed();
		turnrate = another.getBaseTurnRate();
		sounds = another.getSounds();
		particles = another.getParticles();
		boneList = another.getParticleBones();
		for(String bone : boneList) {
			String base = bone.split("\\.")[0];
			String align = bone.split("\\.")[1];
			bones.add(new VectorBone(m.getBone(base).get(), m.getBone(align).get()));
		}
		tank = new FuelTank(another.getFuelTank());
	}	
	
	@Override
	public void updateModel(ActiveModel m) {
		super.updateModel(m);
		for(VectorBone bone : bones) {
			bone.updateModel(m);
		}
	}

	public void setStarted(boolean b) {
		started = b;
		starting = false;
	}

	public FuelTank getFuelTank() {
		return tank;
	}

	public List<String> getParticleBones() {
		return boneList;
	}
	public List<SoundData> getSounds() {
		return sounds;
	}
	public List<ParticleData> getParticles() {
		return particles;
	}
	
	public boolean requiresStart() {
		return requireStart;
	}
	public boolean isStarted() {
		return started;
	}

	public Throttle getThrottle() {
		return throttle;
	}

	public double getBaseSpeed() {
		return speed;
	}
	
	public double getBaseTurnRate() {
		return turnrate;
	}
	
	public double getSpeed() {
		return speed*(throttle.getCurrent()/100.0);
	}
	
	public double getTurnRate() {
		return turnrate*(throttle.getCurrent()/100.0);
	}
	
	private void normalize() {
		if(!v.getSeatHandler().hasPassengers() && v.getStateHandler().getCurrentState().getType() != State.FLYING) {
			throttle.normalize();
    		if(throttle.getCurrent() == 0) {
    			stop();
    		}
    	}
		if(!checkingStop && started) {
    		checkingStop = true;
    		new BukkitRunnable() {
    	        @Override
    	        public void run() {
    	            if(throttle.getCurrent() == 0) {
    	            	stop();
    	            }
    	            checkingStop = false;
    	        }
    	    }.runTaskLater(VehicleFramework.plugin, 40L);
    	}
	}
	
	public void playSound(List<Player> players) {
		Location loc = entity.getLocation();
		if(throttle.getCurrent() == 0) return;
		float pitch = (float) (0.5f+(throttle.getCurrent()/100.0));
		if(pitch < 0.5f) {
			pitch = 0.5f;
		}
		if(pitch > 1.6f) {
			pitch = 1.6f;
		}
		for(SoundData sound : sounds) {
			sound.playSound(players, loc, entity.getVelocity(), pitch);
		}	
	}
	@Override
	public void slowTick(List<Player> nearby) {
		super.slowTick(nearby);
		if(started) tank.tick(throttle);
		playSound(nearby);
	}
	
	@Override
	public void tick(List<Player> nearby) {
		super.tick(nearby);
		v.getMoveControls().input(null, Input.MOVE);
		int current = throttle.getCurrent();
		AccessPanel panel = v.getAccessPanel();
		panel.setSpeed(getSpeed());
		panel.setTurnRate(getTurnRate());
		boolean reverse = false;
		if(current < 0) {
			current = current*-1;
			reverse = true;
		}
		if(current > healthData.getHealthPercentage()) {
			current = healthData.getHealthPercentage();
			if(reverse) {
				current = current*-1;
			}
			throttle.setThrottle(current);
		}
		panel.setReverse(reverse);
		normalize();
		if(throttle.getCurrent() == 0) {
			return;
		}
		v.animate(Animation.ENGINE_ACTIVE);
		for(VectorBone bone : bones) {
			for(ParticleData pd : particles) {
				pd.spawnParticle(bone.getBaseLocation(), bone.getVector());
			}
		}
		if(healthData.getHealthPercentage() < 1 && started) stop();
		if(tank.getCurrent() == 0 && started && tank.useFuel()) {
			stop();
		}
	}
	
	public void start(Player p) {
		if(starting) return;
		if(cooldown.containsKey(p)) {
			if(cooldown.get(p) > System.currentTimeMillis()) return;
		}
		cooldown.put(p, System.currentTimeMillis()+1000);
		if(tank.getCurrent() == 0 && tank.useFuel()) {
			p.sendMessage("§e["+alias+"] §cNo fuel!");
			return;
		}
		starting = true;
		p.sendMessage("§e["+alias+"] starting...");
		if(Math.random()*100 > healthData.getHealthPercentage()) {
			if(!v.hasEffect(CustomAction.ENGINE_START_FAIL)) defaultFail(p);
			else {
				CustomEffect effect = v.playEffect(CustomAction.ENGINE_START_FAIL);
				new BukkitRunnable() {
			        @Override
			        public void run() {
			            if(effect.isFinished()) {
			            	defaultFail(p);
			            	cancel();
			            }
			        }
			    }.runTaskTimer(VehicleFramework.plugin, 0L, 1L);
			}
			return;
		}
		if(!v.hasEffect(CustomAction.ENGINE_START)) defaultStart(p);
		else {
			CustomEffect effect = v.playEffect(CustomAction.ENGINE_START);
			new BukkitRunnable() {
		        @Override
		        public void run() {
		            if(effect.isFinished()) {
		            	defaultStart(p);
		            	cancel();
		            }
		        }
		    }.runTaskTimer(VehicleFramework.plugin, 0L, 1L);
		}
	}
	private void defaultStart(Player p) {
		p.sendMessage("§e["+alias+"] §astarted!");
		started = true;
		starting = false;
		throttle.increase();
		v.animate(Animation.ENGINE_ACTIVE);
	}
	private void defaultFail(Player p) {
		p.sendMessage("§e["+alias+"] §cfailed!");
		starting = false;
	}
	public void stop() {
		if(!requireStart) return;
		started = false;
		throttle.setThrottle(0);
		v.stopAnimation(Animation.ENGINE_ACTIVE);
	}
}
