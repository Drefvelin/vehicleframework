package net.tfminecraft.VehicleFramework.Weapons.Ammunition.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
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

	private boolean explosive;
	private boolean fire;
	private List<String> potionEffects = new ArrayList<>();

	private List<SoundData> hitSFX = new ArrayList<>();
	private List<ParticleData> hitVFX = new ArrayList<>();
	
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

		explosive = config.getBoolean("explosive", config.getString("type", "CANNONBALL").equalsIgnoreCase("BULLET") ? false : true);
		fire = config.getBoolean("fire", false);
		if(config.contains("potion-effects")) potionEffects = config.getStringList("potion-effects");
		if(config.contains("hit-sfx")) {
			hitSFX = SoundLoader.getSoundsFromConfig(config.getConfigurationSection("hit-sfx"));
		}
		if(config.contains("hit-vfx")) {
			hitVFX = ParticleLoader.getParticlesFromConfig(config.getConfigurationSection("hit-vfx"));
		}
	}
	
	public void fx(List<Player> players, Location loc, float pitch, int i) {
		playFlightSounds(players, loc, pitch, i);
		for (ParticleData pd : vfx) {
			pd.spawnParticle(loc, new Vector(0, 0, 0));
		}
	}

	/**
	 * In-flight FX for bullets: sounds at the segment end, particles interpolated along
	 * the path (matches GunsAndGadgets drawLine so fast bullets do not look dotted).
	 */
	public void trailSegment(
			List<Player> players,
			Location from,
			Location to,
			float pitch,
			int tick) {
		playFlightSounds(players, to, pitch, tick);
		drawTrailLine(from, to);
	}

	private void playFlightSounds(List<Player> players, Location loc, float pitch, int tick) {
		for (SoundData sd : sfx) {
			if (sd.getDelay() > tick) {
				continue;
			}
			sd.playSound(players, loc, pitch);
		}
	}

	private void drawTrailLine(Location from, Location to) {
		if (from == null || to == null || from.getWorld() == null || !from.getWorld().equals(to.getWorld())) {
			return;
		}

		Particle particle = Particle.CAMPFIRE_COSY_SMOKE;
		if (!vfx.isEmpty()) {
			particle = vfx.get(0).getParticle();
		}

		Vector diff = to.toVector().subtract(from.toVector());
		int steps = (int) (diff.length() * 4);
		if (steps <= 0) {
			return;
		}

		Vector step = diff.clone().multiply(1.0 / steps);
		Location point = from.clone();
		for (int i = 0; i < steps; i++) {
			from.getWorld().spawnParticle(
					particle,
					point,
					1,
					0, 0, 0,
					0,
					null,
					true);
			point.add(step);
		}
	}

	public void hitFX(List<Player> players, Location loc, float pitch) {
		for(SoundData sd : hitSFX) {
			sd.playSound(players, loc, pitch);
		}
		for(ParticleData pd : hitVFX) {
			pd.spawnParticleVisible(loc, new Vector(0, 0.1, 0));
		}
	}

	public boolean isExplosive() {
		return explosive;
	}

	public boolean isFire() {
		return fire;
	}

	public List<String> getPotionEffects() {
		return potionEffects;
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

	public static class LingeringCloudData {
		private final PotionEffect effect;
		private final org.bukkit.Color color;
		private final int cloudDurationTicks;

		public LingeringCloudData(PotionEffect effect, org.bukkit.Color color, int cloudDurationTicks) {
			this.effect = effect;
			this.color = color;
			this.cloudDurationTicks = cloudDurationTicks;
		}

		public PotionEffect getEffect() {
			return effect;
		}

		public org.bukkit.Color getColor() {
			return color;
		}

		public int getCloudDurationTicks() {
			return cloudDurationTicks;
		}
	}

	public List<LingeringCloudData> getLingeringClouds() {
		List<LingeringCloudData> clouds = new ArrayList<>();

		for (String s : potionEffects) {
			LingeringCloudData data = parseLingeringCloud(s);
			if (data != null) {
				clouds.add(data);
			}
		}

		return clouds;
	}

	private LingeringCloudData parseLingeringCloud(String input) {
		if (input == null || input.isEmpty()) return null;

		try {
			Pattern potionPattern = Pattern.compile("potion\\(([^,]+),(\\d+),(\\d+)\\)", Pattern.CASE_INSENSITIVE);
			Pattern colorPattern = Pattern.compile("color\\((\\d+),(\\d+),(\\d+)\\)", Pattern.CASE_INSENSITIVE);
			Pattern trailingNumberPattern = Pattern.compile("(\\d+)\\s*$");

			Matcher potionMatcher = potionPattern.matcher(input);
			Matcher colorMatcher = colorPattern.matcher(input);
			Matcher trailingMatcher = trailingNumberPattern.matcher(input);

			PotionEffectType effectType = PotionEffectType.POISON;
			int amplifier = 0;
			int effectDurationSeconds = 5;

			if (potionMatcher.find()) {
				String potionId = potionMatcher.group(1).trim().toUpperCase();
				amplifier = Integer.parseInt(potionMatcher.group(2));
				effectDurationSeconds = Integer.parseInt(potionMatcher.group(3));

				PotionEffectType parsed = PotionEffectType.getByName(potionId);
				if (parsed != null) {
					effectType = parsed;
				}
			}

			int r = 0;
			int g = 255;
			int b = 0;
			if (colorMatcher.find()) {
				r = clampColor(Integer.parseInt(colorMatcher.group(1)));
				g = clampColor(Integer.parseInt(colorMatcher.group(2)));
				b = clampColor(Integer.parseInt(colorMatcher.group(3)));
			}

			int cloudDurationSeconds = 30;
			if (trailingMatcher.find()) {
				cloudDurationSeconds = Integer.parseInt(trailingMatcher.group(1));
			}

			PotionEffect effect = new PotionEffect(effectType, effectDurationSeconds * 20, amplifier);
			org.bukkit.Color color = org.bukkit.Color.fromRGB(r, g, b);

			return new LingeringCloudData(effect, color, cloudDurationSeconds * 20);
		} catch (Exception ex) {
			// fallback: poison cloud
			PotionEffect fallback = new PotionEffect(PotionEffectType.POISON, 5 * 20, 0);
			org.bukkit.Color fallbackColor = org.bukkit.Color.fromRGB(0, 255, 0);
			return new LingeringCloudData(fallback, fallbackColor, 30 * 20);
		}
	}

	private int clampColor(int value) {
		return Math.max(0, Math.min(255, value));
	}
}
