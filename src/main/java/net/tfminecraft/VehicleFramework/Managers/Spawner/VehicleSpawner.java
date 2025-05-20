package net.tfminecraft.VehicleFramework.Managers.Spawner;

import org.bukkit.Location;
import org.bukkit.entity.Entity;

import com.ticxo.modelengine.api.ModelEngineAPI;
import com.ticxo.modelengine.api.model.ActiveModel;
import com.ticxo.modelengine.api.model.ModeledEntity;

import io.lumine.mythic.api.mobs.MythicMob;
import io.lumine.mythic.bukkit.BukkitAdapter;
import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.core.mobs.ActiveMob;
import net.tfminecraft.VehicleFramework.VFLogger;
import net.tfminecraft.VehicleFramework.Database.IncompleteVehicle;
import net.tfminecraft.VehicleFramework.Managers.VehicleManager;
import net.tfminecraft.VehicleFramework.Vehicles.ActiveVehicle;
import net.tfminecraft.VehicleFramework.Vehicles.Vehicle;

public class VehicleSpawner {
	
	public ActiveVehicle spawn(Location loc, Vehicle v, VehicleManager manager, IncompleteVehicle i) {
		MythicMob mob = MythicBukkit.inst().getMobManager().getMythicMob("VehicleFrameworkDummy").orElse(null);
		if(mob != null){ 
			
		    ActiveMob activeMob = mob.spawn(BukkitAdapter.adapt(loc),1);
		    Entity e = activeMob.getEntity().getBukkitEntity();
		    
		    ModeledEntity modeledEntity = ModelEngineAPI.createModeledEntity(e);
			ActiveModel m = ModelEngineAPI.createActiveModel(v.getSkinHandler().getCurrentSkin().getModel());
			modeledEntity.addModel(m, true);
			m.getMountManager().get().setCanRide(true);
		    return new ActiveVehicle(v, e, m, manager, i);
		}
		VFLogger.log(" could not find the VehicleFrameworkDummy mythicmob");
		return null;
	}
}
