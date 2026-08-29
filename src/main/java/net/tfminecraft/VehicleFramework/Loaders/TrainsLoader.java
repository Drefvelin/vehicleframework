package net.tfminecraft.VehicleFramework.Loaders;

import java.io.File;
import java.io.IOException;

import org.bukkit.Material;
import org.bukkit.Particle;
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
		Cache.trackMinJunctionSpacing = Math.max(1.0, config.getDouble("min-junction-spacing", 16.0));
		Cache.trackMaxJunctionLength = Math.max(
				Cache.trackMinLayDistance,
				config.getDouble("max-junction-length", 32.0));
		Cache.trackJunctionArmDistance = Math.max(1.0, config.getDouble("junction-arm-distance", 16.0));
		Cache.trackMaxGradeDegrees = Math.max(1.0, config.getDouble("max-grade-degrees", 10.0));
		Cache.trackDesiredGradeDegrees = Math.min(
				Cache.trackMaxGradeDegrees,
				Math.max(1.0, config.getDouble("desired-grade-degrees", 6.0)));
		Cache.debugLogging = config.getBoolean("debug-logging", true);
		loadFx(config);
		loadBuild(config);
	}

	private void loadBuild(FileConfiguration config) {
		Cache.trackBuildIntervalTicks = Math.max(0, config.getInt("build.interval-ticks", 4));
		Cache.trackBuildSwing = config.getBoolean("build.swing", true);
		Cache.trackBuildSound = config.getString("build.sound", "minecraft:block.gravel.break");
		Cache.trackBuildSoundVolume = (float) config.getDouble("build.sound-volume", 0.55);
		Cache.trackBuildSoundPitch = (float) config.getDouble("build.sound-pitch", 0.9);
		Cache.trackBuildCount = Math.max(0, config.getInt("build.count", 6));
		Cache.trackBuildWidth = Math.max(1, config.getInt("build.width", 1));
		Cache.trackBuildExtra = config.getDouble("build.extra", 0.08);
		String particleName = config.getString("build.particle", "BLOCK_CRACK");
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
		String blockName = config.getString("build.particle-block", "GRAVEL");
		try {
			Cache.trackBuildBlock = Material.valueOf(blockName.trim().toUpperCase());
		} catch (Exception e) {
			Cache.trackBuildBlock = Material.GRAVEL;
			VFLogger.log("Invalid track build particle-block: " + blockName + ". Using GRAVEL.");
		}
	}

	private void loadFx(FileConfiguration config) {
		Cache.trackFxWidth = Math.max(1, config.getInt("fx.width", 3));
		Cache.trackFxCount = Math.max(0, config.getInt("fx.count", 3));
		Cache.trackFxExtra = config.getDouble("fx.extra", 0.06);
		Cache.trackFxYOffset = config.getDouble("fx.y-offset", 0.08);
		Cache.trackFxSound = config.getString("fx.sound", "minecraft:block.stone.break");
		Cache.trackFxSoundVolume = (float) config.getDouble("fx.sound-volume", 0.35);
		Cache.trackFxSoundPitch = (float) config.getDouble("fx.sound-pitch", 0.85);
		Cache.trackFxSoundInterval = Math.max(0.25, config.getDouble("fx.sound-interval-blocks", 2.5));
		String particleName = config.getString("fx.particle", "BLOCK_CRACK");
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
		String blockName = config.getString("fx.particle-block", "GRAVEL");
		try {
			Cache.trackFxBlock = Material.valueOf(blockName.trim().toUpperCase());
		} catch (Exception e) {
			Cache.trackFxBlock = Material.GRAVEL;
			VFLogger.log("Invalid track fx particle-block: " + blockName + ". Using GRAVEL.");
		}
	}
}
