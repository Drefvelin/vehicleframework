package net.tfminecraft.VehicleFramework.Data;

public final class StoredVehicleMeta {
    private final String uuid;
    private final String name;
    private final String typeId;
    private final String owner;

    public StoredVehicleMeta(String uuid, String name, String typeId, String owner) {
        this.uuid = uuid;
        this.name = name;
        this.typeId = typeId;
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

    public String getOwner() {
        return owner;
    }

    public static boolean isPlayerOwner(String owner) {
        if (owner == null || owner.isBlank()) {
            return false;
        }
        if (!owner.regionMatches(true, 0, "player_", 0, 7)) {
            return false;
        }
        return !owner.equalsIgnoreCase("player_none") && owner.length() > 7;
    }
}
