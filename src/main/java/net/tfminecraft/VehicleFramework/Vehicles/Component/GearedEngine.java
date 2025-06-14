package net.tfminecraft.VehicleFramework.Vehicles.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import com.ticxo.modelengine.api.model.ActiveModel;

import net.tfminecraft.VehicleFramework.VFLogger;
import net.tfminecraft.VehicleFramework.VehicleFramework;
import net.tfminecraft.VehicleFramework.Bones.VectorBone;
import net.tfminecraft.VehicleFramework.Data.ParticleData;
import net.tfminecraft.VehicleFramework.Database.IncompleteComponent;
import net.tfminecraft.VehicleFramework.Effects.CustomEffect;
import net.tfminecraft.VehicleFramework.Enums.Animation;
import net.tfminecraft.VehicleFramework.Enums.Component;
import net.tfminecraft.VehicleFramework.Enums.CustomAction;
import net.tfminecraft.VehicleFramework.Enums.Input;
import net.tfminecraft.VehicleFramework.Enums.State;
import net.tfminecraft.VehicleFramework.Util.ParticleLoader;
import net.tfminecraft.VehicleFramework.Vehicles.ActiveVehicle;
import net.tfminecraft.VehicleFramework.Vehicles.Component.Fuel.FuelTank;
import net.tfminecraft.VehicleFramework.Vehicles.Component.Gear.Gear;
import net.tfminecraft.VehicleFramework.Vehicles.Component.Propulsion.Throttle;
import net.tfminecraft.VehicleFramework.Vehicles.Util.AccessPanel;

public class GearedEngine extends VehicleComponent{
	
	private boolean checkingStop = false;
	
	private boolean shifting = false;
	
	private List<Gear> gears = new ArrayList<>();
	
	private int currentGear;
	private int defaultGear;
	
	private boolean requireStart;
	private boolean started;
	private boolean starting;
		
	private List<VectorBone> bones = new ArrayList<>();
	private Entity entity;

	private double turnrate;

	private List<ParticleData> particles = new ArrayList<>();
	private List<String> boneList = new ArrayList<>();

	private FuelTank tank;
	
	@SuppressWarnings("unchecked")
	public GearedEngine(ConfigurationSection config) {
		super(Component.GEARED_ENGINE, config);
		requireStart = config.getBoolean("requires-start", false);
		turnrate = config.getDouble("turn-rate");
		if(config.isConfigurationSection("particles")) {
			ConfigurationSection particleConfig = config.getConfigurationSection("particles");
			particles = ParticleLoader.getParticlesFromConfig(particleConfig);
		}
		if(!config.isConfigurationSection("gears")) VFLogger.log("Geared engine has no gears!");
		Set<String> set = config.getConfigurationSection("gears").getKeys(false);

		List<String> list = new ArrayList<String>(set);
		
		for(String key : list) {
			gears.add(new Gear(this, config.getConfigurationSection("gears."+key)));
		}
		currentGear = config.getInt("start-gear", 0);
		defaultGear = currentGear;
		boneList = (List<String>) config.getList("particle-bones", new ArrayList<String>());
		tank = new FuelTank(config);
	}

	public GearedEngine(ActiveVehicle v, GearedEngine another, Entity e, ActiveModel m, IncompleteComponent ic) {
		super(another, v, m, ic);
		entity = e;
		requireStart = another.requiresStart();
		started = !requireStart;
		starting = false;
		turnrate = another.getTurnRate();
		particles = another.getParticles();
		boneList = another.getParticleBones();
		currentGear = another.getCurrentGear();
		defaultGear = another.getDefaultGear();
		for(String bone : boneList) {
			String base = bone.split("\\.")[0];
			String align = bone.split("\\.")[1];
			bones.add(new VectorBone(m.getBone(base).get(), m.getBone(align).get()));
		}
		for(Gear g : another.getGears()) {
			gears.add(new Gear(this, g));
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

	public List<ParticleData> getParticles() {
		return particles;
	}
	
	public boolean requiresStart() {
		return requireStart;
	}
	public boolean isStarted() {
		return started;
	}
	
	public double getTurnRate() {
		return turnrate;
	}
	
	public List<Gear> getGears(){
		return gears;
	}

	public void setCurrentGear(int i) {
		if(i >= 0 && gears.size() > i) currentGear = i;
	}
	
	public int getCurrentGear() {
		return currentGear;
	}
	
	public int getDefaultGear() {
		return defaultGear;
	}
	
	public Gear getGear() {
		return gears.get(currentGear);
	}
	
	public double getSpeed() {
		return getGear().getSpeed();
	}
	
	public void throttle(boolean down) {
		if(shifting) return;
		Gear g = getGear();
		Throttle t = g.getThrottle();
		g.getAccelerationSound().playSound(v.getEntity().getLocation());
		if(down) {
			if(t.getCurrent() == t.getMin() && currentGear > 0) {
				shift(-1);
				return;
			}
			t.change(g.getAcceleration()*-1);
		} else {
			if(t.getCurrent() == t.getMax() && currentGear < gears.size()-1) {
				shift(1);
				return;
			}
			g.getThrottle().change(g.getAcceleration());
		}
	}
	
	private void shift(int dir) {
		if(shifting) return;
		shifting = true;
		new BukkitRunnable() {
			int i = 0;
	        @Override
	        public void run() {
	        	if(i == 10) {
	        		currentGear+=dir;
	        		shifting = false;
	        		cancel();
	        	}
	        	if(getGear().getThrottle().getCurrent() > 0) getGear().getThrottle().decrease();
	            i++;
	        }
	    }.runTaskTimer(VehicleFramework.plugin, 0L, 1L);
	}
	
	//This is some shitty as fuck code i took from the other engine class why do i even have a playsound method who the fuck knows
	
	private void normalize(Gear g) {
		if(!v.getSeatHandler().hasPassengers() && v.getStateHandler().getCurrentState().getType() != State.FLYING) {
			Throttle t = g.getThrottle();
			t.normalize();
			if(currentGear == defaultGear) {
				if(g.getThrottle().getCurrent() == 0) {
	    			stop();
	    		}
			} else {
				if(!shifting) {
					if(t.getCurrent() == t.getMin() && currentGear > 0) {
						shift(-1);
					} else if(t.getCurrent() == t.getMax() && currentGear < gears.size()-1) {
						shift(1);
					}
				}
			}
    	}
		if(!checkingStop && started && currentGear == defaultGear) {
    		checkingStop = true;
    		new BukkitRunnable() {
    	        @Override
    	        public void run() {
    	            if(g.getThrottle().getCurrent() == 0 && currentGear == defaultGear) {
    	            	stop();
    	            }
    	            checkingStop = false;
    	        }
    	    }.runTaskLater(VehicleFramework.plugin, 40L);
    	}
	}
	
	public void playSound(List<Player> players) {
		Location loc = entity.getLocation();
		if(getGear().getThrottle().getCurrent() == 0) return;
		float pitch = (float) (0.5f+(getGear().getThrottle().getCurrent()/100.0));
		if(pitch < 0.5f) {
			pitch = 0.5f;
		}
		if(pitch > 1.6f) {
			pitch = 1.6f;
		}
		getGear().getEngineSound().playSound(players, loc, entity.getVelocity(), pitch);
	}
	@Override
	public void slowTick(List<Player> nearby) {
		super.slowTick(nearby);
		if(started) tank.tick(getGear().getThrottle());
		playSound(nearby);
	}
	
	@Override
	public void tick(List<Player> nearby) {
		super.tick(nearby);
		v.getMoveControls().input(null, Input.MOVE);
		Gear g = getGear();
		int current = g.getThrottle().getCurrent();
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
			getGear().getThrottle().setThrottle(current);
		}
		panel.setReverse(reverse);
		normalize(g);
		if(g.getThrottle().getCurrent() == 0) {
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
		currentGear = defaultGear;
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
		getGear().getThrottle().increase();
		v.animate(Animation.ENGINE_ACTIVE);
	}
	private void defaultFail(Player p) {
		p.sendMessage("§e["+alias+"] §cfailed!");
		starting = false;
	}
	public void stop() {
		if(!requireStart) return;
		started = false;
		getGear().getThrottle().setThrottle(0);
		setCurrentGear(defaultGear);
		getGear().getThrottle().setThrottle(0);
		v.stopAnimation(Animation.ENGINE_ACTIVE);
	}
}
