package net.tfminecraft.VehicleFramework.Vehicles.Handlers.Container;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import com.ticxo.modelengine.api.model.ActiveModel;

import net.tfminecraft.VehicleFramework.Vehicles.ActiveVehicle;
import net.tfminecraft.VehicleFramework.Vehicles.Seat.Seat;

public class ContainerHandler {

    private ActiveVehicle vehicle;
    private HashMap<String, Container> containers = new HashMap<>();
    
    public ContainerHandler(ConfigurationSection config) {
        Set<String> set = config.getKeys(false);

        List<String> keys = new ArrayList<>(set);

        for(String key : keys) {
            Container c = new Container(key, config.getConfigurationSection(key));
            containers.put(c.getSeat(), c);
        }
    }

    public ContainerHandler(ActiveVehicle v, ActiveModel m, ContainerHandler another) {
        vehicle = v;
        for(Container c : another.getContainers().values()) {
            containers.put(c.getSeat(), new Container(v, c));
        }
    }

    public HashMap<String, Container> getContainers() {
        return containers;
    }

    public Container get(String id) {
        for(Container c : containers.values()) {
            if(id.equalsIgnoreCase(c.getId())) return c;
        }
        return null;
    } 
    public Container getBySeat(String seat) {
        for(Container c : containers.values()) {
            if(seat.equalsIgnoreCase(c.getSeat())) return c;
        }
        return null;
    }

    public boolean open(Player player) {
        if (containers.isEmpty()) return false;

        Seat s = vehicle.getSeat(player);
        if(s == null) return false;
        Container c = getBySeat(s.getBone());
        if(c == null) return false;
        c.open(vehicle, player);
        return true;
    }

    public void destroy(Location loc) {
        for(Container c : containers.values()) c.destroy(loc);
    }
}
