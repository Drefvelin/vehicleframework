package net.tfminecraft.VehicleFramework.Database;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.joml.Quaternionf;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.tfminecraft.VehicleFramework.VFLogger;
import net.tfminecraft.VehicleFramework.Bones.BoneRotator;
import net.tfminecraft.VehicleFramework.Enums.Component;
import net.tfminecraft.VehicleFramework.Managers.SpawnManager;
import net.tfminecraft.VehicleFramework.Util.SpawnLocation;
import net.tfminecraft.VehicleFramework.Vehicles.ActiveVehicle;
import net.tfminecraft.VehicleFramework.Vehicles.Component.Engine;
import net.tfminecraft.VehicleFramework.Vehicles.Component.GearedEngine;
import net.tfminecraft.VehicleFramework.Vehicles.Component.SinkableHull;
import net.tfminecraft.VehicleFramework.Vehicles.Component.VehicleComponent;
import net.tfminecraft.VehicleFramework.Vehicles.Handlers.Container.Container;
import net.tfminecraft.VehicleFramework.Vehicles.Seat.Seat;
import net.tfminecraft.VehicleFramework.Weapons.ActiveWeapon;

public class Database {
	private JSONObject json; // org.json.simple
    JSONParser parser = new JSONParser();
    public IncompleteVehicle loadVehicle(SpawnLocation sLoc) {
    	SpawnManager.remove(sLoc);
    	File file = new File("plugins/VehicleFramework/data/vehicles", sLoc.getFile());
    	if(!file.exists()) {
    		VFLogger.log("Failed to locate vehicle file "+file.getName());
    		return null;
    	}
    	try {
			json = (JSONObject) parser.parse(new InputStreamReader(new FileInputStream(file), "UTF-8"));
			String uuid = new String(sLoc.getFile()).replace(".json", "");
			String id = (String) json.get("id");
			String name = (String) json.get("name");
			String skin = (String) json.get("skin");

			int throttle = 0;
			int gear = 1;
			double fuel = 0;

			float yaw = json.containsKey("yaw") ? ((Double) json.get("yaw")).floatValue() : 0f;
			
			JSONObject componentsObject = (JSONObject) json.get("components");
	        List<IncompleteComponent> componentsList = new ArrayList<>();

	        // Iterate over the components (key is the component type, value is the component data)
	        for (Object componentKey : componentsObject.keySet()) {
	            String componentType = (String) componentKey;
	            JSONObject componentData = (JSONObject) componentsObject.get(componentType);

	            // Extract properties
	            double damage = (Double) componentData.get("damage");
	            int fireProgress = componentData.containsKey("fire") ? ((Long) componentData.get("fire")).intValue() : 0;
	            int sinkProgress = componentData.containsKey("sinkprogress") ? ((Long) componentData.get("sinkprogress")).intValue() : 0;
				try {
					Component c = Component.valueOf(componentType.toUpperCase());
					switch (c) {
						case ENGINE:
							if(componentData.containsKey("throttle")) throttle = ((Long) componentData.get("throttle")).intValue();
							if(componentData.containsKey("fuel")) fuel = (Double) componentData.get("fuel");
							break;
						case GEARED_ENGINE:
							if(componentData.containsKey("gear")) gear = ((Long) componentData.get("gear")).intValue();
							if(componentData.containsKey("throttle")) throttle = ((Long) componentData.get("throttle")).intValue();
							if(componentData.containsKey("fuel")) fuel = (Double) componentData.get("fuel");
							break;
						default:
							break;
					}
					// Instantiate IncompleteComponent based on the type (example logic here)
					IncompleteComponent incompleteComponent = new IncompleteComponent(
						c,
						damage, 
						fireProgress, 
						sinkProgress
					);
					componentsList.add(incompleteComponent);
				} catch (Exception e) {
					VFLogger.log("Error loading component "+componentType+ " for vehicle "+id);
				}
	        }
			JSONObject rotatorsObject = (JSONObject) json.get("rotators");
			List<RotationData> rotations = new ArrayList<>();

			for (Object key : rotatorsObject.keySet()) {
				String rotatorId = (String) key;
				JSONObject rotationValues = (JSONObject) rotatorsObject.get(rotatorId);
				rotations.add(new RotationData(rotatorId, rotationValues));
			}

	        List<IncompleteWeapon> weapons = new ArrayList<>();
	        if(json.containsKey("weapons")) {
				JSONObject weaponsObject = (JSONObject) json.get("weapons");
				for (Object weaponKey : weaponsObject.keySet()) {
					String weaponId = (String) weaponKey;
					JSONObject weaponData = (JSONObject) weaponsObject.get(weaponId);

					// Extract properties
					double damage = (Double) weaponData.get("damage");
					String ammo = weaponData.containsKey("ammo") ? (String) weaponData.get("ammo") : null;
					int count = weaponData.containsKey("count") ? ((Long) weaponData.get("count")).intValue() : 0;
					IncompleteWeapon incompleteWeapon = new IncompleteWeapon(
						weaponId,
						damage, 
						ammo,
						count
					);
					weapons.add(incompleteWeapon);
				}
	        }	

			List<PassengerData> passengers = new ArrayList<>();
			if(json.containsKey("passengers")) {
				JSONObject passengersObject = (JSONObject) json.get("passengers");

				// Iterate over the components (key is the component type, value is the component data)
				for (Object passengerKey : passengersObject.keySet()) {
					String seatId = (String) passengerKey;
					JSONObject passengerData = (JSONObject) passengersObject.get(seatId);

					// Extract properties
					String player = (String) passengerData.get("player");
					passengers.add(new PassengerData(player, seatId));
				}
			}
			List<JsonObject> containers = loadContainers(json);
	        // Create and return IncompleteVehicle with loaded components
	        file.delete();
	        return new IncompleteVehicle(uuid, id, name, skin, componentsList, weapons, rotations, passengers, containers, throttle, gear, yaw, fuel);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
    	return null;
	}

	public List<JsonObject> loadContainers(JSONObject vehicleJson) {
		List<JsonObject> containerList = new ArrayList<>();

		if (!vehicleJson.containsKey("containers")) return containerList;

		JSONObject containersSection = (JSONObject) vehicleJson.get("containers");

		for (Object key : containersSection.keySet()) {
			Object rawContainer = containersSection.get(key);
			if (rawContainer == null) continue;

			// Convert JSON.simple object -> JSON string -> Gson JsonObject
			String containerString = rawContainer.toString();
			JsonObject containerJson = JsonParser.parseString(containerString).getAsJsonObject();
			containerList.add(containerJson);
		}

		return containerList;
	}

	@SuppressWarnings("unchecked")
	public JSONObject saveContainers(List<Container> containers, JSONObject vehicleJson) {
		JSONObject containersSection = new JSONObject();

		for (Container container : containers) {
			// Get the container JSON (Gson)
			JsonObject gsonJson = container.getAsJson();

			// Convert Gson JsonObject to JSON.simple JSONObject via string roundtrip
			Object parsed = org.json.simple.JSONValue.parse(gsonJson.toString());
			if (parsed instanceof JSONObject simpleJson) {
				containersSection.put(container.getId(), simpleJson);
			}
		}

		vehicleJson.put("containers", containersSection);
		return vehicleJson;
	}


    public void loadActiveSpawnLocations() {
    	for (World world : Bukkit.getWorlds()) {
            for (Chunk chunk : world.getLoadedChunks()) {
                loadSpawnLocations(chunk);
            }
        }
	}
    public void loadSpawnLocations(Chunk c) {
        File folder = new File(getPath(c));
        if (!folder.exists()) return;
        
        // Get all files in the folder
        File[] files = folder.listFiles();
        if (files == null || files.length == 0) return; // Ensure there are files to process

        // Process each file
        for (int i = 0; i < files.length; i++) {
            File file = files[i];
            
            if (!file.isDirectory()) {
                try {
                    // Parse the JSON from the file
                    json = (JSONObject) parser.parse(new InputStreamReader(new FileInputStream(file), "UTF-8"));
                    Location loc = locFromFile(file);
                    String fileString = (String) json.get("file");

                    // Create a SpawnLocation object and add it to the SpawnManager
                    SpawnLocation sLoc = new SpawnLocation(c, loc, fileString);
                    SpawnManager.add(sLoc);

                    // Delete the file after processing
                    file.delete();

                    // If this is the last file in the folder, delete the folder
                    if (i == files.length - 1) {
                        folder.delete();
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
    }
	public void saveSpawnLocation(SpawnLocation sLoc) {
		try {
			String path = getPath(sLoc.getChunk());
			File folder = new File(path);
			if(!folder.exists()) folder.mkdirs();
			File file = new File(path, UUID.randomUUID().toString()+".json");
			if(file.exists() == true) {
				file.delete();
			}
			file.createNewFile();
        	PrintWriter pw = new PrintWriter(file, "UTF-8");
        	pw.print("{");
        	pw.print("}");
        	pw.flush();
        	pw.close();
            HashMap<String, Object> defaults = new HashMap<String, Object>();
        	json = (JSONObject) parser.parse(new InputStreamReader(new FileInputStream(file), "UTF-8"));
        	Location loc = sLoc.getLoc();
        	defaults.put("world", loc.getWorld().getName());
        	defaults.put("xPos", loc.getX());
        	defaults.put("yPos", loc.getY());
        	defaults.put("zPos", loc.getZ());
        	defaults.put("file", sLoc.getFile());
        	save(file, defaults);
        } catch (Throwable ex) {
			ex.printStackTrace();
        }
    }
	private Location locFromFile(File file) {
		Location loc = new Location(Bukkit.getServer().getWorld((String) json.get("world")), (Double) json.get("xPos"),(Double) json.get("yPos"),(Double) json.get("zPos"));
		return loc;
	}
	private String getPath(Chunk c) {
		String path = "plugins/VehicleFramework/data/locations";
		path = path +"/"+chunkToString(c);
		return path;
	}
	private String chunkToString(Chunk c) {
		return c.getWorld().getName() + "/" + c.getX()+"."+c.getZ();
	}
	@SuppressWarnings("unchecked")
	public void saveVehicle(ActiveVehicle v) {
		try {
			String id = v.getUUID().toString();
			String fileString = id + ".json";

			saveSpawnLocation(new SpawnLocation(v.getEntity().getLocation().getChunk(), v.getEntity().getLocation(), fileString));

			File file = new File("plugins/VehicleFramework/data/vehicles", fileString);
			if (file.exists()) file.delete();
			file.createNewFile();

			JSONObject vehicleData = new JSONObject();
			vehicleData.put("id", v.getId());
			vehicleData.put("name", v.getName());
			vehicleData.put("yaw", v.getEntity().getLocation().getYaw());
			vehicleData.put("skin", v.getSkinHandler().getCurrentSkin().getId());
			if(v.hasContainers()) saveContainers(v.getContainerHandler().getContainers(), vehicleData);
			// --- PASSENGERS ---
			JSONObject passengersObject = new JSONObject();
			for (Entity e : v.getSeatHandler().getPassengers()) {
				if(!(e instanceof Player)) continue;
				Player p = (Player) e;
				Seat seat = v.getSeat(p);
				if(seat == null) continue;
				JSONObject passengerData = new JSONObject();
				passengerData.put("player", p.getName());
				passengersObject.put(seat.getBone(), passengerData);
			}
			vehicleData.put("passengers", passengersObject);

			// --- COMPONENTS ---
			JSONObject componentsObject = new JSONObject();
			for (VehicleComponent component : v.getComponents()) {
				JSONObject componentData = new JSONObject();
				componentData.put("damage", component.getHealthData().getDamage());

				if (component.isOnFire()) {
					componentData.put("fire", component.getFire().getProgress());
				}
				if (component instanceof SinkableHull) {
					SinkableHull hull = (SinkableHull) component;
					if (hull.isSinking()) {
						componentData.put("sinkprogress", hull.getSinkProgress());
					}
				}

				Component type = component.getType();
				switch (type) {
					case ENGINE:
						componentData.put("throttle", ((Engine) component).getThrottle().getCurrent());
						componentData.put("fuel", ((Engine) component).getFuelTank().getCurrent());
						break;
					case GEARED_ENGINE:
						componentData.put("gear", ((GearedEngine) component).getCurrentGear());
						componentData.put("throttle", ((GearedEngine) component).getGear().getThrottle().getCurrent());
						componentData.put("fuel", ((GearedEngine) component).getFuelTank().getCurrent());
						break;
					default:
						break;
				}

				componentsObject.put(type.toString().toLowerCase(), componentData);
			}
			vehicleData.put("components", componentsObject);

			// --- ROTATORS ---
			JSONObject rotatorsObject = new JSONObject();
			for (BoneRotator rotator : v.getAccessPanel().getRotators()) {
				JSONObject rotation = new JSONObject();
				Quaternionf q = rotator.getAnimator().getRotation();
				rotation.put("x", q.x());
				rotation.put("y", q.y());
				rotation.put("z", q.z());
				rotation.put("w", q.w());
				rotatorsObject.put(rotator.getId(), rotation);
			}
			vehicleData.put("rotators", rotatorsObject);

			// --- WEAPONS ---
			if (!v.getWeaponHandler().getWeapons().isEmpty()) {
				JSONObject weaponsObject = new JSONObject();

				for (ActiveWeapon w : v.getWeaponHandler().getWeapons()) {
					JSONObject weaponData = new JSONObject();
					weaponData.put("damage", w.getHealthData().getDamage());

					if (w.getAmmunitionHandler().hasAmmo()) {
						weaponData.put("ammo", w.getAmmunitionHandler().getAmmo().getId());
						weaponData.put("count", w.getAmmunitionHandler().getCount());
					}

					weaponsObject.put(w.getId(), weaponData);
				}

				vehicleData.put("weapons", weaponsObject);
			}
			save(file, vehicleData);

		} catch (Throwable ex) {
			ex.printStackTrace();
		}
	}

	@SuppressWarnings("unchecked")
    public boolean save(File file, HashMap<String, Object> defaults) {
      try {
    	  JSONObject toSave = new JSONObject();
      
        for (String s : defaults.keySet()) {
          Object o = defaults.get(s);
          if (o instanceof String) {
            toSave.put(s, getString(s, defaults));
          } else if (o instanceof Double) {
            toSave.put(s, getDouble(s, defaults));
          } else if (o instanceof Float) {
              toSave.put(s, getFloat(s, defaults));
          } else if (o instanceof Integer) {
            toSave.put(s, getInteger(s, defaults));
          } else if (o instanceof JSONObject) {
            toSave.put(s, getObject(s, defaults));
          } else if (o instanceof JSONArray) {
            toSave.put(s, getArray(s, defaults));
          }
        }
      
        TreeMap<String, Object> treeMap = new TreeMap<String, Object>(String.CASE_INSENSITIVE_ORDER);
        treeMap.putAll(toSave);
      
       Gson g = new GsonBuilder().setPrettyPrinting().create();
       String prettyJsonString = g.toJson(treeMap);
      
        FileWriter fw = new FileWriter(file);
        fw.write(prettyJsonString);
        fw.flush();
        fw.close();
      
        return true;
      } catch (Exception ex) {
        ex.printStackTrace();
        return false;
      }
    }
    
    public String getRawData(String key, HashMap<String, Object> defaults) {
        return json.containsKey(key) ? json.get(key).toString()
           : (defaults.containsKey(key) ? defaults.get(key).toString() : key);
      }
    
      public String getString(String key, HashMap<String, Object> defaults) {
        return ChatColor.translateAlternateColorCodes('&', getRawData(key, defaults));
      }

      public boolean getBoolean(String key, HashMap<String, Object> defaults) {
        return Boolean.valueOf(getRawData(key, defaults));
      }

      public double getDouble(String key, HashMap<String, Object> defaults) {
        try {
          return Double.parseDouble(getRawData(key, defaults));
        } catch (Exception ex) { }
        return -1;
      }
      
      public double getFloat(String key, HashMap<String, Object> defaults) {
          try {
            return Float.parseFloat(getRawData(key, defaults));
          } catch (Exception ex) { }
          return -1;
        }

      public double getInteger(String key, HashMap<String, Object> defaults) {
        try {
          return Integer.parseInt(getRawData(key, defaults));
        } catch (Exception ex) { }
        return -1;
      }
     
      public JSONObject getObject(String key, HashMap<String, Object> defaults) {
         return json.containsKey(key) ? (JSONObject) json.get(key)
           : (defaults.containsKey(key) ? (JSONObject) defaults.get(key) : new JSONObject());
      }
     
      public JSONArray getArray(String key, HashMap<String, Object> defaults) {
    	     return json.containsKey(key) ? (JSONArray) json.get(key)
    	       : (defaults.containsKey(key) ? (JSONArray) defaults.get(key) : new JSONArray());
      }
}
