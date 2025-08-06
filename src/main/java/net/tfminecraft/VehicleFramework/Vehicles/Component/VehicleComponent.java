package net.tfminecraft.VehicleFramework.Vehicles.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.lang3.text.WordUtils;
import org.bukkit.Particle;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import com.ticxo.modelengine.api.model.ActiveModel;

import net.tfminecraft.VehicleFramework.Data.DamageData;
import net.tfminecraft.VehicleFramework.Data.HealthData;
import net.tfminecraft.VehicleFramework.Database.IncompleteComponent;
import net.tfminecraft.VehicleFramework.Enums.Component;
import net.tfminecraft.VehicleFramework.Enums.CustomAction;
import net.tfminecraft.VehicleFramework.Vehicles.ActiveVehicle;
import net.tfminecraft.VehicleFramework.Vehicles.Util.Fire;
import net.tfminecraft.VehicleFramework.Vehicles.Util.VFX;

public class VehicleComponent {
	protected ActiveVehicle v;
	
	protected HashMap<Player, Long> cooldown = new HashMap<>();
	
	protected String alias;
	
	protected Component type;
	
	protected HealthData healthData;
	protected DamageData damageData;
	
	protected double damageChance;
	
	protected VFX vfx;
	
	protected Fire fire;
	
	protected boolean fatal;
	
	@SuppressWarnings({ "unchecked", "deprecation" })
	public VehicleComponent(Component type, ConfigurationSection config) {
		this.type = type;
		alias = config.getString("alias", WordUtils.capitalize(type.toString().toLowerCase()));
		if(type.equals(Component.HULL)) {
			fatal = true;
		} else {
			fatal = false;
		}
		fatal = config.getBoolean("fatal", fatal);
		healthData = new HealthData(config.getDouble("health"), 0, config.getInt("repair-time"));
		damageData = new DamageData((List<String>) config.getList("damage", new ArrayList<String>()));
		damageChance = config.getDouble("damage-chance");
		vfx = new VFX(config.getStringList("vfx"));
	}
	public VehicleComponent(VehicleComponent another, ActiveVehicle v, ActiveModel model, IncompleteComponent ic) {
		this.v = v;
		type = another.getType();
		alias = another.getAlias();
		fatal = another.isFatal();
		if(ic != null) {
			healthData = new HealthData(another.getHealthData().getHealth(), ic.getDamage(), another.getHealthData().getBaseRepairTime());
			if(ic.hasFire()) {
				startFire();
				fire.setProgress(ic.getFireProgress());
			}
		} else {
			healthData = new HealthData(another.getHealthData().getHealth(), another.getHealthData().getDamage(), another.getHealthData().getBaseRepairTime());
		}
		damageData = another.getDamageData();
		damageChance = another.getDamageChance();
		vfx = new VFX(another.getVfx(), model);
	}
	
	public void updateModel(ActiveModel m) {
		vfx.updateModel(m);
	}

	public void slowTick(List<Player> nearby) {
		if(isOnFire()) {
			if(fire.getProgress() == 0) {
				fire = null;
			} else {
				fire.tick();
			}
		}
	}
	public void tick(List<Player> nearby) {
		if(isOnFire()) {
			fireEffects(nearby);
		}
		healthData.tick();
	}
	
	public String getAlias() {
		return alias;
	}
	
	public boolean isFatal() {
		return fatal;
	}
	
	public boolean isOnFire() {
		return fire != null;
	}
	
	public void startFire() {
		if(!v.isOnFire() && v.hasEffect(CustomAction.FIRE_START)) v.playEffect(CustomAction.FIRE_START);
		fire = new Fire();
	}
	
	public Component getType() {
		return type;
	}
	
	public Fire getFire() {
		return fire;
	}
	
	public VFX getVfx() {
		return vfx;
	}
	
	public HealthData getHealthData() {
		return healthData;
	}
	public DamageData getDamageData() {
		return damageData;
	}
	
	public double getDamageChance() {
		return damageChance;
	}

	public void damage(String cause, double a) {
		if(damageData.hasModifier(cause)) {
			a = a*damageData.getModifier(cause);
		}
		
		if(Math.random() <= damageChance) {
			healthData.damage(a);
		}
		if(a > 15) {
			double fireChance = (100.0-healthData.getHealthPercentage())/1000.0;
			fireChance *= 1.0+(a/100.0);
			if(Math.random() <= fireChance) {
				startFire();
			}
		}
	}
	
	public void fireEffects(List<Player> nearby) {
		vfx.particle(Particle.CAMPFIRE_COSY_SMOKE, nearby, fire.getProgress()/10);
		vfx.particle(Particle.FLAME, nearby, fire.getProgress()/10);
	}
}
