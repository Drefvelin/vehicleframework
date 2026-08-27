package net.tfminecraft.VehicleFramework.Data;

import java.util.Optional;

import org.bukkit.Location;

public final class OwnedVehicleSummary {
    private final String uuid;
    private final String name;
    private final String typeId;
    private final Optional<Location> location;
    private final boolean spawned;
    private final String owner;

    public OwnedVehicleSummary(
            String uuid,
            String name,
            String typeId,
            Optional<Location> location,
            boolean spawned) {
        this(uuid, name, typeId, location, spawned, null);
    }

    public OwnedVehicleSummary(
            String uuid,
            String name,
            String typeId,
            Optional<Location> location,
            boolean spawned,
            String owner) {
        this.uuid = uuid;
        this.name = name;
        this.typeId = typeId;
        this.location = location == null ? Optional.empty() : location;
        this.spawned = spawned;
        this.owner = owner;
    }

    public String getUuid() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    public String getTypeId() {
        return typeId;
    }

    public Optional<Location> getLocation() {
        return location;
    }

    public boolean isSpawned() {
        return spawned;
    }

    public String getOwner() {
        return owner;
    }
}
