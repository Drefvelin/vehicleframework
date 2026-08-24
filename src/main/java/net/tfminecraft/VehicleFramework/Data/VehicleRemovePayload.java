package net.tfminecraft.VehicleFramework.Data;

import java.util.Optional;

import net.tfminecraft.VehicleFramework.Enums.VehicleDeath;
import net.tfminecraft.VehicleFramework.Enums.VehicleRemoveReason;
import net.tfminecraft.VehicleFramework.Enums.VehicleRemoveType;

public final class VehicleRemovePayload {
    private final VehicleRemoveType type;
    private final VehicleDeath deathCause;
    private final VehicleRemoveReason removeReason;

    private VehicleRemovePayload(
            VehicleRemoveType type,
            VehicleDeath deathCause,
            VehicleRemoveReason removeReason) {
        this.type = type;
        this.deathCause = deathCause;
        this.removeReason = removeReason;
    }

    public static VehicleRemovePayload death(VehicleDeath cause) {
        if (cause == null) {
            throw new IllegalArgumentException("death cause required");
        }
        return new VehicleRemovePayload(VehicleRemoveType.DEATH, cause, null);
    }

    public static VehicleRemovePayload remove(VehicleRemoveReason reason) {
        VehicleRemoveReason resolved = reason != null ? reason : VehicleRemoveReason.GENERIC;
        return new VehicleRemovePayload(VehicleRemoveType.REMOVE, null, resolved);
    }

    public VehicleRemoveType getType() {
        return type;
    }

    public Optional<VehicleDeath> getDeathCause() {
        return Optional.ofNullable(deathCause);
    }

    public Optional<VehicleRemoveReason> getRemoveReason() {
        return Optional.ofNullable(removeReason);
    }

    public boolean isDeath() {
        return type == VehicleRemoveType.DEATH;
    }

    public boolean isRemove() {
        return type == VehicleRemoveType.REMOVE;
    }
}
