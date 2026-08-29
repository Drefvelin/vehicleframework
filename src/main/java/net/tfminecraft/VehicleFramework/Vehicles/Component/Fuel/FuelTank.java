package net.tfminecraft.VehicleFramework.Vehicles.Component.Fuel;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.joml.Math;

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

    private boolean useFuel = true;

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
        if(input == null) useFuel = false;
    }

    public FuelTank(double c, double cap, double r, List<State> s, Fuel fuel) {
        current = c;
        capacity = cap;
        rate = r;
        states = s;
        input = fuel;
        useFuel = input != null ? true : false;
    }

    public FuelTank(FuelTank another) {
        current = another.getCurrent();
        capacity = another.getCapacity();
        rate = another.getRate();
        states = another.getStates();
        input = another.getInput();
        useFuel = input != null ? true : false;
    }

    public boolean useFuel() {
        return useFuel;
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

    public boolean addFuel(int amount) {
        if (amount <= 0 || current >= capacity) {
            return false;
        }
        current += amount;
        if (current > capacity) {
            current = capacity;
        }
        return true;
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
        Throttle t = v.getThrottle();
        int throttle = t == null ? 0 : t.getCurrent();
        if (engineBlocksRefuel(input != null && input.refuelWhileRunning(), throttle)) {
            if(p != null) p.sendMessage("§cCannot refuel while the engine is on");
            return;
        }
        if(amount < 0) return;
        current+=amount;
        if(current > capacity) current = capacity;
        playRefuelSound(p, v);
        p.sendMessage("§aFuel: §e"+Math.round(current)+"/"+Math.round(capacity));
        p.getInventory().getItemInMainHand().setAmount(p.getInventory().getItemInMainHand().getAmount()-1);
    }

    public static boolean engineBlocksRefuel(boolean refuelWhileRunning, int throttle) {
        return throttle != 0 && !refuelWhileRunning;
    }

    private void playRefuelSound(Player p, ActiveVehicle v) {
        if (p == null || p.getWorld() == null || v.getEntity() == null) {
            return;
        }
        Location loc = v.getEntity().getLocation();
        if (input != null && input.getSound() != null && !input.getSound().isBlank()) {
            p.getWorld().playSound(loc, input.getSound(), SoundCategory.BLOCKS, input.getSoundVolume(), input.getSoundPitch());
            return;
        }
        p.getWorld().playSound(loc, Sound.ITEM_BUCKET_FILL, 1f, 0.8f);
    }

    public void tick(Throttle throttle) {
        double amount = 1;
        if(throttle.getMax() == 0) amount = 1;
        if(throttle.getCurrent() == 0) return;
        else {
            double percentage = (double) throttle.getCurrent()/throttle.getMax();
            if(percentage < 0) percentage *=-1;
            percentage = Math.max(1, percentage);
            if(current == 0) return;
            amount = rate*percentage;
        }
        current-=amount;
        if(current < 0) current = 0;
    }

    public int getPercentage() {
        return (int) Math.round((current/capacity)*100);
    }
}
