package net.tfminecraft.VehicleFramework.Bones;

import org.bukkit.Location;
import org.bukkit.util.Vector;

import com.ticxo.modelengine.api.model.ActiveModel;
import com.ticxo.modelengine.api.model.bone.ModelBone;

public class VectorBone {
	private String id;
	
	private ModelBone base;
	private ModelBone align;
	
	public VectorBone(ModelBone b, ModelBone a) {
		id = b.getBoneId();
		base = b;
		align = a;
	}
	
	public void updateModel(ActiveModel m) {
		base = m.getBone(base.getBoneId()).get();
		align = m.getBone(align.getBoneId()).get();
	}
	
	public ActiveModel getModel() {
		return base.getActiveModel();
	}
	
	public String getId() {
		return id;
	}

	public ModelBone getBase() {
		return base;
	}

	public ModelBone getAlign() {
		return align;
	}
	
	public Location getBaseLocation() {
		return base.getLocation();
	}
	
	public Location getAlignLocation() {
		return align.getLocation();
	}
	
	public Vector getVector() {
		Location spawnLocation = base.getLocation();
		Location alignmentLocation = align.getLocation();

		// Check if both locations are the same (by comparing coordinates)
		if (spawnLocation.getX() == alignmentLocation.getX() &&
			spawnLocation.getY() == alignmentLocation.getY() &&
			spawnLocation.getZ() == alignmentLocation.getZ()) {
			return new Vector(1, 0, 0); // Default direction
		}


		Vector direction = alignmentLocation.toVector().subtract(spawnLocation.toVector());

		

		direction.normalize();
		return direction;
	}
}
