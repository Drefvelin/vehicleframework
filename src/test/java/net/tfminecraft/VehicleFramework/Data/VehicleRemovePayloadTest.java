package net.tfminecraft.VehicleFramework.Data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import net.tfminecraft.VehicleFramework.Enums.VehicleDeath;
import net.tfminecraft.VehicleFramework.Enums.VehicleRemoveReason;
import net.tfminecraft.VehicleFramework.Enums.VehicleRemoveType;

class VehicleRemovePayloadTest {
    @Test
    void deathPayloadExposesCause() {
        VehicleRemovePayload payload = VehicleRemovePayload.death(VehicleDeath.CRASH);
        assertEquals(VehicleRemoveType.DEATH, payload.getType());
        assertTrue(payload.isDeath());
        assertEquals(VehicleDeath.CRASH, payload.getDeathCause().orElse(null));
        assertTrue(payload.getRemoveReason().isEmpty());
    }

    @Test
    void removePayloadExposesReason() {
        VehicleRemovePayload payload = VehicleRemovePayload.remove(VehicleRemoveReason.ADMIN_KILL);
        assertEquals(VehicleRemoveType.REMOVE, payload.getType());
        assertTrue(payload.isRemove());
        assertEquals(VehicleRemoveReason.ADMIN_KILL, payload.getRemoveReason().orElse(null));
        assertTrue(payload.getDeathCause().isEmpty());
    }

    @Test
    void removePayloadDefaultsToGeneric() {
        VehicleRemovePayload payload = VehicleRemovePayload.remove(null);
        assertEquals(VehicleRemoveReason.GENERIC, payload.getRemoveReason().orElse(null));
    }
}
