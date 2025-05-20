package net.tfminecraft.VehicleFramework.Effects;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import com.ticxo.modelengine.api.animation.BlueprintAnimation;
import com.ticxo.modelengine.api.animation.property.SimpleProperty;
import com.ticxo.modelengine.api.model.ActiveModel;
import com.ticxo.modelengine.api.model.bone.ModelBone;
import com.ticxo.modelengine.api.model.bone.SimpleManualAnimator;

import net.tfminecraft.VehicleFramework.VehicleFramework;
import net.tfminecraft.VehicleFramework.Bones.VectorBone;
import net.tfminecraft.VehicleFramework.Enums.Component;
import net.tfminecraft.VehicleFramework.Enums.VehicleDeath;
import net.tfminecraft.VehicleFramework.Vehicles.ActiveVehicle;

public class CustomEffect {
	private List<String> commands = new ArrayList<>();
	private boolean finished;
	
	public CustomEffect(List<String> list) {
		finished = true;
		commands = list;
	}
	
	public boolean isFinished() {
		return finished;
	}
	
	public boolean isEmpty() {
		return commands.size() == 0;
	}
	
	public List<String> getCommands() {
		return commands;
	}
	
	public void play(List<Player> players, ActiveVehicle v) {
		finished = false;
		run(players, v, 0);
	}
	
	private void run(List<Player> players, ActiveVehicle v, int i) {
		if(i >= commands.size()) {
			finished = true;
			return;
		}
		ActiveModel m = v.getModel();
		String command = commands.get(i).split("\\(")[0];
		String info = commands.get(i).split("\\(")[1].replace(")", "");
		if(command.equalsIgnoreCase("delay")) {
			int time = Integer.parseInt(info);
			new BukkitRunnable() {
		        @Override
		        public void run() {
		            CustomEffect.this.run(players, v, i+1);
		        }
		    }.runTaskLater(VehicleFramework.plugin, time*1L);
		    return;
		}
		else if(command.equalsIgnoreCase("sound")) sound(m, info);
		else if(command.equalsIgnoreCase("particle")) particle(players, m, info);
		else if(command.equalsIgnoreCase("animation")) animation(m, info);
		else if(command.equalsIgnoreCase("death")) death(v, info);
		else if(command.equalsIgnoreCase("start_fire")) startFire(v, info);
		else if(command.equalsIgnoreCase("freeze_bone")) freezeBone(m, info);
		run(players, v, i+1);
	}
	
	private void particle(List<Player> players, ActiveModel m, String info) {
		Particle particle = Particle.valueOf(info.split("\\;")[0].toUpperCase());
		VectorBone bone = new VectorBone(m.getBone(info.split("\\;")[1].split("\\.")[0]).get(), m.getBone(info.split("\\;")[1].split("\\.")[1]).get());
		int amount = Integer.parseInt(info.split("\\;")[2]);
		double spread = Double.parseDouble(info.split("\\;")[3]);
		double speed = Double.parseDouble(info.split("\\;")[4]);
		for(Player p : players) {
			for (int i = 0; i < amount; i++) {
	            Vector particleDirection = bone.getVector().clone();

	            double randomX = (Math.random() - 0.5) * spread;
	            double randomY = (Math.random() - 0.5) * spread;
	            double randomZ = (Math.random() - 0.5) * spread;
	            
	            particleDirection.add(new Vector(randomX, randomY, randomZ));

	            particleDirection.normalize();

	            p.spawnParticle(
	                particle,
	                bone.getBaseLocation(),
	                0,
	                particleDirection.getX(), particleDirection.getY(), particleDirection.getZ(),
	                speed
	            );
	        }
		}
	}
	
	private void sound(ActiveModel m, String info) {
		Location loc = m.getBone(info.split("\\;")[0]).get().getLocation();
		String sound = info.split("\\;")[1];
		float volume = (float) Double.parseDouble(info.split("\\;")[2]);
		float pitch = (float) Double.parseDouble(info.split("\\;")[3]);
		loc.getWorld().playSound(loc, sound, volume, pitch);
	}
	
	private void animation(ActiveModel m, String info) {
		BlueprintAnimation anim = m.getBlueprint().getAnimations().get(info);
		if(anim == null) return;
		if(m.getAnimationHandler().isPlayingAnimation(info)) return;
		m.getAnimationHandler().playAnimation(new SimpleProperty(m, anim), true);
	}
	
	private void death(ActiveVehicle v, String death) {
		if(VehicleDeath.valueOf(death.toUpperCase()) == null) return;
		if(v.isDestroyed()) return;
		v.kill(VehicleDeath.valueOf(death.toUpperCase()));
	}
	
	private void startFire(ActiveVehicle v, String component) {
		Component c = Component.valueOf(component.toUpperCase());
		if(c == null) return;
		if(v.hasComponent(c) && !v.getComponent(c).isOnFire()) v.getComponent(c).startFire();
	}
	
	private void freezeBone(ActiveModel m, String info) {
		String b = info.split("\\;")[0];
		boolean freeze = Boolean.parseBoolean(info.split("\\;")[1]);
		if(m.getBone(b).isEmpty()) return;
		ModelBone bone = m.getBone(b).get();
		if(bone.getManualAnimator() != null && freeze) return;
		if(bone.getManualAnimator() == null && !freeze) return;
		if(freeze) bone.setManualAnimator(new SimpleManualAnimator(bone));
		else bone.setManualAnimator(null);
	}
}
