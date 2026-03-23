package net.tfminecraft.VehicleFramework.Database;

import java.util.UUID;

public class PassengerData {
    private String passenger;   // player name; null for entity passengers
    private String seat;
    private UUID entityUUID;    // null for players

    // Player constructor
    public PassengerData(String p, String s) {
        passenger = p;
        seat = s;
        entityUUID = null;
    }

    // Entity constructor
    public PassengerData(UUID entityUUID, String seat) {
        passenger = null;
        this.entityUUID = entityUUID;
        this.seat = seat;
    }

    public String getPassenger() {
        return passenger;
    }

    public String getSeat() {
        return seat;
    }

    public UUID getEntityUUID() {
        return entityUUID;
    }

    public boolean isEntity() {
        return entityUUID != null;
    }
}
