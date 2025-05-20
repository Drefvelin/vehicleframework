package net.tfminecraft.VehicleFramework.Vehicles.Handlers;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.ticxo.modelengine.api.model.ActiveModel;

import net.tfminecraft.VehicleFramework.Database.IncompleteComponent;
import net.tfminecraft.VehicleFramework.Database.IncompleteVehicle;
import net.tfminecraft.VehicleFramework.Enums.Component;
import net.tfminecraft.VehicleFramework.Vehicles.ActiveVehicle;
import net.tfminecraft.VehicleFramework.Vehicles.Component.Engine;
import net.tfminecraft.VehicleFramework.Vehicles.Component.GearedEngine;
import net.tfminecraft.VehicleFramework.Vehicles.Component.Harness;
import net.tfminecraft.VehicleFramework.Vehicles.Component.Hull;
import net.tfminecraft.VehicleFramework.Vehicles.Component.Pump;
import net.tfminecraft.VehicleFramework.Vehicles.Component.SinkableHull;
import net.tfminecraft.VehicleFramework.Vehicles.Component.VehicleComponent;
import net.tfminecraft.VehicleFramework.Vehicles.Component.Wings;

public class ComponentHandler {

	private List<VehicleComponent> components = new ArrayList<>();
	
	public ComponentHandler(ConfigurationSection config) {
		Set<String> set = config.getKeys(false);

		List<String> list = new ArrayList<String>(set);
		for(String c : list) {
			Component cType = Component.valueOf(c.toUpperCase());
			ConfigurationSection compSection = config.getConfigurationSection(c);
			switch (cType) {
				case ENGINE:
					components.add(new Engine(compSection));
					break;
				case HULL:
					if(compSection.getBoolean("sinkable", false)) {
						components.add(new SinkableHull(compSection));
					} else {
						components.add(new Hull(compSection));
					}
					break;
				case PUMP:
					components.add(new Pump(compSection));
					break;
				case WINGS:
					components.add(new Wings(compSection));
					break;
				case HARNESS:
					components.add(new Harness(compSection));
					break;
				case GEARED_ENGINE:
					components.add(new GearedEngine(compSection));
					break;
				default:
					break;
			}
		}
	}
	
	public void updateModel(ActiveModel m) {
		for(VehicleComponent c : components) {
			c.updateModel(m);
		}
	}
	
	public void slowTick() {
		fireSpread();
	}
	public void fireSpread() {
		for(VehicleComponent c : components) {
			if(c.isOnFire()) {
				for(VehicleComponent o : getComponents()) {
					if(o.isOnFire()) continue;
					if(Math.floor(Math.random()*100)>c.getFire().getProgress()/2) continue;
					o.startFire();
				}
			}
		}
	}
	public void randomFire() {
		int i = (int) Math.floor(Math.random()*components.size());
		int safeguard = 0;
		while(components.get(i).isOnFire() && safeguard < 10) {
			i = (int) Math.floor(Math.random()*components.size());
			safeguard++;
		}
		components.get(i).startFire();
	}
	public ComponentHandler(ActiveVehicle v, Entity e, ActiveModel m, IncompleteVehicle i, ComponentHandler another) {
		for(VehicleComponent component : another.getComponents()) {
			IncompleteComponent ic = null;
			if(i != null) {
				for(IncompleteComponent icm : i.getComponents()) {
					if(icm.getType().equals(component.getType())) ic = icm;
				}
			}
			if(ic == null) {
				ic = new IncompleteComponent(component.getType(), 0, 0, 0);
			}
			switch (component.getType()) {
				case ENGINE:
					components.add(new Engine(v, (Engine) component, e, m, ic));
					break;
				case HULL:
					if(component instanceof SinkableHull) {
						components.add(new SinkableHull(v, (SinkableHull) component, m, ic));
					} else {
						components.add(new Hull((Hull) component, v, m, ic));
					}
					break;
				case PUMP:
					components.add(new Pump((Pump) component, v, m, ic));
					break;
				case WINGS:
					components.add(new Wings(v, (Wings) component, m, ic));
					break;
				case HARNESS:
					components.add(new Harness((Harness) component, v, m, ic));
					break;
				case GEARED_ENGINE:
					components.add(new GearedEngine(v, (GearedEngine) component, e, m, ic));
					break;
				default:
					break;
			}
		}
	}
	
	public void damage(String cause, double a) {
		for(VehicleComponent c : components) {
			c.damage(cause, a);
		}
	}
	
	public boolean hasComponent(Component c) {
		for(VehicleComponent component : components) {
			if(component.getType().equals(c)) return true;
		}
		return false;
	}
	
	public List<VehicleComponent> getComponents(){
		return components;
	}
	
	public List<VehicleComponent> getComponents(Component c) {
		List<VehicleComponent> list = new ArrayList<>();
		for(VehicleComponent component : components) {
			if(component.getType().equals(c)) list.add(component);
		}
		return list;
	}
	
	public VehicleComponent getComponent(Component c) {
		for(VehicleComponent component : components) {
			if(component.getType().equals(c)) return component;
		}
		return null;
	}
}
