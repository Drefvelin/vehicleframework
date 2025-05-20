package net.tfminecraft.VehicleFramework.Weapons.Ammunition.Data;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import net.tfminecraft.VehicleFramework.Data.ParticleData;
import net.tfminecraft.VehicleFramework.Data.SoundData;
import net.tfminecraft.VehicleFramework.Util.ParticleLoader;
import net.tfminecraft.VehicleFramework.Util.SoundLoader;
import net.tfminecraft.VehicleFramework.Weapons.Ammunition.Data.Projectile.ItemModel;
import net.tfminecraft.VehicleFramework.Weapons.Ammunition.Data.Projectile.MEGModel;
import net.tfminecraft.VehicleFramework.Weapons.Ammunition.Data.Projectile.ProjectileModel;

public class AmmunitionData {
	
	private String input;
	
	private ProjectileModel model;
	
	private float yield;
	
	private int radius;
	private int damage;
	
	private int rounds;
	
	private String damageType;
	
	private List<SoundData> sfx = new ArrayList<>();
	private List<ParticleData> vfx = new ArrayList<>();
	
	public AmmunitionData(ConfigurationSection config) {
		input = config.getString("input", "none");
		yield = (float) config.getDouble("yield", 1.0);
		radius = config.getInt("radius", 5);
		damage = config.getInt("damage", 8);
		rounds = config.getInt("rounds", 1);
		damageType = config.getString("damage-type", "PROJECTILE").toUpperCase();
		if(config.isConfigurationSection("sounds")) {
			ConfigurationSection soundConfig = config.getConfigurationSection("sounds");
			sfx = SoundLoader.getSoundsFromConfig(soundConfig);
		}
		if(config.isConfigurationSection("particles")) {
			ConfigurationSection particleConfig = config.getConfigurationSection("particles");
			vfx = ParticleLoader.getParticlesFromConfig(particleConfig);
		}
		if(config.isConfigurationSection("model")) {
			String modelType = config.getConfigurationSection("model").getString("type", "item");
			if(modelType.equalsIgnoreCase("item")) {
				model = new ItemModel(config.getConfigurationSection("model"));
			} else if(modelType.equalsIgnoreCase("meg")) {
				model = new MEGModel(config.getConfigurationSection("model"));
			}
		}
	}
	
	public void fx(List<Player> players, Location loc, float pitch, int i) {
		for(SoundData sd : sfx) {
			if(sd.getDelay() > i) continue;
			sd.playSound(players, loc, pitch);
		}
		for(ParticleData pd : vfx) {
			pd.spawnParticle(loc, new Vector(0, 0, 0));
		}
	}

	public String getInput() {
		return input;
	}

	public ProjectileModel getModel() {
		return model;
	}

	public float getYield() {
		return yield;
	}

	public int getRadius() {
		return radius;
	}

	public int getDamage() {
		return damage;
	}

	public int getRounds() {
		return rounds;
	}
	
	public String getDamageType() {
		return damageType;
	}
	
	public double getOffset() {
		return model.getOffset();
	}
	
	public Entity spawn(Location loc) {
		return model.spawn(loc);
	}
	
}
