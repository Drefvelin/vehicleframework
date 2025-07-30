package net.tfminecraft.VehicleFramework.Managers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import com.comphenix.net.bytebuddy.dynamic.TypeResolutionStrategy.Active;

import io.lumine.mythic.bukkit.utils.lib.lang3.text.WordUtils;
import net.tfminecraft.VehicleFramework.VehicleFramework;
import net.tfminecraft.VehicleFramework.Enums.VFGUI;
import net.tfminecraft.VehicleFramework.Managers.Inventory.VFInventoryHolder;
import net.tfminecraft.VehicleFramework.Vehicles.ActiveVehicle;
import net.tfminecraft.VehicleFramework.Vehicles.Component.Engine;
import net.tfminecraft.VehicleFramework.Vehicles.Component.GearedEngine;
import net.tfminecraft.VehicleFramework.Vehicles.Component.Harness;
import net.tfminecraft.VehicleFramework.Vehicles.Component.Hull;
import net.tfminecraft.VehicleFramework.Vehicles.Component.Pump;
import net.tfminecraft.VehicleFramework.Vehicles.Component.VehicleComponent;
import net.tfminecraft.VehicleFramework.Vehicles.Component.Wings;
import net.tfminecraft.VehicleFramework.Vehicles.Handlers.Container.Container;
import net.tfminecraft.VehicleFramework.Vehicles.Handlers.Skins.VehicleSkin;
import net.tfminecraft.VehicleFramework.Vehicles.Seat.Seat;
import net.tfminecraft.VehicleFramework.Weapons.ActiveWeapon;

public class InventoryManager {
	//Seat Selector
	public void seatSelection(Inventory i, Player p, ActiveVehicle v, boolean open) {
		if(open) {
			i = VehicleFramework.plugin.getServer().createInventory(new VFInventoryHolder(v.getUUID(), VFGUI.SEAT_SELECTION), 27, "§7Select Seat");
		}
		int x = 0;
		for(Seat seat : v.getSeatHandler().getSeats()) {
			i.setItem(x, getSeatItem(v, seat));
			x++;
		}
		if(v.isPassenger(p, false)) i.setItem(26, createDismountButton());
		int slotn = 0;
		while(slotn < i.getSize()) {
			if(i.getItem(slotn) == null) {
				ItemStack fill = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
				ItemMeta fm = fill.getItemMeta();
				fm.setDisplayName("§8 ");
				fill.setItemMeta(fm);
				i.setItem(slotn, fill);
			}
			slotn++;
		}
		if(open) {
			p.openInventory(i);
		}
	}
	
	private ItemStack getSeatItem(ActiveVehicle v, Seat s) {
		ItemStack i = new ItemStack(Material.GREEN_CONCRETE, 1);
		if(s.isOccupied()) {
			i = new ItemStack(Material.YELLOW_CONCRETE, 1);
		}
		ItemMeta m = i.getItemMeta();
		if(s.isOccupied()) {
			m.setDisplayName("§e"+WordUtils.capitalize(new String(s.getBone())).replace("_", " "));
		} else {
			m.setDisplayName("§a"+WordUtils.capitalize(new String(s.getBone())).replace("_", " "));
		}
		NamespacedKey key = new NamespacedKey(VehicleFramework.plugin, "vf_seat_id");
		m.getPersistentDataContainer().set(key, PersistentDataType.STRING, s.getBone());
		List<String> lore = new ArrayList<>();
		lore.add("§7Type: §e"+WordUtils.capitalize(s.getType().toString().toLowerCase()));
		if(v.hasContainers()) {
			Container c = v.getContainerHandler().getBySeat(s.getBone());
			if(c != null) {
				lore.add("");
				lore.add("§7Container: §f"+c.getName());
			}
		}
		m.setLore(lore);
		i.setItemMeta(m);
		return i;
	}
	private ItemStack createDismountButton() {
		ItemStack i = new ItemStack(Material.BARRIER, 1);
		ItemMeta m = i.getItemMeta();
		m.setDisplayName("§cDismount");
		i.setItemMeta(m);
		return i;
	}
	
	//Repair Window
	public void repairWindow(Inventory i, Player p, ActiveVehicle v, boolean open, String tool) {
		if(open) {
			i = VehicleFramework.plugin.getServer().createInventory(new VFInventoryHolder(v.getUUID(), VFGUI.REPAIR), 27, "§7Repair Vehicle");
		}
		if(v != null) {
			List<Integer> locked = Arrays.asList(8, 17);
			int x = 0;
			for(VehicleComponent c : v.getComponents()) {
				while(locked.contains(x)) x++;
				i.setItem(x, getComponentItem(c));
				x++;
			}
			for(ActiveWeapon weapon : v.getWeaponHandler().getWeapons()) {
				while(locked.contains(x)) x++;
				i.setItem(x, getWeaponItem(weapon));
				x++;
			}
		}
		i.setItem(8, getRepairItem(tool));
		i.setItem(17, getWaterItem(tool));
		int slotn = 0;
		while(slotn < i.getSize()) {
			if(i.getItem(slotn) == null) {
				ItemStack fill = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
				ItemMeta fm = fill.getItemMeta();
				fm.setDisplayName("§8 ");
				fill.setItemMeta(fm);
				i.setItem(slotn, fill);
			}
			slotn++;
		}
		if(open) {
			p.openInventory(i);
		}
	}
	private ItemStack getRepairItem(String tool) {
		ItemStack i = new ItemStack(Material.IRON_SHOVEL, 1);
		ItemMeta m = i.getItemMeta();
		m.setDisplayName("§7Repair Tool");
		NamespacedKey key = new NamespacedKey(VehicleFramework.plugin, "wm_tool_type");
		m.getPersistentDataContainer().set(key, PersistentDataType.STRING, "repair");
		List<String> lore = new ArrayList<>();
		if(tool.equalsIgnoreCase("repair")) {
			lore.add("§aSelected");
			m.addEnchant(Enchantment.DURABILITY, 1, true);
			m.addItemFlags(ItemFlag.HIDE_ENCHANTS);
		} else {
			lore.add("§eClick to Select");
		}
		
		lore.add("§7Used to repair damage to components");
		lore.add(" ");
		lore.add("§fClick on components to repair them");
		
		m.setLore(lore);
		i.setItemMeta(m);
		return i;
	}
	private ItemStack getWaterItem(String tool) {
		ItemStack i = new ItemStack(Material.WATER_BUCKET, 1);
		ItemMeta m = i.getItemMeta();
		m.setDisplayName("§7Water Bucket");
		NamespacedKey key = new NamespacedKey(VehicleFramework.plugin, "vf_tool_type");
		m.getPersistentDataContainer().set(key, PersistentDataType.STRING, "water");
		List<String> lore = new ArrayList<>();
		if(tool.equalsIgnoreCase("water")) {
			lore.add("§aSelected");
			m.addEnchant(Enchantment.DURABILITY, 1, true);
			m.addItemFlags(ItemFlag.HIDE_ENCHANTS);
		} else {
			lore.add("§eClick to Select");
		}
		
		lore.add("§7Used to put out fires");
		lore.add(" ");
		lore.add("§fClick on components to put out fires");
		
		m.setLore(lore);
		i.setItemMeta(m);
		return i;
	}
	private ItemStack getComponentItem(VehicleComponent c) {
		ItemStack i = new ItemStack(Material.GREEN_CONCRETE, 1);
		String type = "none";
		if(c instanceof Hull) {
			i.setType(Material.NETHERITE_BLOCK);
			type = "hull";
		} else if(c instanceof Engine) {
			i.setType(Material.BLAST_FURNACE);
			type = "engine";
		} else if(c instanceof GearedEngine) {
			i.setType(Material.FURNACE);
			type = "engine";
		} else if(c instanceof Pump) {
			i.setType(Material.BREWING_STAND);
			type = "pump";
		} else if(c instanceof Wings) {
			i.setType(Material.WHITE_WOOL);
			type = "pump";
		} else if(c instanceof Harness) {
			i.setType(Material.LEAD);
			type = "pump";
		} else {
			type = c.getType().toString().toLowerCase();
		}
		ItemMeta m = i.getItemMeta();
		m.setDisplayName("§7"+WordUtils.capitalize(new String(type)));
		NamespacedKey key = new NamespacedKey(VehicleFramework.plugin, "vf_component_type");
		m.getPersistentDataContainer().set(key, PersistentDataType.STRING, type);
		List<String> lore = new ArrayList<>();
		lore.add(c.getHealthData().getHealthPercentageString());
		if(c.isOnFire()) {
			lore.add(" ");
			lore.add(c.getFire().getFireString());
		}
		if(c.getHealthData().isUnderRepair()) {
			lore.add(" ");
			lore.add("§aRepairing: §e"+c.getHealthData().getRepairString());
		}
		m.setLore(lore);
		i.setItemMeta(m);
		return i;
	}
	private ItemStack getWeaponItem(ActiveWeapon w) {
		ItemStack i = new ItemStack(Material.IRON_BLOCK, 1);
		ItemMeta m = i.getItemMeta();
		m.setDisplayName(w.getName());
		NamespacedKey key = new NamespacedKey(VehicleFramework.plugin, "vf_weapon_repair_type");
		m.getPersistentDataContainer().set(key, PersistentDataType.STRING, w.getId());
		List<String> lore = new ArrayList<>();
		lore.add(w.getHealthData().getHealthPercentageString());
		if(w.getHealthData().isUnderRepair()) {
			lore.add(" ");
			lore.add("§aRepairing: §e"+w.getHealthData().getRepairString());
		}
		m.setLore(lore);
		i.setItemMeta(m);
		return i;
	}
	
	//Skin Selector
	public void skinSelection(Inventory i, Player p, ActiveVehicle v, boolean open) {
		if(open) {
			i = VehicleFramework.plugin.getServer().createInventory(new VFInventoryHolder(v.getUUID(), VFGUI.SKIN_SELECTION), 27, "§7Select Skin");
		}
		int x = 0;
		for(VehicleSkin skin : v.getSkinHandler().getSkins().values()) {
			i.setItem(x, getSkinItem(skin, v.getSkinHandler().getCurrentSkin().getId()));
			x++;
		}
		//i.setItem(26, createDismountButton());
		int slotn = 0;
		while(slotn < i.getSize()) {
			if(i.getItem(slotn) == null) {
				ItemStack fill = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
				ItemMeta fm = fill.getItemMeta();
				fm.setDisplayName("§8 ");
				fill.setItemMeta(fm);
				i.setItem(slotn, fill);
			}
			slotn++;
		}
		if(open) {
			p.openInventory(i);
		}
	}
		
	private ItemStack getSkinItem(VehicleSkin s, String active) {
		ItemStack i = new ItemStack(Material.GREEN_CONCRETE, 1);
		if(s.getId().equalsIgnoreCase(active)) {
			i = new ItemStack(Material.YELLOW_CONCRETE, 1);
		}
		ItemMeta m = i.getItemMeta();
		if(s.getId().equalsIgnoreCase(active)) {
			m.setDisplayName("§e"+s.getName());
		} else {
			m.setDisplayName("§a"+s.getName());
		}
		NamespacedKey key = new NamespacedKey(VehicleFramework.plugin, "vf_skin_id");
		m.getPersistentDataContainer().set(key, PersistentDataType.STRING, s.getId());
		i.setItemMeta(m);
		return i;
	}
}
