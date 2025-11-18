package net.tfminecraft.VehicleFramework.Util;

import org.bukkit.EntityEffect;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class Damager {
    public static void applyDirectDamage(LivingEntity entity, double damage) {

        double newHealth = Math.max(0, entity.getHealth() - damage);
        entity.setHealth(newHealth);

        // Visual feedback
        entity.playEffect(EntityEffect.HURT_BERRY_BUSH);

        // Sound
        entity.getWorld().playSound(
                entity.getLocation(),
                entity instanceof Player ?
                        Sound.ENTITY_PLAYER_HURT :
                        Sound.ENTITY_GENERIC_HURT,
                1f, 1f
        );

        // Optional knockback (weak)
        // entity.setVelocity(entity.getLocation().getDirection().multiply(-0.15));
    }
}
