package net.tfminecraft.VehicleFramework.Data;

import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.scheduler.BukkitRunnable;

import net.tfminecraft.VehicleFramework.VehicleFramework;

public class TimedSound{
	private SoundData sound;
	
	private boolean playing = false;
	
	private int duration;
	
	public TimedSound(ConfigurationSection config) {
		sound = new SoundData(config);
		duration = config.getInt("duration", 20);
	}
	
	public TimedSound(TimedSound another) {
		sound = another.getSound();
		duration = another.getDuration();
		playing = false;
	}

	public SoundData getSound() {
		return sound;
	}

	public boolean isPlaying() {
		return playing;
	}

	public int getDuration() {
		return duration;
	}
	
	public void playSound(Location loc) {
		if(playing) return;
		sound.playSound(loc);
		playing = true;
		new BukkitRunnable()
		{
			public void run() {
				playing = false;
			}
		}.runTaskLater(VehicleFramework.plugin, duration*1L);
	}
	
	
}
