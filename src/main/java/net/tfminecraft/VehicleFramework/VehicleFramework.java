package net.tfminecraft.VehicleFramework;

import java.io.File;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import net.tfminecraft.VehicleFramework.Cache.Cache;
import net.tfminecraft.VehicleFramework.Loaders.AmmunitionLoader;
import net.tfminecraft.VehicleFramework.Loaders.ConfigLoader;
import net.tfminecraft.VehicleFramework.Loaders.VehicleLoader;
import net.tfminecraft.VehicleFramework.Managers.CommandManager;
import net.tfminecraft.VehicleFramework.Managers.VehicleManager;
import net.tfminecraft.VehicleFramework.Protocol.VehiclePacketListener;
import net.tfminecraft.VehicleFramework.Util.TabCompletion;

public class VehicleFramework extends JavaPlugin{
	
	public static VehicleFramework plugin;
	
	private final CommandManager commandManager = new CommandManager();
	
	//private final SpawnManager spawnManager = new SpawnManager();
	private final static VehicleManager vehicleManager = new VehicleManager();
	//private final RepairManager repairManager = new RepairManager();
	
	private final ConfigLoader configLoader = new ConfigLoader();
	private final AmmunitionLoader ammunitionLoader = new AmmunitionLoader();
	private final VehicleLoader vehicleLoader = new VehicleLoader();
	
	@Override
	public void onEnable() {
		Bukkit.getLogger().info("Initializing VF");
		plugin = this;
		createFolders();
		createConfigs();
		loadConfigs();
		registerListeners();
		startManagers();
	}
	@Override
	public void onDisable() {
		//vehicleManager.unloadAll();
		Cache.removeLights();
	}
	public void registerListeners() {
		getServer().getPluginManager().registerEvents(vehicleManager, this);
		//getServer().getPluginManager().registerEvents(repairManager, this);
		//getServer().getPluginManager().registerEvents(spawnManager, this);
		
		getCommand(commandManager.cmd1).setExecutor(commandManager);
		getCommand(commandManager.cmd1).setTabCompleter(new TabCompletion());
		
		VehiclePacketListener packetListener = new VehiclePacketListener(vehicleManager);
        packetListener.register();
	}
	public void startManagers() {
		//weaponManager.start(repairManager);
		vehicleManager.start();
		//spawnManager.start(vehicleManager, weaponManager);
	}
	public void createFolders() {
		if (!getDataFolder().exists()) getDataFolder().mkdir();
		File subFolder = new File(getDataFolder(), "data");
		if(!subFolder.exists()) subFolder.mkdir();
		subFolder = new File(getDataFolder(), "data/vehicles");
		if(!subFolder.exists()) subFolder.mkdir();
		subFolder = new File(getDataFolder(), "data/weapons");
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
		File folder = new File(getDataFolder(), "vehicles");
    	for (final File file : folder.listFiles()) {
    		if(!file.isDirectory()) {
    			vehicleLoader.load(file);
    		}
    	}
    	folder = new File(getDataFolder(), "ammunition");
    	for (final File file : folder.listFiles()) {
    		if(!file.isDirectory()) {
    			ammunitionLoader.load(file);
    		}
    	}
		/*
		weaponLoader.load(new File(getDataFolder(), "weapons.yml"));
		
    	*/
	}
	
	public void createConfigs() {
		String[] files = {
				"config.yml",
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
}
