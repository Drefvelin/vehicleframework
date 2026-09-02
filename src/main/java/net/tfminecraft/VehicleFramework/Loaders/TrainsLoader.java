package net.tfminecraft.VehicleFramework.Loaders;

import java.io.File;
import java.io.IOException;

import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import net.tfminecraft.VehicleFramework.Cache.Cache;
import net.tfminecraft.VehicleFramework.VFLogger;

public class TrainsLoader {

	public void load(File file) {
		VFLogger.info("Loading trains...");
		FileConfiguration config = new YamlConfiguration();
		try {
			config.load(file);
		} catch (IOException | InvalidConfigurationException e) {
			e.printStackTrace();
			return;
		}
		Cache.trackItemSmall = config.getString("item-small", "ia.tfmc:track_small");
		Cache.trackItemMedium = config.getString("item-medium", "ia.tfmc:track_medium");
		Cache.trackItemLarge = config.getString("item-large", "ia.tfmc:track_large");
		Cache.trackLayerItem = config.getString("item-layer", "v.iron_shovel");
		Cache.trackRemoverItem = config.getString("item-remover", "v.iron_pickaxe");
		Cache.trackRecorderItem = config.getString("item-recorder", "v.clock");
		Cache.trackJunctionItem = config.getString("item-junction", "v.diamond_shovel");
		Cache.trackSwitchItem = config.getString("item-switch", "ia.tfmc:railroad_switch");
		Cache.trackItem = config.getString("item-track", "ia.tfmc:train_track");
		Cache.trackSwitchOffsetAlong = config.getDouble("switch.offset-along", -1.2);
		Cache.trackSwitchOffsetOut = config.getDouble("switch.offset-out", 2.4);
		Cache.trackSwitchOffsetY = config.getDouble("switch.offset-y", 0.0);
		Cache.trackSwitchYawInward = (float) config.getDouble("switch.yaw-inward", 90.0);
		Cache.trackSwitchThrowDegrees = (float) config.getDouble("switch.throw-degrees", 90.0);
		Cache.trackSwitchThrowDegreesPerSecond = (float) Math.max(
				1.0, config.getDouble("switch.throw-degrees-per-second", 90.0));
		Cache.trackSnapDistance = Math.max(0.5, config.getDouble("snap-distance", 3.0));
		Cache.trackDisplayYOffset = config.getDouble("display-y-offset", 0.5);
		Cache.trackVehicleYOffset = config.getDouble("vehicle-y-offset", 0.5);
		Cache.trackMaxTurnDegrees = Math.max(1.0, config.getDouble("max-turn-degrees", 25.0));
		Cache.trackMinLayDistance = Math.max(1.0, config.getDouble("min-lay-distance", 8.0));
		Cache.trackJoinDistance = Math.max(0.25, config.getDouble("join-distance", 1.5));
		Cache.trackPlaceKeepoutRadius = Math.max(0.0, config.getDouble("place-keepout-radius", 1.5));
		Cache.trackMinJunctionSpacing = Math.max(1.0, config.getDouble("min-junction-spacing", 16.0));
		Cache.trackMaxJunctionLength = Math.max(
				Cache.trackMinLayDistance,
				config.getDouble("max-junction-length", 32.0));
		Cache.trackJunctionArmDistance = Math.max(1.0, config.getDouble("junction-arm-distance", 16.0));
		Cache.trackMaxGradeDegrees = Math.max(1.0, config.getDouble("max-grade-degrees", 10.0));
		Cache.trackDesiredGradeDegrees = Math.min(
				Cache.trackMaxGradeDegrees,
				Math.max(1.0, config.getDouble("desired-grade-degrees", 6.0)));
		Cache.trackResyncChunksPerTick = Math.max(1, config.getInt("resync-chunks-per-tick", 2));
		Cache.debugLogging = config.getBoolean("debug-logging", false);
		loadFx(config);
		loadBuild(config);
	}

	private void loadBuild(FileConfiguration config) {
		ConfigurationSection build = config.getConfigurationSection("build");
		if (build == null) {
			Cache.trackBuildIntervalTicks = 4;
			Cache.trackBuildSwing = true;
			Cache.trackBuildSound = "minecraft:block.gravel.break";
			Cache.trackBuildSoundVolume = 0.55f;
			Cache.trackBuildSoundPitch = 0.9f;
			Cache.trackBuildSound2 = "minecraft:block.iron.place";
			Cache.trackBuildSound2Volume = 1.0f;
			Cache.trackBuildSound2Pitch = 2.0f;
			Cache.trackBuildCount = 6;
			Cache.trackBuildWidth = 1;
			Cache.trackBuildExtra = 0.08;
			Cache.trackBuildYOffset = 0.08;
			Cache.trackBuildParticle = Particle.BLOCK_CRACK;
			Cache.trackBuildBlock = Material.GRAVEL;
			return;
		}
		Cache.trackBuildIntervalTicks = Math.max(0, build.getInt("interval-ticks", 4));
		Cache.trackBuildSwing = build.getBoolean("swing", true);
		Cache.trackBuildSound = build.getString("sound", "minecraft:block.gravel.break");
		Cache.trackBuildSoundVolume = (float) build.getDouble("sound-volume", 0.55);
		Cache.trackBuildSoundPitch = (float) build.getDouble("sound-pitch", 0.9);
		Cache.trackBuildSound2 = build.getString("sound-2", "minecraft:block.iron.place");
		Cache.trackBuildSound2Volume = (float) build.getDouble("sound-2-volume", 1.0);
		Cache.trackBuildSound2Pitch = (float) build.getDouble("sound-2-pitch", 2.0);
		Cache.trackBuildCount = Math.max(0, build.getInt("count", 6));
		Cache.trackBuildWidth = Math.max(1, build.getInt("width", 1));
		Cache.trackBuildExtra = build.getDouble("extra", 0.08);
		Cache.trackBuildYOffset = build.getDouble("y-offset", 0.08);
		String particleName = build.getString("particle", "BLOCK_CRACK");
		if (particleName == null || particleName.isBlank() || particleName.equalsIgnoreCase("none")) {
			Cache.trackBuildParticle = null;
		} else {
			try {
				Cache.trackBuildParticle = Particle.valueOf(particleName.trim().toUpperCase());
			} catch (IllegalArgumentException e) {
				Cache.trackBuildParticle = Particle.BLOCK_CRACK;
				VFLogger.log("Invalid track build particle: " + particleName + ". Using BLOCK_CRACK.");
			}
		}
		String blockName = build.getString("particle-block", "GRAVEL");
		try {
			Cache.trackBuildBlock = Material.valueOf(blockName.trim().toUpperCase());
		} catch (Exception e) {
			Cache.trackBuildBlock = Material.GRAVEL;
			VFLogger.log("Invalid track build particle-block: " + blockName + ". Using GRAVEL.");
		}
	}

	private void loadFx(FileConfiguration config) {
		ConfigurationSection fx = config.getConfigurationSection("fx");
		if (fx == null) {
			Cache.trackFxWidth = 3;
			Cache.trackFxCount = 3;
			Cache.trackFxExtra = 0.06;
			Cache.trackFxYOffset = 0.08;
			Cache.trackFxSound = "minecraft:block.stone.break";
			Cache.trackFxSoundVolume = 0.35f;
			Cache.trackFxSoundPitch = 0.85f;
			Cache.trackFxSoundInterval = 2.5;
			Cache.trackFxParticle = Particle.BLOCK_CRACK;
			Cache.trackFxBlock = Material.GRAVEL;
			return;
		}
		Cache.trackFxWidth = Math.max(1, fx.getInt("width", 3));
		Cache.trackFxCount = Math.max(0, fx.getInt("count", 3));
		Cache.trackFxExtra = fx.getDouble("extra", 0.06);
		Cache.trackFxYOffset = fx.getDouble("y-offset", 0.08);
		Cache.trackFxSound = fx.getString("sound", "minecraft:block.stone.break");
		Cache.trackFxSoundVolume = (float) fx.getDouble("sound-volume", 0.35);
		Cache.trackFxSoundPitch = (float) fx.getDouble("sound-pitch", 0.85);
		Cache.trackFxSoundInterval = Math.max(0.25, fx.getDouble("sound-interval-blocks", 2.5));
		String particleName = fx.getString("particle", "BLOCK_CRACK");
		if (particleName == null || particleName.isBlank() || particleName.equalsIgnoreCase("none")) {
			Cache.trackFxParticle = null;
		} else {
			try {
				Cache.trackFxParticle = Particle.valueOf(particleName.trim().toUpperCase());
			} catch (IllegalArgumentException e) {
				Cache.trackFxParticle = Particle.BLOCK_CRACK;
				VFLogger.log("Invalid track fx particle: " + particleName + ". Using BLOCK_CRACK.");
			}
		}
		String blockName = fx.getString("particle-block", "GRAVEL");
		try {
			Cache.trackFxBlock = Material.valueOf(blockName.trim().toUpperCase());
		} catch (Exception e) {
			Cache.trackFxBlock = Material.GRAVEL;
			VFLogger.log("Invalid track fx particle-block: " + blockName + ". Using GRAVEL.");
		}
	}
}
