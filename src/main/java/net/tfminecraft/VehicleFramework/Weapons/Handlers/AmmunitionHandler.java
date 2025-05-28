package net.tfminecraft.VehicleFramework.Weapons.Handlers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import com.ticxo.modelengine.api.model.ActiveModel;
import io.lumine.mythic.bukkit.utils.lib.lang3.text.WordUtils;
import me.Plugins.TLibs.TLibs;
import me.Plugins.TLibs.Enums.APIType;
import me.Plugins.TLibs.Objects.API.ItemAPI;
import net.tfminecraft.VehicleFramework.VFLogger;
import net.tfminecraft.VehicleFramework.VehicleFramework;
import net.tfminecraft.VehicleFramework.Bones.VectorBone;
import net.tfminecraft.VehicleFramework.Data.ParticleData;
import net.tfminecraft.VehicleFramework.Data.SoundData;
import net.tfminecraft.VehicleFramework.Enums.Animation;
import net.tfminecraft.VehicleFramework.Enums.SoundArg;
import net.tfminecraft.VehicleFramework.Enums.State;
import net.tfminecraft.VehicleFramework.Loaders.AmmunitionLoader;
import net.tfminecraft.VehicleFramework.Vehicles.ActiveVehicle;
import net.tfminecraft.VehicleFramework.Weapons.ActiveWeapon;
import net.tfminecraft.VehicleFramework.Weapons.Ammunition.Ammunition;
import net.tfminecraft.VehicleFramework.Weapons.Shooter.ProjectileShooter;

public class AmmunitionHandler {
	private ProjectileShooter projectileShooter = new ProjectileShooter();
	
	private ActiveWeapon w;
	private ActiveVehicle v;
	
	private int count;
	private Ammunition ammo;
	private List<String> acceptedAmmunition = new ArrayList<>();
	
	private int baseReloadTime;
	private int reloadTime;

	private int cooldown;
	
	private long activeCooldown;
	
	private int currentBone;
	private List<VectorBone> exitBones = new ArrayList<>();
	
	private List<State> reloadStates = new ArrayList<>();
	
	@SuppressWarnings("unchecked")
	public AmmunitionHandler(ConfigurationSection config) {
		acceptedAmmunition = (List<String>) config.getList("accepted-ammunition", new ArrayList<String>());
		baseReloadTime = config.getInt("reload-time", 4);
		cooldown = config.getInt("cooldown", 10);
		if(config.contains("reload-states")) {
			for(String s : config.getStringList("reload-states")) {
				try {
		            reloadStates.add(State.valueOf(s.toUpperCase()));
		        } catch (IllegalArgumentException e) {
		            System.out.println("Invalid state in reload-states: " + s);
		        }
			}
		} else {
			reloadStates = new ArrayList<>(Arrays.asList(State.values()));
		}
	}
	
	public AmmunitionHandler(ActiveModel m, ActiveVehicle vehicle, ActiveWeapon weapon, AmmunitionHandler another, List<String> bones) {
		w = weapon;
		v = vehicle;
		count = 0;
		ammo = null;
		acceptedAmmunition = another.getAcceptedAmmunition();
		reloadTime = -1;
		baseReloadTime = another.getBaseReloadTime();
		cooldown = another.getCooldown();
		activeCooldown = 0L;
		currentBone = 0;
		reloadStates = another.getReloadStates();
		for(String bone : bones) {
			String base = bone.split("\\.")[0];
			String alignment = bone.split("\\.")[1];
			if(m.getBone(base).isEmpty()) VFLogger.log(weapon.getId()+" has an invalid exit bone");
			if(m.getBone(alignment).isEmpty()) VFLogger.log(weapon.getId()+" has an invalid alignment bone");
			exitBones.add(new VectorBone(m.getBone(base).get(), m.getBone(alignment).get()));
		}
	}

	public void setAmmo(Ammunition a, int i) {
		ammo = a;
		count = i;
	}
	
	public void updateModel(ActiveModel m) {
		for(VectorBone bone : exitBones) {
			bone.updateModel(m);
		}
	}

	public int getCount() {
		return count;
	}
	
	public boolean hasAmmo() {
		return ammo != null;
	}

	public Ammunition getAmmo() {
		return ammo;
	}

	public List<String> getAcceptedAmmunition() {
		return acceptedAmmunition;
	}
	
	public int getBaseReloadTime() {
		return baseReloadTime;
	}
	
	public int getReloadTime() {
		return reloadTime;
	}

	public int getCooldown() {
		return cooldown;
	}

	public long getActiveCooldown() {
		return activeCooldown;
	}

	public int getAmmoAmount() {
		return exitBones.size();
	}
	
	public List<State> getReloadStates() {
		return reloadStates;
	}
	
	private void playSound(VectorBone bone, SoundArg arg) {
		List<SoundData> sounds = w.getWeaponData().getSounds(arg);
		for(SoundData sound : sounds) {
			sound.playSound(null, bone.getBaseLocation(), 1f);
		}
	}
	
	private void particle(VectorBone bone) {
		for(ParticleData pd : w.getWeaponData().getParticles()) {
        	pd.spawnParticle(bone.getBaseLocation(), bone.getVector());
        }
	}
	
	private void reload(Player p, Ammunition a) {
		p.sendTitle("", "§aReloaded!", 0, 10, 4);
		p.sendMessage("§aLoaded ammunition: §e"+WordUtils.capitalize(new String(a.getId()).replace("_", " ")));
		ammo = a;
		count = a.getData().getRounds()*exitBones.size();
		playSound(exitBones.get(0), SoundArg.RELOAD);
	}
	
	public void reloadStart(Player p, Ammunition a) {
		reloadTime = baseReloadTime;
		playSound(exitBones.get(0), SoundArg.RELOAD_START);
		w.getAnimationHandler().animate(Animation.RELOAD);
		new BukkitRunnable() {
	        @Override
	        public void run() {
	        	if(reloadTime == 0) {
	        		reloadTime = -1;
	        		reload(p, a);
	        		cancel();
	        		return;
	        	}
	        	p.sendTitle("", "§aReloading: §e"+reloadTime+"s", 0, 25, 0);
	        	reloadTime--;
	        }
	    }.runTaskTimer(VehicleFramework.plugin, 0L, 20L);
	}
	
	public void load(Player p, ItemStack i, boolean message) {
		if(i.getType().equals(Material.AIR)) return;
		ItemAPI api = (ItemAPI) TLibs.getApiInstance(APIType.ITEM_API);
		String input = api.getChecker().getAsStringPath(i);
		if(AmmunitionLoader.getByInput(input) == null) return;
		Ammunition a = AmmunitionLoader.getByInput(input);
		if(!reloadStates.contains(v.getCurrentState().getType())) {
			if(message) p.sendMessage("§cCannot reload in the "+v.getCurrentState().getType().toString().toLowerCase().replace("_", " ")+ " state");
			return;
		}
		if(hasAmmo()) {
			if(message) p.sendMessage("§eWeapon already loaded");
			return;
		}
		if(!acceptedAmmunition.contains(a.getId())) {
			p.sendMessage("§eCannot use this ammunition for this weapon");
			return;
		}
		int count = getAmmoAmount();
		if(i.getAmount() < count) {
			p.sendMessage("§eWeapon needs "+count+" ammo items per reload");
			return;
		}
		if(reloadTime != -1) {
			if(message) p.sendMessage("§eAlready reloading");
			return;
		}
		i.setAmount(i.getAmount()-count);
		reloadStart(p, a);
	}
	
	private void shootProjectiles(List<Player> nearby) {
		if(exitBones.size() > 1 && currentBone >= exitBones.size()) {
			currentBone = 0;
		}
		if(w.getAnimationHandler().get(Animation.SHOOT).size() > 1) {
			w.getAnimationHandler().animate(Animation.SHOOT, currentBone);
		} else {
			w.getAnimationHandler().animate(Animation.SHOOT);
		}
		VectorBone bone = exitBones.get(currentBone);
        projectileShooter.shoot(nearby, v.getEntity(), bone.getBaseLocation(), bone.getVector(), ammo, w);
        particle(bone);
        playSound(bone, SoundArg.SHOOT);
        if(exitBones.size() > 1) currentBone++;
	}
	private void decrease() {
		count--;
		if(count == 0) {
			ammo = null;
			activeCooldown = 0L;
		}
		v.updateBoard();
	}
	public void shoot(Player p, List<Player> nearby) {
		if(!hasAmmo()) {
			p.sendMessage("§cNo ammo");
			return;
		}
		if(activeCooldown > System.currentTimeMillis()) {
			return;
		}
		activeCooldown = System.currentTimeMillis()+cooldown*50;
		shootProjectiles(nearby);
		decrease();
	}
	
}
