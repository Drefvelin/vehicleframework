package net.tfminecraft.VehicleFramework.Database;

public class PassengerData {
    private String passenger;
    private String seat;

    public PassengerData(String p, String s) {
        passenger = p;
        seat = s;
    }

    public String getPassenger() {
        return passenger;
    }

    public String getSeat() {
        return seat;
    }
}
