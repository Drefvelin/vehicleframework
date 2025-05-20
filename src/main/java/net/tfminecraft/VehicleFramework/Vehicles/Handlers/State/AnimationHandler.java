package net.tfminecraft.VehicleFramework.Vehicles.Handlers.State;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.configuration.ConfigurationSection;

import com.ticxo.modelengine.api.animation.BlueprintAnimation;
import com.ticxo.modelengine.api.animation.property.SimpleProperty;
import com.ticxo.modelengine.api.model.ActiveModel;

import net.tfminecraft.VehicleFramework.Enums.Animation;

public class AnimationHandler {
	private HashMap<Animation, List<String>> animations = new HashMap<>();
	
	private ActiveModel m;
	
	public AnimationHandler(ConfigurationSection config) {
		for (Animation animation : Animation.values()) {
	        String enumString = animation.name().toLowerCase();
	        if (config.contains(enumString)) {
	            animations.put(animation, config.getStringList(enumString));
	        } else {
	        	animations.put(animation, new ArrayList<String>());
	        }
	    }
	}
	
	public AnimationHandler() {
		for (Animation animation : Animation.values()) {
			animations.put(animation, new ArrayList<String>());
	    }
	}
	
	public AnimationHandler(ActiveModel m, AnimationHandler other) {
		this.m = m;
        for (Map.Entry<Animation, List<String>> entry : other.animations.entrySet()) {
            animations.put(entry.getKey(), new ArrayList<>(entry.getValue()));
        }
    }
	
	public List<String> get(Animation a){
		return animations.get(a);
	}
	
	public boolean hasModel() {
		return m != null;
	}
	
	public void updateModel(ActiveModel model) {
		m = model;
	}
	
	public void animate(Animation a) {
		stopOtherAnimations(a);
		for(String anim : animations.get(a)) {
			castAnimation(anim);
		}
	}
	
	public void animate(Animation a, int i) {
		stopOtherAnimations(a);
		if(animations.get(a).size() < i+1) return;
		castAnimation(animations.get(a).get(i));
	}
	
	public void animate(String anim) { //if you want to use an animation not specified in the config, for example for a weapon shoot animation
		castAnimation(anim);
	}
	
	private void castAnimation(String s) {
		BlueprintAnimation anim = m.getBlueprint().getAnimations().get(s);
		if(anim == null) return;
		if(m.getAnimationHandler().isPlayingAnimation(s)) return;
		m.getAnimationHandler().playAnimation(new SimpleProperty(m, anim), true);
	}
	
	public void stopAllAnimations() {
		for(Animation a : animations.keySet()) {
			stop(a);
		}
	}
	
	private void stopOtherAnimations(Animation a) {
		switch(a) {
			case BACKWARD:
				stop(Animation.FORWARD);
				break;
			case DEFAULT:
				break;
			case EXPLODE:
				break;
			case FORWARD:
				stop(Animation.BACKWARD);
				break;
			case LEFT:
				break;
			case RIGHT:
				break;
			case SINK:
				break;
			default:
				break;
		}
	}
	
	public void stop(Animation a) {
		for(String s : animations.get(a)) {
			BlueprintAnimation anim = m.getBlueprint().getAnimations().get(s);
			if(anim == null) return;
			if(!m.getAnimationHandler().isPlayingAnimation(s)) return;
			m.getAnimationHandler().stopAnimation(s);
		}	
	}
}
