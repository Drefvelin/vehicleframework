package net.tfminecraft.VehicleFramework.Vehicles.Handlers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import com.ticxo.modelengine.api.model.ActiveModel;

import net.tfminecraft.VehicleFramework.Data.SoundData;
import net.tfminecraft.VehicleFramework.Vehicles.Handlers.Utility.DirectionalLight;

public class UtilityHandler {
	private HashMap<Player, Long> cooldown = new HashMap<>();
	
	private List<DirectionalLight> lights = new ArrayList<>();
	
	private SoundData horn;
	
	public UtilityHandler(ConfigurationSection config) {
		if(config.isConfigurationSection("lights")) {
			Set<String> set = config.getConfigurationSection("lights").getKeys(false);

			List<String> list = new ArrayList<String>(set);
			
			for(String key : list) {
				lights.add(new DirectionalLight(config.getConfigurationSection("lights."+key)));
			}
		}
		if(config.isConfigurationSection("horn")) {
			horn = new SoundData(config.getConfigurationSection("horn"));
		}
	}
	
	public UtilityHandler(ActiveModel m, UtilityHandler another) {
		for(DirectionalLight l : another.getLights()) {
			lights.add(new DirectionalLight(l, m));
		}
		horn = another.getHorn();
	}
	
	public void tick(List<Player> nearby) {
		for(DirectionalLight l : lights) {
			l.tick(nearby);
		}
	}

	public List<DirectionalLight> getLights() {
		return lights;
	}
	
	public SoundData getHorn() {
		return horn;
	}
	
	public void toggleLights(Player p) {
		if(cooldown.containsKey(p)) {
			if(cooldown.get(p) > System.currentTimeMillis()) return;
		}
		cooldown.put(p, System.currentTimeMillis()+400);
		for(DirectionalLight l : lights) {
			l.toggle();
		}
	}
	
	public void honk(Location loc) {
		if(horn == null) return;
		horn.playSound(loc);
	}
}
