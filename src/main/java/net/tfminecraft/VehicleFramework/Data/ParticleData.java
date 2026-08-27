package net.tfminecraft.VehicleFramework.Data;

import java.util.List;

import org.bukkit.Location;
import org.bukkit.Particle;
import net.tfminecraft.VehicleFramework.VFLogger;
import net.tfminecraft.VehicleFramework.Util.ImpactVfx;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class ParticleData {
	private Particle particle;
	private int amount;
	private double spread;
	private double speed;
	
	private double x;
	private double y;
	private double z;
	
	public ParticleData(ConfigurationSection config) {
		try {
			particle = Particle.valueOf(config.getString("particle", "FLAME").toUpperCase());
		} catch (Exception e) {
			// TODO: handle exception
			particle = Particle.FLAME;
			VFLogger.log("Invalid particle type: " + config.getString("particle", "FLAME") + ". Defaulting to FLAME.");
		}
		amount = config.getInt("amount", 20);
		spread = config.getDouble("spread", 0.2);
		speed = config.getDouble("speed", 0.4);
		
		x = config.getDouble("x", 0.0);
		y = config.getDouble("y", 0.0);
		z = config.getDouble("z", 0.0);
	}

	public Particle getParticle() {
		return particle;
	}

	public int getAmount() {
		return amount;
	}

	public double getSpread() {
		return spread;
	}

	public double getSpeed() {
		return speed;
	}
	
	public void spawnParticle(Location spawnLocation, Vector vector) {
		spawnParticle(spawnLocation, vector, false);
	}

	public void spawnParticleVisible(Location spawnLocation, Vector vector) {
		spawnParticle(spawnLocation, vector, true);
	}

	private void spawnParticle(Location spawnLocation, Vector vector, boolean nearbyPlayers) {
		if (spawnLocation == null || spawnLocation.getWorld() == null) {
			return;
		}
		for (int i = 0; i < amount; i++) {
	        Vector particleDirection = vector.clone();

	        double randomX = (Math.random() - 0.5) * spread+x;
	        if(x > 0) randomX = Math.abs(randomX);
	        if(x < 0 && randomX > 0) randomX = randomX*-1;
	        double randomY = (Math.random() - 0.5) * spread+y;
	        if(y > 0) randomY = Math.abs(randomY);
	        if(y < 0 && randomY > 0) randomY = randomY*-1;
	        double randomZ = (Math.random() - 0.5) * spread+z;
	        if(z > 0) randomZ = Math.abs(randomZ);
	        if(z < 0 && randomZ > 0) randomZ = randomZ*-1;
	        particleDirection.add(new Vector(randomX, randomY, randomZ));

	        particleDirection.normalize();

			if (nearbyPlayers) {
				ImpactVfx.spawn(
						spawnLocation,
						particle,
						0,
						particleDirection.getX(), particleDirection.getY(), particleDirection.getZ(),
						speed,
						null);
			} else {
				spawnLocation.getWorld().spawnParticle(
					particle,
					spawnLocation,
					0,
					particleDirection.getX(), particleDirection.getY(), particleDirection.getZ(),
					speed,
					null,
					true
				);
			}
	    }
	}
}
