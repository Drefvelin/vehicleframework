package net.tfminecraft.VehicleFramework.Vehicles.Handlers.Container;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import com.ticxo.modelengine.api.model.ActiveModel;

import net.tfminecraft.VehicleFramework.VehicleFramework;
import net.tfminecraft.VehicleFramework.Enums.VFGUI;
import net.tfminecraft.VehicleFramework.Managers.Inventory.VFInventoryHolder;
import net.tfminecraft.VehicleFramework.Vehicles.ActiveVehicle;

public class ContainerHandler {

    private ActiveVehicle vehicle;
    private List<Container> containers = new ArrayList<>();
    
    public ContainerHandler(ConfigurationSection config) {
        Set<String> set = config.getKeys(false);

        List<String> keys = new ArrayList<>(set);

        for(String key : keys) {
            containers.add(new Container(key, config.getConfigurationSection(key)));
        }
    }

    public ContainerHandler(ActiveVehicle v, ActiveModel m, ContainerHandler another) {
        vehicle = v;
        for(Container c : another.getContainers()) {
            containers.add(new Container(m, c));
        }
    }

    public List<Container> getContainers() {
        return containers;
    }

    public Container get(String uuid) {
        for(Container c : containers) {
            if(uuid.equalsIgnoreCase(c.getId())) return c;
        }
        return null;
    } 

    public void open(Player p, String id) {
        for(Container c : containers) {
            if(c.getId().equalsIgnoreCase(id)) {
                c.open(vehicle, p);
                return;
            }
        }
    }

    public void open(Player player) {
        if (containers.isEmpty()) return;

        if (containers.size() == 1) {
            containers.get(0).open(vehicle, player);
            return;
        }

        int guiSize = 27;
        Inventory gui = Bukkit.createInventory(
            new VFInventoryHolder(null, VFGUI.CONTAINER_SELECT),
            guiSize,
            "Select Container"
        );

        NamespacedKey key = new NamespacedKey(VehicleFramework.plugin, "vf_container_id");

        for (int i = 0; i < Math.min(guiSize, containers.size()); i++) {
            Container container = containers.get(i);

            ItemStack item = new ItemStack(Material.CHEST);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(container.getName()); // Or ChatColor if you want styling
            meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, container.getId());
            item.setItemMeta(meta);

            gui.setItem(i, item);
        }

        player.openInventory(gui);
    }
}
