package net.tfminecraft.VehicleFramework.Weapons.Ammunition;

import org.bukkit.configuration.ConfigurationSection;

import net.tfminecraft.VehicleFramework.VFLogger;
import net.tfminecraft.VehicleFramework.Enums.Projectile;
import net.tfminecraft.VehicleFramework.Weapons.Ammunition.Data.AmmunitionData;

public class Ammunition {
	protected Projectile type;
	protected String name;
	protected String id;
	protected AmmunitionData data;
	
	public static Ammunition create(String key, ConfigurationSection config) {
		String t = config.getString("type", "CANNONBALL");
		if(Projectile.valueOf(t.toUpperCase()) == null) VFLogger.log(key+" ammunition has malformed type");
		Projectile type = Projectile.valueOf(t.toUpperCase());
		switch(type) {
			case BULLET:
				return new Bullet(key, config);
			case CANNONBALL:
				return new Ammunition(key, config);
			case CLUSTER:
				return new ClusterBomb(key, config);
			case TORPEDO:
				return new FusedExplosive(key, config);
			case BOMB:
				return new FusedExplosive(key, config);
			default:
				return new Ammunition(key, config);
			
		}
	}
	
	public Ammunition (String key, ConfigurationSection config) {
		id = key;
		name = config.getString("name", key);
		data = new AmmunitionData(config);
		String t = config.getString("type", "CANNONBALL");
		if(Projectile.valueOf(t.toUpperCase()) == null) VFLogger.log(key+" ammunition has malformed type");
		type = Projectile.valueOf(t.toUpperCase());
	}
	
	public Projectile getType() {
		return type;
	}

	public String getId() {
		return id;
	}
	
	public String getName() {
		return name;
	}
	
	public AmmunitionData getData() {
		return data;
	}
}
