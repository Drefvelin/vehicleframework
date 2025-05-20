package net.tfminecraft.VehicleFramework.Vehicles.Handlers.Train;

import org.bukkit.util.Vector;

import com.ticxo.modelengine.api.model.ActiveModel;
import com.ticxo.modelengine.api.model.bone.ModelBone;

import net.tfminecraft.VehicleFramework.Vehicles.ActiveVehicle;

public class Connector {
	private ActiveVehicle v;
	private String boneString;
	private ModelBone bone;
	
	public Connector(String s) {
		boneString = s;
	}
	
	public Connector(ActiveVehicle vehicle, Connector another) {
		v = vehicle;
		bone = v.getModel().getBone(another.getBoneString()).get();
	}
	
	public void updateModel(ActiveModel m) {
		bone = m.getBone(bone.getBoneId()).get();
	}

	public String getBoneString() {
		return boneString;
	}

	public ModelBone getBone() {
		return bone;
	}

	public Vector getOffset() {
		return bone.getLocation().toVector().subtract(v.getEntity().getLocation().toVector());
	}
	
	
}
