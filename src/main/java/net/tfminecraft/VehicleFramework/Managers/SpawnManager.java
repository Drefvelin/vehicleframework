package net.tfminecraft.VehicleFramework.Managers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.scheduler.BukkitRunnable;

import net.tfminecraft.VehicleFramework.VFLogger;
import net.tfminecraft.VehicleFramework.VehicleFramework;
import net.tfminecraft.VehicleFramework.Database.Database;
import net.tfminecraft.VehicleFramework.Database.IncompleteVehicle;
import net.tfminecraft.VehicleFramework.Loaders.VehicleLoader;
import net.tfminecraft.VehicleFramework.Managers.Spawner.VehicleSpawner;
import net.tfminecraft.VehicleFramework.Util.SpawnLocation;
import net.tfminecraft.VehicleFramework.Vehicles.ActiveVehicle;
import net.tfminecraft.VehicleFramework.Vehicles.Vehicle;

public class SpawnManager implements Listener {
	private static List<SpawnLocation> spawns = new ArrayList<>();
	
	private Database db = new Database();
	
	private VehicleManager vehicleManager;

	public SpawnManager(VehicleManager m) {
		vehicleManager = m;
	}
	
	public static boolean exists(SpawnLocation s) {
		for(SpawnLocation e : spawns) {
			if(e.getFile().equalsIgnoreCase(s.getFile())) return true;
		}
		return false;
	}
	
	public static void add(SpawnLocation s) {
		if(exists(s)) return;
		spawns.add(s);
	}
	
	public static void remove(SpawnLocation s) {
		if(!exists(s)) return;
		spawns.remove(s);
	}
	
	public void start() {
		db.loadActiveSpawnLocations();
		startTickCycle();
	}

	public void save() {
		db.saveSpawnLocations(spawns);
	}
	
	private void startTickCycle() {
		new BukkitRunnable() {
	        @Override
	        public void run() {
	        	for(int i = 0; i<spawns.size(); i++) {
	        		SpawnLocation loc = spawns.get(i);
	        		if(!loc.hasNearby()) continue;
	        		loadVehicle(loc, db.loadVehicle(loc));
					if(i > 0) i--;
	        	}
	        }
	    }.runTaskTimer(VehicleFramework.plugin, 0L, 20L);
	}
	
	private void loadVehicle(SpawnLocation loc, IncompleteVehicle i) {
		Vehicle v = VehicleLoader.getByString(i.getId());
		if(v == null) return;
		vehicleManager.spawn(loc.getLoc(), v, i);
	}
	
	@EventHandler
	public void chunkLoad(ChunkLoadEvent e) {
		db.loadSpawnLocations(e.getChunk());
	}
	@SuppressWarnings("unchecked")
	@EventHandler
	public void chunkUnload(ChunkUnloadEvent e) {
		Chunk c = e.getChunk();
		for(SpawnLocation loc : spawns) {
			if(loc.getChunk().equals(c)) db.saveSpawnLocation(loc);
		}
		HashMap<Entity, ActiveVehicle> vc = (HashMap<Entity, ActiveVehicle>) vehicleManager.get().clone();
		for(Map.Entry<Entity, ActiveVehicle> entry : vc.entrySet()) {
			ActiveVehicle v = entry.getValue();
			Entity entity = entry.getKey();
			if(!entity.getLocation().getChunk().equals(c)) continue;
			vehicleManager.unload(v);
		}
	}
}
