package net.tfminecraft.VehicleFramework;

import java.io.File;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import net.tfminecraft.VehicleFramework.Cache.Cache;
import net.tfminecraft.VehicleFramework.Database.LogWriter;
import net.tfminecraft.VehicleFramework.Loaders.AmmunitionLoader;
import net.tfminecraft.VehicleFramework.Loaders.ConfigLoader;
import net.tfminecraft.VehicleFramework.Loaders.FuelLoader;
import net.tfminecraft.VehicleFramework.Loaders.VehicleLoader;
import net.tfminecraft.VehicleFramework.Managers.CommandManager;
import net.tfminecraft.VehicleFramework.Managers.VehicleManager;
import net.tfminecraft.VehicleFramework.Protocol.VehiclePacketListener;
import net.tfminecraft.VehicleFramework.Util.TabCompletion;

public class VehicleFramework extends JavaPlugin{
	
	public static VehicleFramework plugin;
	
	private static LogWriter log;
	private final CommandManager commandManager = new CommandManager();
	private final static VehicleManager vehicleManager = new VehicleManager();
	
	private final ConfigLoader configLoader = new ConfigLoader();
	private final AmmunitionLoader ammunitionLoader = new AmmunitionLoader();
	private final VehicleLoader vehicleLoader = new VehicleLoader();
	private final FuelLoader fuelLoader = new FuelLoader();
	
	@Override
	public void onEnable() {
		Bukkit.getLogger().info("Initializing VF");
		printBanner();
		plugin = this;
		log = new LogWriter(getDataFolder());
		VFLogger.info("Running checks...");
		createFolders();
		createConfigs();
		loadConfigs();
		VFLogger.info("Starting systems...");
		registerListeners();
		startManagers();
		VFLogger.info("Setup complete!");
	}
	@Override
	public void onDisable() {
		vehicleManager.unloadAll();
		Cache.removeLights();
	}
	public void registerListeners() {
		getServer().getPluginManager().registerEvents(vehicleManager, this);
		getServer().getPluginManager().registerEvents(vehicleManager.getRepairManager(), this);
		getServer().getPluginManager().registerEvents(vehicleManager.getSpawnManager(), this);
		
		getCommand(commandManager.cmd1).setExecutor(commandManager);
		getCommand(commandManager.cmd1).setTabCompleter(new TabCompletion());
		
		VehiclePacketListener packetListener = new VehiclePacketListener(vehicleManager);
        packetListener.register();
	}
	public void startManagers() {
		vehicleManager.start();
	}
	public void createFolders() {
		if (!getDataFolder().exists()) getDataFolder().mkdir();
		File subFolder = new File(getDataFolder(), "data");
		if(!subFolder.exists()) subFolder.mkdir();
		subFolder = new File(getDataFolder(), "data/vehicles");
		if(!subFolder.exists()) subFolder.mkdir();
		subFolder = new File(getDataFolder(), "vehicles");
		if(!subFolder.exists()) subFolder.mkdir();
		subFolder = new File(getDataFolder(), "ammunition");
		if(!subFolder.exists()) subFolder.mkdir();
		subFolder = new File(getDataFolder(), "templates");
		if(!subFolder.exists()) subFolder.mkdir();
	}
	public void loadConfigs() {
		configLoader.load(new File(getDataFolder(), "config.yml"));
		VFLogger.info("Loading fuel...");
		fuelLoader.load(new File(getDataFolder(), "fuel.yml"));
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
	}
	
	public void createConfigs() {
		String[] files = {
				"config.yml",
				"fuel.yml"
				};
		for(String s : files) {
			File newConfigFile = new File(getDataFolder(), s);
	        if (!newConfigFile.exists()) {
	        	newConfigFile.getParentFile().mkdirs();
	            saveResource(s, false);
	        }
		}
	}
	
	//Access we dont need static variables all over the place:
	public static VehicleManager getVehicleManager() {
		return vehicleManager;
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
