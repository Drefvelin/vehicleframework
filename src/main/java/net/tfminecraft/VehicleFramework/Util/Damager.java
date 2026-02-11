package net.tfminecraft.VehicleFramework.Util;

import org.bukkit.EntityEffect;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import net.tfminecraft.VehicleFramework.VehicleFramework;

public class Damager {
    public static void damage(LivingEntity entity, double damage) {
        VehicleFramework.getVehicleManager().setDamaged(entity, true);
        try {
            entity.damage(damage);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            VehicleFramework.getVehicleManager().setDamaged(entity, false);
        }
    }
}
