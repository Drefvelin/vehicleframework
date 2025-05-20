package net.tfminecraft.VehicleFramework.Vehicles.Util;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import com.ticxo.modelengine.api.model.ActiveModel;
import com.ticxo.modelengine.api.model.bone.ModelBone;

public class VFX {
	public List<String> boneList = new ArrayList<>();
	
	public List<ModelBone> modelBones = new ArrayList<>();
	
	public VFX(List<String> list) {
		boneList = list;
	}
	
	public VFX(VFX another, ActiveModel m) {
		boneList = another.getBoneList();
		for(String s : boneList) {
			modelBones.add(m.getBone(s).get());
		}
	}
	
	public void updateModel(ActiveModel m) {
		for(ModelBone bone : modelBones) {
			bone = m.getBone(bone.getBoneId()).get();
		}
	}

	public List<String> getBoneList() {
		return boneList;
	}

	public List<ModelBone> getModelBones() {
		return modelBones;
	}
	
	public void particle(Particle particle, List<Player> nearby, int a) {
		if(a < 1) a = 1;
		if(a > 3) a = 3;
		for(ModelBone b : modelBones) {
			for(int i = 0; i<a; i++) {
	        	Vector velocity = new Vector(
	        	        (Math.random() - 0.5) * 0.1, // Small random X movement
	        	        Math.random() * 0.2 + 0.1,   // Always upward Y movement
	        	        (Math.random() - 0.5) * 0.1  // Small random Z movement
	        	    );
	        	for(Player p : nearby) {
	        		p.spawnParticle(particle, b.getLocation(), 0, velocity.getX(), velocity.getY(), velocity.getZ(), 0.2);
	        	}
	        }
		}
		
	}
}
