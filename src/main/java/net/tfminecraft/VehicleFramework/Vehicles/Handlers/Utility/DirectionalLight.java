package net.tfminecraft.VehicleFramework.Vehicles.Handlers.Utility;

import java.util.List;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import com.ticxo.modelengine.api.model.ActiveModel;

import net.tfminecraft.VehicleFramework.Bones.VectorBone;
import net.tfminecraft.VehicleFramework.Util.LightEffect;

public class DirectionalLight {
	
	private String vectorString;
	private VectorBone vector;
	
	private int falloff;
	private int power;
	
	private boolean active = false;
	
	public DirectionalLight(ConfigurationSection config) {
		vectorString = config.getString("vector");
		power = config.getInt("power", 8);
		if(power < 1 || power > 15) power = 8;
		falloff = config.getInt("falloff", 1);
		if(falloff < 1) falloff = 1;
	}
	
	public DirectionalLight(DirectionalLight another, ActiveModel m) {
		vectorString = another.getVectorString();
		vector = new VectorBone(m.getBone(vectorString.split("\\.")[0]).get(), m.getBone(vectorString.split("\\.")[1]).get());
		falloff = another.getFalloff();
		power = another.getPower();
	}
	
	public void updateModel(ActiveModel m) {
		vector.updateModel(m);
	}
	
	public void tick(List<Player> nearby) {
		if(!active) return;
		Location start = vector.getBaseLocation();
		Vector direction = vector.getVector();

        direction = direction.normalize();
        
        int tp = power;
        int m = 1;
        Location lightLoc = start.clone().add(direction.clone().multiply(m));
        while(tp > 0) {
        	lightLoc = start.clone().add(direction.clone().multiply(m));
        	sendFakeLight(lightLoc, 15); // Maximum light level
            m++;
            tp-=falloff;
        }
	}
	
	
	private void sendFakeLight(Location loc, int lightLevel) {
		LightEffect e = new LightEffect();
		e.createTemporaryLight(loc, lightLevel);
    }
	

	public String getVectorString() {
		return vectorString;
	}

	public VectorBone getVector() {
		return vector;
	}

	public int getFalloff() {
		return falloff;
	}

	public int getPower() {
		return power;
	}
	
	public void toggle() {
		active = !active;
	}
	
}
