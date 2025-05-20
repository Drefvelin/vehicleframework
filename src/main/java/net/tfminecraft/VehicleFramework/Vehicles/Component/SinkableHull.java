package net.tfminecraft.VehicleFramework.Vehicles.Component;

import java.util.List;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import com.ticxo.modelengine.api.model.ActiveModel;

import net.tfminecraft.VehicleFramework.Database.IncompleteComponent;
import net.tfminecraft.VehicleFramework.Vehicles.ActiveVehicle;

public class SinkableHull extends Hull{
	
	private Pump pump;
	
	private boolean sinking;
	
	private double sinkProgress;
	
	public SinkableHull(ConfigurationSection config) {
		super(config);
		sinkProgress = 0;
		sinking = false;
	}
	public SinkableHull(ActiveVehicle v, SinkableHull another, ActiveModel m, IncompleteComponent ic) {
		super(another, v, m, ic);
		sinkProgress = ic.getSinkProgress();
		sinking = ic.isSinking();
	}
	
	public boolean hasPump() {
		return pump != null;
	}
	
	public Pump getPump() {
		return pump;
	}
	
	public void setPump(Pump p) {
		pump = p;
	}
	
	public boolean isSinking() {
		return sinking;
	}
	
	public boolean hasSinkProgress() {
		return sinkProgress > 0;
	}

	public int getSinkProgress() {
		return (int) Math.round(sinkProgress);
	}
	@Override
	public void slowTick(List<Player> nearby) {
		super.slowTick(nearby);
		if(!v.getBehaviourHandler().shouldFloat()) return;
		double hullStrength = healthData.getHealthPercentage();
		double pumpThreshold = 80;
		if(hasPump()) {
			pumpThreshold = 100 - pump.getPower();
		}
	    if(pumpThreshold > 80) pumpThreshold = 80;
	    if (hullStrength < pumpThreshold) {
	    	double rate = Math.round((pumpThreshold - hullStrength)/5);
	    	if(rate < 1) rate = 1;
	    	if(rate > 7) rate = 4;
	        sinkProgress += rate;
	        if(sinkProgress > 100) sinkProgress = 100;
	        sinking = true;
	    } else {
	    	sinking = false;
	        if(sinkProgress > 0) {
	        	if(hasPump()) {
	        		sinkProgress -= pump.getPower()/10;
	        	} else {
	        		sinkProgress -= 1;
	        	}
	        }
	    }
	}

}
