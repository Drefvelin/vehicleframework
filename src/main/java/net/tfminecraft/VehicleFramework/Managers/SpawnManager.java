package net.tfminecraft.VehicleFramework.Managers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.bukkit.Chunk;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.scheduler.BukkitRunnable;

import com.ticxo.modelengine.api.ModelEngineAPI;
import com.ticxo.modelengine.api.model.ActiveModel;
import com.ticxo.modelengine.api.model.ModeledEntity;

import io.lumine.mythic.api.mobs.MythicMob;
import io.lumine.mythic.bukkit.BukkitAdapter;
import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.core.mobs.ActiveMob;
import net.tfminecraft.VehicleFramework.VehicleFramework;
import net.tfminecraft.VehicleFramework.Database.Database;
import net.tfminecraft.VehicleFramework.Database.IncompleteVehicle;
import net.tfminecraft.VehicleFramework.Loaders.VehicleLoader;
import net.tfminecraft.VehicleFramework.Util.SpawnLocation;
import net.tfminecraft.VehicleFramework.Vehicles.Vehicle;

public class SpawnManager implements Listener {
	private static List<SpawnLocation> spawns = new ArrayList<>();
	
	private Database db = new Database();
	
	private VehicleManager vehicleManager;
	
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
	
	public void start(VehicleManager m) {
		vehicleManager = m;
		db.loadActiveSpawnLocations();
		startTickCycle();
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
		MythicMob mob = MythicBukkit.inst().getMobManager().getMythicMob(v.getVehicleData().getMob()).orElse(null);
		if(mob == null) return;   
		
	    ActiveMob active = mob.spawn(BukkitAdapter.adapt(loc.getLoc()),1);
	    Entity entity = active.getEntity().getBukkitEntity();
	    entity.setRotation(loc.getLoc().getYaw(), loc.getLoc().getPitch());
	    remove(loc);
	    new BukkitRunnable() {
	        @Override
	        public void run() {
	        	ModeledEntity me = ModelEngineAPI.getModeledEntity(entity);
	    		Optional<ActiveModel> opt = me.getModel(v.getVehicleData().getModel());
	    		if(opt.isEmpty()) return;
	    		ActiveModel model = opt.get();
	    		if(v != null) {
	    			Vehicle created = null;
	    	    	switch (v.getType()) {
	    				case AIR:
	    					break;
	    				case LAND:
	    					break;
	    				case SEA:
	    					created = new Ship((Ship) v, entity, model, vehicleManager, i);
	    					break;
	    				default:
	    					break;
	    		    	
	    	    	}
	    	    	if(created == null) return;
	    	    	vehicleManager.add(entity, created);
	    		}
	        }
	    }.runTaskLater(WarMachines.plugin, 5L);  
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
		HashMap<Entity, Vehicle> vc = (HashMap<Entity, Vehicle>) VehicleManager.get().clone();
		for(Entity entity : vc.keySet()) {
			if(!entity.getLocation().getChunk().equals(c)) continue;
			Vehicle v = vc.get(entity);
			vehicleManager.unload(v);
		}
		HashMap<Entity, SiegeWeapon> wc = (HashMap<Entity, SiegeWeapon>) WeaponManager.get().clone();
		for(Entity entity : wc.keySet()) {
			if(!entity.getLocation().getChunk().equals(c)) continue;
			SiegeWeapon w = wc.get(entity);
			weaponManager.unload(w);
		}
	}
}
