package net.tfminecraft.VehicleFramework.Managers;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import net.tfminecraft.VehicleFramework.Enums.VFGUI;
import net.tfminecraft.VehicleFramework.Managers.Inventory.VFInventoryHolder;
import net.tfminecraft.VehicleFramework.VehicleFramework;
import net.tfminecraft.VehicleFramework.Vehicles.ActiveVehicle;

public class OwnershipGUIManager {

    // ── Ownership Settings GUI ──────────────────────────────────────────────

    public void ownershipGui(Inventory i, Player p, ActiveVehicle v, boolean open) {
        if (open) {
            i = VehicleFramework.plugin.getServer().createInventory(
                new VFInventoryHolder(v.getUUID(), VFGUI.OWNERSHIP, v), 27, "§7Ownership Settings");
        }

        // Slot 0 – Toggle whitelisting
        i.setItem(0, createToggleWhitelistButton(v.getOwnerData().isWhiteListed()));
        // Slot 2 – Add player to whitelist
        i.setItem(2, createAddToWhitelistButton());
        // Slot 4 – View / manage whitelist
        i.setItem(4, createViewWhitelistButton(v.getOwnerData().getWhiteList().size()));
        // Slot 6 – Remove ownership
        i.setItem(6, createRemoveOwnershipButton());

        fillGlass(i);

        if (open) p.openInventory(i);
    }

    // ── Whitelist GUI ────────────────────────────────────────────────────────

    public void whitelistGui(Inventory i, Player p, ActiveVehicle v, boolean open) {
        if (open) {
            i = VehicleFramework.plugin.getServer().createInventory(
                new VFInventoryHolder(v.getUUID(), VFGUI.WHITELIST, v), 27, "§7Whitelist");
        }

        i.clear();

        int slot = 0;
        for (String entry : v.getOwnerData().getWhiteList()) {
            if (slot >= 26) break; // slot 26 is the back button
            i.setItem(slot, createWhitelistEntry(entry));
            slot++;
        }

        // Slot 26 – Back button
        i.setItem(26, createBackButton());

        fillGlass(i);

        if (open) p.openInventory(i);
    }

    // ── Seat-selection ownership button (shown to the owner) ─────────────────

    public ItemStack createOwnershipButton() {
        ItemStack i = new ItemStack(Material.GOLDEN_HELMET);
        ItemMeta m = i.getItemMeta();
        m.setDisplayName("§6Ownership Settings");
        List<String> lore = new ArrayList<>();
        lore.add("§7Click to manage ownership");
        m.setLore(lore);
        i.setItemMeta(m);
        return i;
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    private ItemStack createToggleWhitelistButton(boolean enabled) {
        ItemStack i = new ItemStack(enabled ? Material.LIME_DYE : Material.GRAY_DYE);
        ItemMeta m = i.getItemMeta();
        m.setDisplayName(enabled ? "§aWhitelisting: ON" : "§7Whitelisting: OFF");
        List<String> lore = new ArrayList<>();
        lore.add("§7When enabled only whitelisted players can enter");
        lore.add("");
        lore.add("§eClick to toggle");
        m.setLore(lore);
        i.setItemMeta(m);
        return i;
    }

    private ItemStack createAddToWhitelistButton() {
        ItemStack i = new ItemStack(Material.WRITABLE_BOOK);
        ItemMeta m = i.getItemMeta();
        m.setDisplayName("§aAdd to Whitelist");
        List<String> lore = new ArrayList<>();
        lore.add("§7Click, then type a player name in chat");
        m.setLore(lore);
        i.setItemMeta(m);
        return i;
    }

    private ItemStack createViewWhitelistButton(int count) {
        ItemStack i = new ItemStack(Material.CHEST);
        ItemMeta m = i.getItemMeta();
        m.setDisplayName("§eView Whitelist");
        List<String> lore = new ArrayList<>();
        lore.add("§7Players on whitelist: §f" + count);
        lore.add("§7Click to manage whitelist");
        m.setLore(lore);
        i.setItemMeta(m);
        return i;
    }

    private ItemStack createRemoveOwnershipButton() {
        ItemStack i = new ItemStack(Material.BARRIER);
        ItemMeta m = i.getItemMeta();
        m.setDisplayName("§cRemove Ownership");
        List<String> lore = new ArrayList<>();
        lore.add("§7Resets the owner of this vehicle to none");
        m.setLore(lore);
        i.setItemMeta(m);
        return i;
    }

    private ItemStack createWhitelistEntry(String storedName) {
        ItemStack i = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta m = i.getItemMeta();
        String display = storedName.startsWith("player_") ? storedName.substring(7) : storedName;
        m.setDisplayName("§e" + display);
        List<String> lore = new ArrayList<>();
        lore.add("§cClick to remove from whitelist");
        m.setLore(lore);
        NamespacedKey key = new NamespacedKey(VehicleFramework.plugin, "vf_whitelist_entry");
        m.getPersistentDataContainer().set(key, PersistentDataType.STRING, storedName);
        i.setItemMeta(m);
        return i;
    }

    private ItemStack createBackButton() {
        ItemStack i = new ItemStack(Material.BARRIER);
        ItemMeta m = i.getItemMeta();
        m.setDisplayName("§7Back");
        i.setItemMeta(m);
        return i;
    }

    private void fillGlass(Inventory i) {
        for (int s = 0; s < i.getSize(); s++) {
            if (i.getItem(s) == null) {
                ItemStack fill = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
                ItemMeta fm = fill.getItemMeta();
                fm.setDisplayName("§8 ");
                fill.setItemMeta(fm);
                i.setItem(s, fill);
            }
        }
    }
}
