package net.tfminecraft.VehicleFramework.Vehicles.Component;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import com.ticxo.modelengine.api.model.ActiveModel;
import com.ticxo.modelengine.api.model.bone.manager.MountManager;
import com.ticxo.modelengine.api.mount.controller.MountControllerTypes;

import net.tfminecraft.VehicleFramework.VFLogger;
import net.tfminecraft.VehicleFramework.Bones.ConvertedAngle;
import net.tfminecraft.VehicleFramework.Database.IncompleteComponent;
import net.tfminecraft.VehicleFramework.Enums.Component;
import net.tfminecraft.VehicleFramework.Enums.SeatType;
import net.tfminecraft.VehicleFramework.Vehicles.ActiveVehicle;
import net.tfminecraft.VehicleFramework.Vehicles.Seat.Seat;
import net.tfminecraft.VehicleFramework.Vehicles.Util.AccessPanel;

public class Harness extends VehicleComponent{
	private List<Seat> mounts = new ArrayList<>();
	
	private double turnRate;
	
	private double speed = 0;
	
	public Harness(ConfigurationSection config) {
		super(Component.HARNESS, config);
		turnRate = config.getDouble("turn-rate", 0.1);
		if(!config.contains("mount-bones")) {
			VFLogger.log("Harness has no mount bones");
		} else {
			for(String s : config.getStringList("mount-bones")) {
				mounts.add(new Seat(SeatType.HARNESS, s));
			}
		}
		
		
	}
	public Harness(Harness another, ActiveVehicle v, ActiveModel m, IncompleteComponent ic) {
		super(another, v, m, ic);
		turnRate = another.getTurnRate();
		for(Seat s : another.getMounts()) {
			mounts.add(new Seat(v, s));
		}
	}

	@Override
	public void slowTick(List<Player> nearby) {
		super.slowTick(nearby);
	}
	
	@Override
	public void tick(List<Player> nearby) {
	    super.tick(nearby);
	    AccessPanel p = v.getAccessPanel();
	    p.setSpeed(getSpeed());
	    p.setTurnRate(getTurnRate());
	    Vector align = v.getBehaviourHandler().getVector().getVector();
	    
	    for (Seat s : mounts) {
	        if (!s.isOccupied()) continue;
	        
	        Entity e = s.getEntity();
	        if(e.isDead()) dismount(s);
	        Location loc = v.getModel().getBone(s.getBone()).get().getLocation().clone();

	        // Convert vector direction to yaw
	        float yaw = (float) Math.toDegrees(Math.atan2(-align.getX(), align.getZ()));

	        // Apply yaw alignment
	        loc.setYaw(yaw);
	        e.teleport(loc);
	    }
	}
	
	public void dismount(Seat s) {
		Entity e = s.getEntity();
		s.dismount();
		e.getWorld().dropItem(e.getLocation(), new ItemStack(Material.LEAD, 1));
		if (e instanceof AbstractHorse) {
            AbstractHorse horse = (AbstractHorse) e;
            double horseSpeed = horse.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).getBaseValue();
            speed -= horseSpeed; // Adjust the speed field accordingly
            if(speed < 0) speed = 0;
        }
	}
	
	public void mount(Player p, Entity e) {
		boolean mounted = false;
		for(Seat s : mounts) {
			if(s.isOccupied()) continue;
			mounted = true;
			s.mount(e);
			if (e instanceof AbstractHorse) {
	            AbstractHorse horse = (AbstractHorse) e;
	            double horseSpeed = horse.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).getBaseValue();
	            speed += horseSpeed; // Adjust the speed field accordingly
	        }
			break;
		}
		if(!mounted) {
			p.sendMessage("§cNo free slots to mount the entity");
		}
	}
	
	public List<Seat> getMounts() {
		return mounts;
	}
	
	public double getTurnRate() {
		if(v == null) return turnRate;
		if(!v.getBehaviourHandler().turnScale()) return turnRate;
		Entity e = v.getEntity();
		if(e.getVelocity().length() < 0.08) return 0;
		double calculatedTurnRate = turnRate*e.getVelocity().length();
		if(calculatedTurnRate > turnRate) return turnRate;
		return calculatedTurnRate;
	}
	
	public double getSpeed() {
		return speed/2;
	}
	
	public boolean hasMounts() {
		for(Seat s : mounts) {
			if(s.isOccupied()) return true;
		}
		return false;
	}
	
	public List<LivingEntity> getMountedEntities(){
		List<LivingEntity> list = new ArrayList<>();
		for(Seat s : mounts) {
			if(!s.isOccupied()) continue;
			if(s.getEntity() instanceof LivingEntity) list.add((LivingEntity) s.getEntity());
		}
		return list;
	}
	
	public boolean dismount(Player p) {
		for(Seat s : mounts) {
			if(!s.isOccupied()) continue;
			Entity e = s.getEntity();
			dismount(s);
			e.teleport(p);
			return true;
		}
		return false;
	}
	
}
