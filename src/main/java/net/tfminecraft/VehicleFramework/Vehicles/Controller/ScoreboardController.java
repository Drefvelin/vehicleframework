package net.tfminecraft.VehicleFramework.Vehicles.Controller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;

import io.lumine.mythic.bukkit.utils.lib.lang3.text.WordUtils;
import net.tfminecraft.VehicleFramework.Enums.Component;
import net.tfminecraft.VehicleFramework.Vehicles.ActiveVehicle;
import net.tfminecraft.VehicleFramework.Vehicles.Component.Engine;
import net.tfminecraft.VehicleFramework.Vehicles.Component.GearedEngine;
import net.tfminecraft.VehicleFramework.Vehicles.Component.Hull;
import net.tfminecraft.VehicleFramework.Vehicles.Component.Pump;
import net.tfminecraft.VehicleFramework.Vehicles.Component.SinkableHull;
import net.tfminecraft.VehicleFramework.Vehicles.Component.VehicleComponent;
import net.tfminecraft.VehicleFramework.Weapons.ActiveWeapon;
import net.tfminecraft.VehicleFramework.Weapons.Handlers.AmmunitionHandler;

public class ScoreboardController {
	
	private ActiveVehicle vehicle;
	
	public ScoreboardController(ActiveVehicle v) {
		vehicle = v;
	}
	
	public void scoreboard(Player p) {
		ScoreboardManager manager = Bukkit.getScoreboardManager();
		Scoreboard board = manager.getNewScoreboard();
		Objective obj = board.registerNewObjective("vehicleDummy", Criteria.DUMMY, vehicle.getName());
		obj.setDisplaySlot(DisplaySlot.SIDEBAR);
		List<String> scores = new ArrayList<>();
		scores.add("§fType: §e"+WordUtils.capitalize(new String(vehicle.getId()).replace("_", " ")));
		int i = 0;
		for(VehicleComponent c : vehicle.getComponents()) {
			scores.add("§e"+c.getAlias()+":");
			if(c.isOnFire()) {
				scores.add(c.getFire().getFireString()+" §"+i);
			}
			scores.add("§f- "+c.getHealthData().getHealthPercentageString()+" §"+i);
			if(c instanceof Engine) {
				Engine engine = (Engine) vehicle.getComponent(Component.ENGINE);
				scores.add("§f- Throttle: §e"+engine.getThrottle().getCurrent()+"%");
				if(engine.getFuelTank().useFuel()) scores.add("§f- Fuel: §e"+engine.getFuelTank().getPercentage()+"%");
			} else if(c instanceof Pump) {
				Pump pump = (Pump) vehicle.getComponent(Component.PUMP);
				scores.add("§f- Power: §e"+pump.getPower());
			} else if(c instanceof GearedEngine) {
				GearedEngine engine = (GearedEngine) vehicle.getComponent(Component.GEARED_ENGINE);
				scores.add("§f- Gear: §e"+engine.getGear().getName());
				scores.add("§f- Throttle: §e"+engine.getGear().getThrottle().getCurrent()+"%");
				scores.add("§f- Fuel: §e"+engine.getFuelTank().getPercentage()+"%");
			}
			i++;
		}
		if(vehicle.hasComponent(Component.HULL)) {
			if(vehicle.getComponent(Component.HULL) instanceof SinkableHull) {
				SinkableHull sHull = (SinkableHull) vehicle.getComponent(Component.HULL);
				if(sHull.hasSinkProgress()) {
					scores.add("§9SINKING!! §b"+sHull.getSinkProgress()+"% §1");
				}
			}
		}
		if(vehicle.isPassenger(p, false) && vehicle.getSeat(p).hasWeapon()) {
			ActiveWeapon w = vehicle.getSeat(p).getWeapon();
			scores.add("§eWeapon: §7"+w.getName());
			AmmunitionHandler h = w.getAmmunitionHandler();
			if(h.hasAmmo()) {
				if(h.getCount() > 0) scores.add(h.getAmmo().getName()+" §fx"+h.getCount());
			}
		}
		Collections.reverse(scores);
		i = 0;
		for(String score : scores) {
			Score s = obj.getScore(score);
			s.setScore(i);
			i++;
		}
		Score scoreDivider = obj.getScore(ChatColor.RED + "=-=-=-=-=-=-=-=-=-=-=");
		scoreDivider.setScore(i);
		p.setScoreboard(board);
	}
	
	public void removeScoreboard(Player p) {
		Scoreboard board = p.getScoreboard();
		Objective obj = board.getObjective("vehicleDummy");
		if(obj != null) obj.unregister();
	}
}
