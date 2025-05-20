package net.tfminecraft.VehicleFramework.Data;

import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.List;

public class SoundData {
	private String sound;
	private float volume;
	private float pitch;
	private int delay;
	private boolean pitched;
	private boolean doppler;
	
	private float maxPitch;
	private float minPitch;
	
	public SoundData(ConfigurationSection config) {
		sound = config.getString("sound", "ENTITY_GENERIC_EXPLODE");
		volume = (float) config.getDouble("volume", 1.0);
		pitch = (float) config.getDouble("pitch", 1.0);
		maxPitch = (float) config.getDouble("max-pitch", -1.0);
		minPitch = (float) config.getDouble("min-pitch", -1.0);
		pitched = config.getBoolean("pitched", false);
		doppler = config.getBoolean("doppler", false);
		delay = config.getInt("delay", 0);
	}

	public String getSound() {
		return sound;
	}

	public float getVolume() {
		return volume;
	}

	public float getPitch() {
		return pitch;
	}
	
	public int getDelay() {
		return delay;
	}

	public boolean isPitched() {
		return pitched;
	}
	
	public boolean isDoppler() {
		return doppler;
	}
	
	public void playSound(List<Player> players, Location loc, Vector speed, float pitch) {
		if(!pitched) {
			pitch = this.pitch;
		}
		if(pitch > maxPitch && maxPitch > 0) pitch = maxPitch;
		if(pitch < minPitch && minPitch > 0) pitch = minPitch;
		for (Player player : players) {
	        Location playerLoc = player.getLocation();
	        Vector playerVelocity = player.getVelocity();

	        if (doppler) {
	            // Calculate the relative velocity along the direction of the sound
	            Vector relativePosition = loc.toVector().subtract(playerLoc.toVector()); // Source to listener
	            double distance = relativePosition.length(); // Normalize to get direction
	            relativePosition.normalize();

	            // Project the speed and player velocity onto the direction vector
	            double sourceVelocity = speed.dot(relativePosition); // Source velocity along line of sight
	            double listenerVelocity = playerVelocity.dot(relativePosition); // Listener velocity along line of sight

	            // Doppler effect calculation
	            final double speedOfSound = 343.0; // Speed of sound in m/s
	            double dopplerPitch = pitch * ((speedOfSound + listenerVelocity) / (speedOfSound + sourceVelocity));

	            // Clamp the pitch to a reasonable range to avoid unnatural sounds
	            dopplerPitch = Math.max(0.5f, Math.min(2.0f, (float) dopplerPitch));

	            // Play the sound with the Doppler-shifted pitch
	            player.playSound(loc, sound, volume, (float) dopplerPitch);
	        } else {
	            // Play the sound with the regular pitch
	            player.playSound(loc, sound, volume, pitch);
	        }
	    }
	}
	public void playSound(List<Player> players, Location loc, float pitch) {
		if(!pitched) {
			pitch = this.pitch;
		}
		if(pitch > maxPitch && maxPitch > 0) pitch = maxPitch;
		if(pitch < minPitch && minPitch > 0) pitch = minPitch;
		loc.getWorld().playSound(loc, sound, volume, pitch);
	}
	
	public void playSound(Location loc) {
		loc.getWorld().playSound(loc, sound, volume, pitch);
	}
}
