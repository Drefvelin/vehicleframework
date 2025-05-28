package net.tfminecraft.VehicleFramework.Data;

import net.tfminecraft.VehicleFramework.Vehicles.ActiveVehicle;

public class NamingData {
    private ActiveVehicle v;
    private int count = 0;

    public NamingData(ActiveVehicle v) {
        this.v = v;
    }

    public boolean tick() {
        return count++>=30;
    }

    public ActiveVehicle getVehicle() {
        return v;
    }
}
