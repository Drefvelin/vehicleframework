package net.tfminecraft.VehicleFramework.Vehicles.Component.Propulsion;

import net.tfminecraft.VehicleFramework.Vehicles.Component.VehicleComponent;

public class Throttle {
	
	private VehicleComponent engine;
	private String name;
	
	private int max;
	private int min;
	
	private int current;
	
	public Throttle(String name, int x, int n, VehicleComponent en) {
		if(name != null) this.name = name;
		else name = "Throttle";
		max = x;
		min = n;
		current = 0;
		
		engine = en;
	}
	
	public void change(int i) {
		current += i;
		if(current > max) current = max;
		if(current < min) current = min;
	}
	
	public void increase() {
		if(current == max) return;
		if(current >= max*(engine.getHealthData().getHealthPercentage()/100.0)) return;
		current++;
	}
	
	public void decrease() {
		if(current == min) return;
		current--;
	}
	
	public void setThrottle(int i) {
		current = i;
		if(current > max) current = max;
		if(current < min) current = min;
	}

	public String getName() {
		return name;
	}
	
	public int getCurrent() {
		return current;
	}

	public int getMax() {
		return max;
	}

	public int getMin() {
		return min;
	}

	public void normalize() {
		if(current < 0) {
			current++;
		}
		if(current > 0) {
			current--;
		}
		
	}
}
