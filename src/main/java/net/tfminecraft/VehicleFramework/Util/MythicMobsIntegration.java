package net.tfminecraft.VehicleFramework.Util;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import io.lumine.mythic.bukkit.events.MythicMobDespawnEvent;
import net.tfminecraft.VehicleFramework.VehicleFramework;
import net.tfminecraft.VehicleFramework.Managers.VehicleManager;
import net.tfminecraft.VehicleFramework.Vehicles.ActiveVehicle;

public class MythicMobsIntegration implements Listener{
    VehicleManager manager = VehicleFramework.getVehicleManager();

    @EventHandler
	public void despawnEvent(MythicMobDespawnEvent e) {
		ActiveVehicle v = manager.get(e.getEntity());
		if(manager.get(e.getEntity()) != null) {
			manager.unload(v);
		}
	}
}
