package net.tfminecraft.VehicleFramework.Vehicles.Handlers;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;

import com.ticxo.modelengine.api.model.ActiveModel;

import net.tfminecraft.VehicleFramework.VFLogger;
import net.tfminecraft.VehicleFramework.Bones.BoneRotator;
import net.tfminecraft.VehicleFramework.Bones.RotationLimits;
import net.tfminecraft.VehicleFramework.Bones.RotationTarget;
import net.tfminecraft.VehicleFramework.Bones.VectorBone;
import net.tfminecraft.VehicleFramework.Vehicles.ActiveVehicle;

public class BehaviourHandler {
	
	private boolean shouldFloat;
	private List<Material> floatsIn = new ArrayList<>();
	private boolean turnScaling;
	private String vectorString;
	private String rotatorString;
	
	private BoneRotator rotator;
	private VectorBone vector;
	
	private List<String> secondaryRotatorStrings = new ArrayList<>();
	private List<BoneRotator> secondaryRotators = new ArrayList<>();
	
	private List<RotationTarget> rotationTargets = new ArrayList<>();
	
	private TrainHandler trainHandler;
	
	public BehaviourHandler(ConfigurationSection config) {
		getFloat(config);
		turnScaling = config.getBoolean("turn-scaling", true);
		if(config.isConfigurationSection("train")) {
			trainHandler = new TrainHandler(config.getConfigurationSection("train"));
		}
		rotatorString = config.getString("rotator", "N/A");
		if(rotatorString.equalsIgnoreCase("N/A")) VFLogger.log("Vehicle lacks a rotator reference in behaviour, you WILL get errors!");
		vectorString = config.getString("vector", "N/A");
		if(vectorString.equalsIgnoreCase("N/A")) VFLogger.log("Vehicle lacks a vector reference in behaviour, you WILL get errors!");
		if(config.contains("secondary-rotators")) {
			for(String s : config.getStringList("secondary-rotators")) {
				secondaryRotatorStrings.add(s);
			}
		}
		if(config.isConfigurationSection("rotation-targets")) {
			Set<String> set = config.getConfigurationSection("rotation-targets").getKeys(false);

			List<String> list = new ArrayList<String>(set);
			for(String key : list) {
				rotationTargets.add(new RotationTarget(config.getConfigurationSection("rotation-targets."+key)));
			}
		}
	}
	
	public BehaviourHandler() {
		shouldFloat = false;
	}
	
	public BehaviourHandler(ActiveVehicle v, Entity e, ActiveModel m, BehaviourHandler another) {
		if(another.isTrain()) trainHandler = new TrainHandler(v, another.getTrainHandler());
		turnScaling = another.turnScale();
		shouldFloat = another.shouldFloat();
		floatsIn = new ArrayList<>(another.getFloatsIn());
		rotatorString = another.getRotatorString();
		vectorString = another.getVectorString();
		rotator = new BoneRotator(v, e, m.getBone(rotatorString).get(), new RotationLimits());
		vector = new VectorBone(m.getBone(vectorString.split("\\.")[0]).get(), m.getBone(vectorString.split("\\.")[1]).get());
		for(String s : another.getSecondaryRotatorStrings()) {
			secondaryRotators.add(new BoneRotator(v, e, m.getBone(s).get(), new RotationLimits()));
		}
		for(RotationTarget r : another.getRotationTargets()) {
			rotationTargets.add(new RotationTarget(v, r, rotator));
		}
	}
	
	public void updateModel(ActiveModel m) {
		rotator.updateModel(m);
		vector.updateModel(m);
		if(isTrain()) trainHandler.updateModel(m);
		for(RotationTarget r : rotationTargets) {
			r.updateModel();
		}
	}
	
	private void getFloat(ConfigurationSection config) {
		if(!config.contains("float")) {
			shouldFloat = false;
			return;
		}
		shouldFloat = config.getBoolean("float");
		if(!shouldFloat) return;
		if(config.contains("float-in")) {
			for(String s : config.getStringList("float-in")) {
				if(Material.valueOf(s.toUpperCase()) == null) continue;
				floatsIn.add(Material.valueOf(s.toUpperCase()));
			}
		} else {
			floatsIn.add(Material.WATER);
		}
	}

	public boolean turnScale() {
		return turnScaling;
	}
	
	public boolean isTrain() {
		return trainHandler != null;
	}
	public TrainHandler getTrainHandler() {
		return trainHandler;
	}

	public boolean shouldFloat() {
		return shouldFloat;
	}

	public List<Material> getFloatsIn() {
		return floatsIn;
	}
	
	public String getVectorString() {
		return vectorString;
	}
	
	public String getRotatorString() {
		return rotatorString;
	}
	public BoneRotator getRotator() {
		return rotator;
	}

	public VectorBone getVector() {
		return vector;
	}
	
	public List<String> getSecondaryRotatorStrings(){
		return secondaryRotatorStrings;
	}
	
	public List<RotationTarget> getRotationTargets(){
		return rotationTargets;
	}
	
	public void tick(ActiveVehicle vehicle) {
		for(RotationTarget r : rotationTargets) {
			r.run(vehicle);
		}
	}
}
