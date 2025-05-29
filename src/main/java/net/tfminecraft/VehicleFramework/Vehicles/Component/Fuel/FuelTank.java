package net.tfminecraft.VehicleFramework.Vehicles.Component.Fuel;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import net.tfminecraft.VehicleFramework.Enums.State;
import net.tfminecraft.VehicleFramework.Loaders.FuelLoader;
import net.tfminecraft.VehicleFramework.Vehicles.ActiveVehicle;
import net.tfminecraft.VehicleFramework.Vehicles.Component.Propulsion.Throttle;
import net.tfminecraft.VehicleFramework.Vehicles.Fuel.Fuel;

public class FuelTank {
    private double current;
    private double capacity;
    private double rate;
    private Fuel input;

    private List<State> states = new ArrayList<>();

    public FuelTank(ConfigurationSection config) {
        current = 0;
        capacity = config.getDouble("fuel-capacity", 400);
        rate = config.getDouble("fuel-burn-rate", 2);
        input = FuelLoader.getByString(config.getString("fuel", "none"));
        if(config.contains("refuel-states")) {
			for(String s : config.getStringList("refuel-states")) {
				try {
		            states.add(State.valueOf(s.toUpperCase()));
		        } catch (IllegalArgumentException e) {
		            System.out.println("Invalid state in refuel-states: " + s);
		        }
			}
		}
    }

    public FuelTank(double c, double cap, double r, List<State> s, Fuel fuel) {
        current = c;
        capacity = cap;
        rate = r;
        states = s;
        input = fuel;
    }

    public FuelTank(FuelTank another) {
        current = another.getCurrent();
        capacity = another.getCapacity();
        rate = another.getRate();
        states = another.getStates();
        input = another.getInput();
    }

    public boolean hasInput() {
        return input != null;
    }

    public Fuel getInput() {
        return input;
    }

    public double getCurrent() {
        return current;
    }

    public double getCapacity() {
        return capacity;
    }

    public double getRate() {
        return rate;
    }

    public List<State> getStates() {
        return states;
    }

    public void setFuel(double amount) {
        current = amount;
        if(current > capacity) current = capacity;
        if(current < 0) current = 0;
    }

    public void refuel(Player p, ActiveVehicle v, int amount){
        if(states.size() > 0 && !states.contains(v.getCurrentState().getType())) {
            if(p != null) p.sendMessage("§cCannot refuel in this state ("+v.getCurrentState().getType().toString()+")");
            return;
        }
        if(current == capacity) {
            if(p != null) p.sendMessage("§cFuel tank is full");
            return;
        }
        if(amount < 0) return;
        current+=amount;
        if(current > capacity) current = capacity;
        p.getWorld().playSound(v.getEntity().getLocation(), Sound.ITEM_BUCKET_FILL, 1f, 0.8f);
        p.sendMessage("§aFuel: §e"+Math.round(current)+"/"+Math.round(capacity));
    }

    public void tick(Throttle throttle) {
        double percentage = (double) throttle.getCurrent()/throttle.getMax();
        if(percentage < 0) percentage *=-1;
        if(current == 0) return;
        double amount = rate*percentage;
        current-=amount;
        if(current < 0) current = 0;
    }

    public int getPercentage() {
        return (int) Math.round((current/capacity)*100);
    }
}
