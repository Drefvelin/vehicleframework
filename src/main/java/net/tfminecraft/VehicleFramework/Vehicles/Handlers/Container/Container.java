package net.tfminecraft.VehicleFramework.Vehicles.Handlers.Container;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ticxo.modelengine.api.model.ActiveModel;
import com.ticxo.modelengine.api.model.bone.ModelBone;
import com.ticxo.modelengine.api.model.bone.behavior.BoneBehaviorType;
import com.ticxo.modelengine.core.mythic.mechanics.bone.PartVisibilityMechanic;

import de.tr7zw.nbtapi.NBT;
import de.tr7zw.nbtapi.iface.ReadWriteNBT;
import io.lumine.mythic.api.mobs.entities.MythicEntity;
import me.Plugins.TLibs.Objects.API.SubAPI.StringFormatter;
import net.tfminecraft.VehicleFramework.VFLogger;
import net.tfminecraft.VehicleFramework.Enums.VFGUI;
import net.tfminecraft.VehicleFramework.Managers.Inventory.VFInventoryHolder;
import net.tfminecraft.VehicleFramework.Vehicles.ActiveVehicle;

public class Container {

    private String id;
    private String name;
    private int size;

    //Visual stuff
    private List<String> boneList = new ArrayList<>();
    private List<ModelBone> bones = new ArrayList<>();

    //items
    private List<ItemStack> items = new ArrayList<>();

    public Container(String key, ConfigurationSection config) {
        id = key;
        name = StringFormatter.formatHex(config.getString("name", "Container"));
        size = config.getInt("size", 27);
        if(config.contains("bones")) {
            for(String s : config.getStringList("bones")) {
                boneList.add(s);
            }
        }
    }

    public Container(ActiveModel m, Container stored) {
        id = stored.getId();
        name = stored.getName();
        size = stored.getSize();
        for(String bone : stored.getBoneList()) {
            Optional<ModelBone> opt = m.getBone(bone);
            if(opt.isEmpty()) {
                VFLogger.log(m.getBlueprint().getName()+" has no bone called "+bone);
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

    public List<String> getBoneList() {
        return boneList;
    }

    public List<ModelBone> getBones() {
        return bones;
    }

    public List<ItemStack> getItems() {
        return items;
    }

    public void open(ActiveVehicle v, Player player) {
        // Create custom inventory with your holder
        Inventory inv = Bukkit.createInventory(
            new VFInventoryHolder(id, VFGUI.CONTAINER, v),
            size,
            name
        );

        // Copy item list into inventory
        for (int i = 0; i < Math.min(size, items.size()); i++) {
            inv.setItem(i, items.get(i));
        }

        player.openInventory(inv);
    }

    public void close(Inventory inventory) {
        items.clear();
        ItemStack[] contents = inventory.getContents();
        for (ItemStack item : contents) {
            items.add(item != null ? item.clone() : null); // clone to avoid referencing Bukkit internals
        }
        updateBoneVisibility();
    }

    public JsonObject getAsJson() {
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
        updateBoneVisibility();
    }

    public void updateBoneVisibility() {
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
    }

}
