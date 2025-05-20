package net.tfminecraft.VehicleFramework.Weapons.Ammunition;

import org.bukkit.configuration.ConfigurationSection;

import net.tfminecraft.VehicleFramework.VFLogger;
import net.tfminecraft.VehicleFramework.Weapons.Ammunition.Data.AmmunitionData;

public class ClusterBomb extends Ammunition{
	private AmmunitionData clusterData;
	
	private int fuse;
	private int amount;
	private double spread;
	
	public ClusterBomb (String key, ConfigurationSection config) {
		super(key, config);
		fuse = config.getInt("fuse", 40);
		amount = config.getInt("amount", 4);
		spread = config.getDouble("spread", 2);
		if(!config.isConfigurationSection("cluster")) VFLogger.log("Cluster bomb has no cluster section in the config");
		clusterData = new AmmunitionData(config.getConfigurationSection("cluster"));
	}

	public AmmunitionData getClusterData() {
		return clusterData;
	}

	public int getFuse() {
		return fuse;
	}

	public int getAmount() {
		return amount;
	}

	public double getSpread() {
		return spread;
	}
	
	
}
