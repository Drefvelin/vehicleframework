package net.tfminecraft.VehicleFramework.Vehicles.State;

public class Parameter {
	private double min;
	private double max;
	
	public Parameter(double mn, double mx) {
		min = mn;
		max = mx;
	}
	public Parameter(String s) {
		min = Double.parseDouble(s.split("\\,")[0]);
		max = Double.parseDouble(s.split("\\,")[1]);
	}
	
	public boolean isWithin(double value) {
		return value >= min && value <= max;
	}

	public double getMin() {
		return min;
	}

	public double getMax() {
		return max;
	}
	
}
