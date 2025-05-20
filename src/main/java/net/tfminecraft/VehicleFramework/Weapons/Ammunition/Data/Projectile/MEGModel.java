package net.tfminecraft.VehicleFramework.Weapons.Ammunition.Data.Projectile;

import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import com.ticxo.modelengine.api.ModelEngineAPI;
import com.ticxo.modelengine.api.animation.BlueprintAnimation;
import com.ticxo.modelengine.api.animation.property.SimpleProperty;
import com.ticxo.modelengine.api.model.ActiveModel;
import com.ticxo.modelengine.api.model.ModeledEntity;

import io.lumine.mythic.api.mobs.MythicMob;
import io.lumine.mythic.bukkit.BukkitAdapter;
import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.core.mobs.ActiveMob;
import net.tfminecraft.VehicleFramework.VFLogger;

public class MEGModel implements ProjectileModel{
	private String model;
	private String animation;
	
	public MEGModel(ConfigurationSection config) {
		if(!config.contains("model")) VFLogger.log("ammunition is supposed to have a model, but no model specified in config");
		model = config.getString("model", "none");
		animation = config.getString("animation", "none");
	}
	
	public String getModel() {
		return model;
	}
	@Override
	public double getOffset() {
		return 0.0;
	}
	
	@Override
	public Entity spawn(Location loc) {
		MythicMob mob = MythicBukkit.inst().getMobManager().getMythicMob("VehicleFrameworkDummy").orElse(null);
		if(mob != null){ 
			
		    ActiveMob activeMob = mob.spawn(BukkitAdapter.adapt(loc),1);
		    Entity e = activeMob.getEntity().getBukkitEntity();
		    float yaw = loc.getYaw();
		    float pitch = loc.getPitch();

		    if (Float.isNaN(yaw) || Float.isInfinite(yaw)) yaw = 0;
		    if (Float.isNaN(pitch) || Float.isInfinite(pitch)) pitch = 0;

		    loc.setYaw(yaw);
		    loc.setPitch(pitch);
		    e.setRotation(loc.getYaw(), loc.getPitch());
		    
		    ModeledEntity modeledEntity = ModelEngineAPI.createModeledEntity(e);
			ActiveModel m = ModelEngineAPI.createActiveModel(model);
			modeledEntity.addModel(m, true);
			if(!animation.equalsIgnoreCase("none")) {
				BlueprintAnimation anim = m.getBlueprint().getAnimations().get(animation);
				if(anim == null) return e;
				m.getAnimationHandler().playAnimation(new SimpleProperty(m, anim), true);
			}
		    return e;
		}
		VFLogger.log(" could not find the VehicleFrameworkDummy mythicmob");

        return null;
    }
}
