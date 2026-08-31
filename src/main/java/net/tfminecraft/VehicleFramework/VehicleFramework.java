package net.tfminecraft.VehicleFramework;

import java.io.File;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import net.coreprotect.CoreProtect;
import net.coreprotect.CoreProtectAPI;
import net.tfminecraft.VehicleFramework.Cache.Cache;
import net.tfminecraft.VehicleFramework.Database.LogWriter;
import net.tfminecraft.VehicleFramework.Database.Database;
import net.tfminecraft.VehicleFramework.Database.PersistenceLog;
import net.tfminecraft.VehicleFramework.Loaders.AmmunitionLoader;
import net.tfminecraft.VehicleFramework.Loaders.ConfigLoader;
import net.tfminecraft.VehicleFramework.Loaders.FuelLoader;
import net.tfminecraft.VehicleFramework.Loaders.TrainsLoader;
import net.tfminecraft.VehicleFramework.Loaders.VehicleLoader;
import net.tfminecraft.VehicleFramework.Loaders.WeaponTemplateLoader;
import net.tfminecraft.VehicleFramework.Loaders.ArmorTemplateLoader;
import net.tfminecraft.VehicleFramework.Loaders.DeathTemplateLoader;
import net.tfminecraft.VehicleFramework.Managers.CommandManager;
import net.tfminecraft.VehicleFramework.Managers.VehicleManager;
import net.tfminecraft.VehicleFramework.Protocol.VehiclePacketListener;
import net.tfminecraft.VehicleFramework.Util.Metrics;
import net.tfminecraft.VehicleFramework.Util.MythicMobsIntegration;
import net.tfminecraft.VehicleFramework.Util.TabCompletion;
import net.tfminecraft.VehicleFramework.Tracks.TrackBuildAnimator;
import net.tfminecraft.VehicleFramework.Tracks.TrackDisplayManager;
import net.tfminecraft.VehicleFramework.Tracks.TrackLog;
import net.tfminecraft.VehicleFramework.Tracks.RecorderLog;
import net.tfminecraft.VehicleFramework.Tracks.TrackRegistry;
import net.tfminecraft.VehicleFramework.Tracks.TrackToolListener;
import net.tfminecraft.VehicleFramework.Vehicles.Controller.GroundEngineLog;

public class VehicleFramework extends JavaPlugin{
	
	public static VehicleFramework plugin;

	private static boolean dirtyBit = false;
	
	private static LogWriter log;
	private final CommandManager commandManager = new CommandManager();
	private final static VehicleManager vehicleManager = new VehicleManager();
	
	private final ConfigLoader configLoader = new ConfigLoader();
	private final TrainsLoader trainsLoader = new TrainsLoader();
	private final AmmunitionLoader ammunitionLoader = new AmmunitionLoader();
	private final VehicleLoader vehicleLoader = new VehicleLoader();
	private final WeaponTemplateLoader weaponTemplateLoader = new WeaponTemplateLoader();
	private final ArmorTemplateLoader armorTemplateLoader = new ArmorTemplateLoader();
	private final DeathTemplateLoader deathTemplateLoader = new DeathTemplateLoader();
	private final FuelLoader fuelLoader = new FuelLoader();
	private final Database db = new Database();
	private static TrackRegistry trackRegistry;
	private static TrackDisplayManager trackDisplayManager;
	private static VehiclePacketListener packetListener;
	
	@Override
	public void onEnable() {
		dirtyBit = db.isDirtyFlag();
		Bukkit.getLogger().info("Initializing VF");
		printBanner();
		plugin = this;
		trackRegistry = new TrackRegistry(getDataFolder());
		log = new LogWriter(getDataFolder());
		VFLogger.info("Running checks...");
		createFolders();
		createConfigs();
		loadConfigs();
		Cache.applyTrackDisplayStyle();
		VFLogger.info("Starting systems...");
		registerListeners();
		startManagers();
		setPlugins();
		if (trackDisplayManager != null) {
			trackDisplayManager.spawnLoadedChunks();
		}
		VFLogger.info("Setup complete!");
		int pluginId = 26823; // Replace with your actual bStats plugin ID
		Metrics metrics = new Metrics(this, pluginId);
		db.setDirtyFlag(true);
	}
	@Override
	public void onDisable() {
		PersistenceLog.append("DISABLE_BEGIN");
		db.setDirtyFlag(false);
		if (trackDisplayManager != null) {
			TrackBuildAnimator.finishAll();
			trackDisplayManager.despawnAll();
		}
		vehicleManager.unloadAll();
		vehicleManager.getSpawnManager().save();
		Cache.removeLights();
		Cache.removeProjectiles();
	}
	public void registerListeners() {
		getServer().getPluginManager().registerEvents(vehicleManager, this);
		getServer().getPluginManager().registerEvents(vehicleManager.getRepairManager(), this);
		getServer().getPluginManager().registerEvents(new TrackToolListener(), this);
		trackDisplayManager = new TrackDisplayManager();
		getServer().getPluginManager().registerEvents(trackDisplayManager, this);
		
		getCommand(commandManager.cmd1).setExecutor(commandManager);
		getCommand(commandManager.cmd1).setTabCompleter(new TabCompletion());
		
		VehiclePacketListener listener = new VehiclePacketListener(vehicleManager);
		packetListener = listener;
		listener.register();
	}
	public void startManagers() {
		vehicleManager.start();
	}
	public void createFolders() {
		if (!getDataFolder().exists()) getDataFolder().mkdir();
		File subFolder = new File(getDataFolder(), "data");
		if(!subFolder.exists()) subFolder.mkdir();
		subFolder = new File(getDataFolder(), "data/tracks");
		if(!subFolder.exists()) subFolder.mkdir();
		subFolder = new File(getDataFolder(), "vehicles");
		if(!subFolder.exists()) subFolder.mkdir();
		subFolder = new File(getDataFolder(), "ammunition");
		if(!subFolder.exists()) subFolder.mkdir();
		subFolder = new File(getDataFolder(), "templates");
		if(!subFolder.exists()) subFolder.mkdir();
		subFolder = new File(getDataFolder(), "templates/weapons");
		if(!subFolder.exists()) subFolder.mkdir();
		subFolder = new File(getDataFolder(), "templates/armor");
		if(!subFolder.exists()) subFolder.mkdir();
		subFolder = new File(getDataFolder(), "templates/roles");
		if(!subFolder.exists()) subFolder.mkdir();
		subFolder = new File(getDataFolder(), "templates/death");
		if(!subFolder.exists()) subFolder.mkdir();
	}
	public void loadConfigs() {
		configLoader.load(new File(getDataFolder(), "config.yml"));
		trainsLoader.load(new File(getDataFolder(), "trains.yml"));
		VFLogger.info("Loading fuel...");
		fuelLoader.load(new File(getDataFolder(), "fuel.yml"));
		weaponTemplateLoader.clear();
		VFLogger.info("Loading weapon templates...");
		weaponTemplateLoader.loadFolder(new File(getDataFolder(), "templates/weapons"));
		armorTemplateLoader.clear();
		VFLogger.info("Loading armor templates...");
		armorTemplateLoader.loadArmorFolder(new File(getDataFolder(), "templates/armor"));
		VFLogger.info("Loading role templates...");
		armorTemplateLoader.loadRoleFolder(new File(getDataFolder(), "templates/roles"));
		deathTemplateLoader.clear();
		VFLogger.info("Loading death templates...");
		deathTemplateLoader.loadFolder(new File(getDataFolder(), "templates/death"));
		File folder = new File(getDataFolder(), "vehicles");
		VFLogger.info("Loading vehicles...");
    	for (final File file : folder.listFiles()) {
    		if(!file.isDirectory()) {
    			vehicleLoader.load(file);
    		}
    	}
		
    	folder = new File(getDataFolder(), "ammunition");
		VFLogger.info("Loading ammunition...");
    	for (final File file : folder.listFiles()) {
    		if(!file.isDirectory()) {
    			ammunitionLoader.load(file);
    		}
    	}
		GroundEngineLog.configure(Cache.groundEngineLogging, Cache.wipeLog, getDataFolder());
		TrackLog.configure(Cache.debugLogging, Cache.debugLogging, getDataFolder());
		PersistenceLog.configure(Cache.debugLogging, getDataFolder());
		RecorderLog.configure(Cache.debugLogging, getDataFolder());
		if (trackRegistry != null) {
			trackRegistry.loadFromDisk();
		}
	}
	
	public void createConfigs() {
		String[] files = {
				"config.yml",
				"trains.yml",
				"fuel.yml",
				"templates/weapons/gun_turret.yml",
				"templates/weapons/aa_turret.yml",
				"templates/weapons/naval_cannon.yml",
				"templates/weapons/flak_cannon.yml",
				"templates/weapons/autocannon.yml",
				"templates/armor/wooden.yml",
				"templates/armor/airship.yml",
				"templates/armor/armored.yml",
				"templates/armor/aircraft.yml",
				"templates/armor/emplacement.yml",
				"templates/armor/wagon.yml",
				"templates/roles/roles.yml",
				"templates/death/explode_small.yml",
				"templates/death/explode_medium.yml",
				"templates/death/explode_large.yml",
				"templates/death/ship.yml"
				};
		for(String s : files) {
			File newConfigFile = new File(getDataFolder(), s);
	        if (!newConfigFile.exists()) {
	        	newConfigFile.getParentFile().mkdirs();
	            saveResource(s, false);
	        }
		}
	}

	public void setPlugins() {
		Plugin plugin = getServer().getPluginManager().getPlugin("CoreProtect");

		if (plugin != null && plugin.isEnabled() && plugin instanceof CoreProtect) {
			Cache.coreProtect = true;
			VFLogger.info("Detected CoreProtect, hooking on");
		}
		if (Bukkit.getPluginManager().isPluginEnabled("MythicMobs")) {
			getServer().getPluginManager().registerEvents(new MythicMobsIntegration(), this);
			VFLogger.info("MythicMobs integration enabled.");
		}
	}

	public static CoreProtectAPI getCoreProtect() {
        Plugin coreProtect = plugin.getServer().getPluginManager().getPlugin("CoreProtect");

        // Check that CoreProtect is loaded
        if (coreProtect == null || !(coreProtect instanceof CoreProtect)) {
            return null;
        }

        // Check that the API is enabled
        CoreProtectAPI CoreProtect = ((CoreProtect) coreProtect).getAPI();
        if (CoreProtect.isEnabled() == false) {
            return null;
        }

        // Check that a compatible version of the API is loaded
        if (CoreProtect.APIVersion() < 10) {
            return null;
        }

        return CoreProtect;
	}

	public static VehicleFramework getInstance() {
		return plugin;
	}

	public void reload() {
		PersistenceLog.append("RELOAD_BEGIN loaded=" + vehicleManager.get().size());
		vehicleManager.unloadAll();
		vehicleManager.getSpawnManager().save();
		Cache.removeLights();
		Cache.removeProjectiles();
		loadConfigs();
		vehicleManager.reload();
		if (trackDisplayManager != null) {
			trackDisplayManager.reloadSwitches();
		}
		PersistenceLog.append("RELOAD_END pendingSpawns cycle=" + PersistenceLog.spawnCycle());
	}
	
	//Access we dont need static variables all over the place:
	public static TrackRegistry getTrackRegistry() {
		return trackRegistry;
	}

	public static TrackDisplayManager getTrackDisplayManager() {
		return trackDisplayManager;
	}

	public static VehicleManager getVehicleManager() {
		return vehicleManager;
	}

	public static VehiclePacketListener getPacketListener() {
		return packetListener;
	}

	public static LogWriter getLog() {
		return log;
	}

	public void printBanner() {
		Bukkit.getLogger().info("-------------------------------------------------------------------------------------------------------------------------------------------------");
		Bukkit.getLogger().info(" __   __            _          _                 _                 ___                                                                     _     ");
		Bukkit.getLogger().info(" \\ \\ / /    ___    | |_       (_)      __       | |      ___      | __|     _ _    __ _     _ __      ___    __ __ __    ___       _ _    | |__  ");
		Bukkit.getLogger().info("  \\ V /    / -_)   | ' \\      | |     / _|      | |     / -_)     | _|     | '_|  / _` |   | '  \\    / -_)   \\ V  V /   / _ \\     | '_|   | / /  ");
		Bukkit.getLogger().info("  _\\_/_    \\___|   |_||_|    _|_|_    \\__|_    _|_|_    \\___|    _|_|_    _|_|_   \\__,_|   |_|_|_|   \\___|    \\_/\\_/    \\___/    _|_|_    |_\\_\\  ");
		Bukkit.getLogger().info("_| \"\"\"\"| _|\"\"\"\"\"| _|\"\"\"\"\"| _|\"\"\"\"\"| _|\"\"\"\"\"| _|\"\"\"\"\"| _|\"\"\"\"\"| _| \"\"\"\" | _|\"\"\"\"\"| _|\"\"\"\"\"| _|\"\"\"\"\"| _|\"\"\"\"\"| _|\"\"\"\"\"|  _|\"\"\"\"\"| _|\"\"\"\"\"| _|\"\"\"\"\"| ");
		Bukkit.getLogger().info(" `-0-0-'  `-0-0-'  `-0-0-'  `-0-0-'  `-0-0-'  `-0-0-'  `-0-0-'  `-0-0-'  `-0-0-'  `-0-0-'  `-0-0-'  `-0-0-'  `-0-0-'   `-0-0-'  `-0-0-'  `-0-0-' ");
		Bukkit.getLogger().info("------------------------------------------------------------------by drefvelin-------------------------------------------------------------------");
	}
}
