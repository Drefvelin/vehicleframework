package net.tfminecraft.VehicleFramework.Vehicles.Handlers.Container;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ticxo.modelengine.api.model.bone.ModelBone;

import de.tr7zw.nbtapi.NBT;
import de.tr7zw.nbtapi.iface.ReadWriteNBT;
import me.Plugins.TLibs.TLibs;
import me.Plugins.TLibs.Objects.API.SubAPI.StringFormatter;
import net.tfminecraft.VehicleFramework.VFLogger;
import net.tfminecraft.VehicleFramework.Enums.VFGUI;
import net.tfminecraft.VehicleFramework.Managers.Inventory.VFInventoryHolder;
import net.tfminecraft.VehicleFramework.Vehicles.ActiveVehicle;

public class Container {

    private String id;
    private String name;
    private int size;
    private String seat;

    //Visual stuff
    private List<String> boneList = new ArrayList<>();
    private List<ModelBone> bones = new ArrayList<>();

    //items
    private List<ItemStack> items = new ArrayList<>();
    private List<String> allowItems = new ArrayList<>();
    private Inventory live;

    public Container(String key, ConfigurationSection config) {
        id = key;
        name = StringFormatter.formatHex(config.getString("name", "Container"));
        size = config.getInt("size", 27);
        seat = config.getString("seat", "none");
        if(config.contains("bones")) {
            for(String s : config.getStringList("bones")) {
                boneList.add(s);
            }
        }
        if (config.contains("allow-items")) {
            for (String path : config.getStringList("allow-items")) {
                if (path != null && !path.isBlank()) {
                    allowItems.add(path);
                }
            }
        }
    }

    public Container(ActiveVehicle v, Container stored) {
        id = stored.getId();
        name = stored.getName();
        size = stored.getSize();
        seat = stored.getSeat();
        allowItems.addAll(stored.getAllowItems());
        for(String bone : stored.getBoneList()) {
            Optional<ModelBone> opt = v.getModel().getBone(bone);
            if(opt.isEmpty()) {
                VFLogger.log(v.getModel().getBlueprint().getName()+" has no bone called "+bone);
                continue;
            }
            bones.add(opt.get());
        }
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getSize() {
        return size;
    }

    public String getSeat() {
        return seat;
    }

    public List<String> getBoneList() {
        return boneList;
    }

    public List<ModelBone> getBones() {
        return bones;
    }

    public List<ItemStack> getItems() {
        return items;
    }

    public List<String> getAllowItems() {
        return allowItems;
    }

    public boolean allows(ItemStack item) {
        return decideAllow(allowItems.isEmpty(), isEmpty(item), matchesAllowList(item));
    }

    static boolean decideAllow(boolean emptyList, boolean itemEmpty, boolean pathHit) {
        if (itemEmpty) {
            return true;
        }
        if (emptyList) {
            return true;
        }
        return pathHit;
    }

    private boolean matchesAllowList(ItemStack item) {
        for (String path : allowItems) {
            try {
                if (TLibs.getItemAPI().getChecker().checkItemWithPath(item, path)) {
                    return true;
                }
            } catch (Exception ignored) {
            }
        }
        return false;
    }

    public ItemStack takeOneMatching(String tlibsPath) {
        if (tlibsPath == null || tlibsPath.isBlank()) {
            return null;
        }
        int slots = live != null ? live.getSize() : items.size();
        for (int i = 0; i < slots; i++) {
            ItemStack stack = live != null ? live.getItem(i) : (i < items.size() ? items.get(i) : null);
            if (isEmpty(stack)) {
                continue;
            }
            try {
                if (!TLibs.getItemAPI().getChecker().checkItemWithPath(stack, tlibsPath)) {
                    continue;
                }
            } catch (Exception ignored) {
                continue;
            }
            ItemStack one = stack.clone();
            one.setAmount(1);
            int left = stack.getAmount() - 1;
            ItemStack remain = left <= 0 ? null : stack;
            if (remain != null) {
                remain.setAmount(left);
            }
            if (live != null) {
                live.setItem(i, remain);
            } else {
                items.set(i, remain);
            }
            pullLive();
            updateBoneVisibility();
            return one;
        }
        return null;
    }

    private static boolean isEmpty(ItemStack item) {
        return item == null || item.getType().isAir();
    }

    public void giveOrDrop(Player player, ItemStack item) {
        if (isEmpty(item) || player == null) {
            return;
        }
        HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(item.clone());
        if (leftover.isEmpty() || player.getWorld() == null) {
            return;
        }
        for (ItemStack extra : leftover.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), extra);
        }
    }

    public void stripDisallowed(Inventory inventory, Player player) {
        if (inventory == null || allowItems.isEmpty()) {
            return;
        }
        for (int i = 0; i < inventory.getSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (allows(stack)) {
                continue;
            }
            inventory.setItem(i, null);
            giveOrDrop(player, stack);
        }
    }

    public void open(ActiveVehicle v, Player player) {
        player.openInventory(ensureLive(v));
    }

    public void close(Inventory inventory) {
        if (inventory == null) {
            return;
        }
        if (live != null && inventory == live) {
            pullLive();
        } else {
            items.clear();
            ItemStack[] contents = inventory.getContents();
            for (ItemStack item : contents) {
                items.add(item != null ? item.clone() : null);
            }
            pushLive();
        }
        updateBoneVisibility();
    }

    private Inventory ensureLive(ActiveVehicle v) {
        if (live != null) {
            return live;
        }
        live = Bukkit.createInventory(
                new VFInventoryHolder(id, VFGUI.CONTAINER, v),
                size,
                name);
        pushLive();
        return live;
    }

    private void pullLive() {
        if (live == null) {
            return;
        }
        items.clear();
        for (ItemStack item : live.getContents()) {
            items.add(item != null ? item.clone() : null);
        }
    }

    private void pushLive() {
        if (live == null) {
            return;
        }
        for (int i = 0; i < live.getSize(); i++) {
            live.setItem(i, i < items.size() ? items.get(i) : null);
        }
    }

    public JsonObject getAsJson() {
        pullLive();
        JsonObject root = new JsonObject();
        root.addProperty("id", id);

        JsonArray itemsArray = new JsonArray();

        for (int i = 0; i < items.size(); i++) {
            ItemStack stack = items.get(i);
            if (stack == null || stack.getType().isAir()) continue;

            ReadWriteNBT nbt = NBT.itemStackToNBT(stack); // Full item representation

            JsonArray entry = new JsonArray();
            entry.add(i); // slot index
            entry.add(JsonParser.parseString(nbt.toString())); // SNBT to JSON

            itemsArray.add(entry);
        }

        root.add("items", itemsArray);
        return root;
    }

    public void loadFromJson(JsonObject json) {

        items.clear();
        for (int i = 0; i < size; i++) {
            items.add(null);
        }

        if (!json.has("items")) return;
        JsonArray itemsArray = json.getAsJsonArray("items");

        for (JsonElement elem : itemsArray) {
            if (!elem.isJsonArray()) continue;
            JsonArray entry = elem.getAsJsonArray();
            if (entry.size() != 2) continue;

            int slot = entry.get(0).getAsInt();
            String snbt = entry.get(1).toString(); // SNBT string

            try {
                ReadWriteNBT nbt = NBT.parseNBT(snbt);
                ItemStack stack = NBT.itemStackFromNBT(nbt);

                if (slot >= 0 && slot < size) {
                    items.set(slot, stack);
                }
            } catch (Exception e) {
                e.printStackTrace(); // or log cleanly
            }
        }
        pushLive();
        updateBoneVisibility();
    }

    public void updateBoneVisibility() {
        /*
        if (bones.isEmpty()) {
            VFLogger.creatorLog("No bones to update for container " + id);
            return;
        }

        VFLogger.creatorLog("Updating bone visibility for container: " + id);

        // Debug: size vs items
        VFLogger.creatorLog("Container size: " + size + ", items.size(): " + items.size());

        int filledSlots = 0;
        for (int i = 0; i < items.size(); i++) {
            ItemStack item = items.get(i);
            boolean filled = item != null && !item.getType().isAir();
            VFLogger.creatorLog("Slot " + i + ": " + (filled ? item.getType() : "empty"));
            if (filled) filledSlots++;
        }

        double fillRatio = size == 0 ? 0.0 : (double) filledSlots / size;
        VFLogger.creatorLog("Filled slots: " + filledSlots + "/" + size + " => fillRatio: " + fillRatio);

        int visibleCount = (int) Math.round(fillRatio * bones.size());
        VFLogger.creatorLog("Showing " + visibleCount + " of " + bones.size() + " bones");

        for (int i = 0; i < bones.size(); i++) {
            boolean visible = i < visibleCount;
            ModelBone bone = bones.get(i);
            bone.setVisible(visible);
            VFLogger.creatorLog("Bone " + i + " (" + bones.get(i).getBoneId() + "): " + (visible ? "VISIBLE" : "HIDDEN"));
        }
        */
    }

    public void destroy(Location loc) {
        pullLive();
        for(ItemStack item : items) {
            loc.getWorld().dropItem(loc, item);
        }
    }

}
