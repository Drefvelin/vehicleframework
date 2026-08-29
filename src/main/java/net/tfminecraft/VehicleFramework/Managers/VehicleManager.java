package net.tfminecraft.VehicleFramework.Managers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Donkey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Horse;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mule;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.core.mobs.ActiveMob;
import me.Plugins.TLibs.Objects.API.ItemAPI;
import me.Plugins.TLibs.Objects.API.SubAPI.StringFormatter;
import me.Plugins.TLibs.TLibs;
import net.tfminecraft.VehicleFramework.Cache.Cache;
import net.tfminecraft.VehicleFramework.Data.NamingData;
import net.tfminecraft.VehicleFramework.Data.OwnedVehicleSummary;
import net.tfminecraft.VehicleFramework.Data.StoredVehicleMeta;
import net.tfminecraft.VehicleFramework.Database.Database;
import net.tfminecraft.VehicleFramework.Database.IncompleteVehicle;
import net.tfminecraft.VehicleFramework.Database.PersistenceLog;
import net.tfminecraft.VehicleFramework.Enums.Component;
import net.tfminecraft.VehicleFramework.Enums.Keybind;
import net.tfminecraft.VehicleFramework.Enums.SeatType;
import net.tfminecraft.VehicleFramework.Enums.VFGUI;
import net.tfminecraft.VehicleFramework.Enums.VehicleRemoveReason;
import net.tfminecraft.VehicleFramework.Events.VFEntityDamageEvent;
import net.tfminecraft.VehicleFramework.Events.VehicleOwnerClaimedEvent;
import net.tfminecraft.VehicleFramework.Events.VehiclePreInteractEvent;
import net.tfminecraft.VehicleFramework.Events.VehicleSpawnEvent;
import net.tfminecraft.VehicleFramework.Loaders.FuelLoader;
import net.tfminecraft.VehicleFramework.Loaders.VehicleLoader;
import net.tfminecraft.VehicleFramework.Managers.Inventory.VFInventoryHolder;
import net.tfminecraft.VehicleFramework.Managers.Spawner.VehicleSpawner;
import net.tfminecraft.VehicleFramework.Protocol.PacketConverter;
import net.tfminecraft.VehicleFramework.Tracks.TrainTapeInteract;
import net.tfminecraft.VehicleFramework.Tracks.TrainCollision;
import net.tfminecraft.VehicleFramework.Tracks.TrackJunction;
import net.tfminecraft.VehicleFramework.Util.Damager;
import net.tfminecraft.VehicleFramework.VFLogger;
import net.tfminecraft.VehicleFramework.VehicleFramework;
import net.tfminecraft.VehicleFramework.Vehicles.ActiveVehicle;
import net.tfminecraft.VehicleFramework.Vehicles.Component.Harness;
import net.tfminecraft.VehicleFramework.Vehicles.Handlers.Container.Container;
import net.tfminecraft.VehicleFramework.Vehicles.Handlers.VehicleTicketInteract;
import net.tfminecraft.VehicleFramework.Vehicles.Handlers.VehicleTicketItems;
import net.tfminecraft.VehicleFramework.Vehicles.Handlers.VehicleTicketRules;
import net.tfminecraft.VehicleFramework.Vehicles.Handlers.TowHandler;
import net.tfminecraft.VehicleFramework.Vehicles.Seat.Seat;
import net.tfminecraft.VehicleFramework.Vehicles.Handlers.Train.ConsistRelinker;
import net.tfminecraft.VehicleFramework.Vehicles.Vehicle;

public class VehicleManager implements Listener{
	private ItemAPI api = TLibs.getItemAPI();
	private Database db = new Database();
	private InventoryManager inv = new InventoryManager();
	private RepairManager repairManager = new RepairManager(this);
	private SpawnManager spawnManager = new SpawnManager(this);
	
	private HashMap<Player, Long> cooldown = new HashMap<>();
	
	//Various utils and stuff
	private VehicleSpawner spawner = new VehicleSpawner();
	private PacketConverter converter = new PacketConverter();
	
	//Player management
	private HashMap<Player, ActiveVehicle> tempVehicle = new HashMap<>();
	private HashMap<Player, ActiveVehicle> activeVehicle = new HashMap<>();
	private HashMap<Player, NamingData> naming = new HashMap<>();

	// Ownership – whitelist-add state (player typed name is pending)
	private HashMap<Player, ActiveVehicle> addingToWhitelist = new HashMap<>();
	private HashMap<Player, Integer> addingToWhitelistTimeout = new HashMap<>();

	// Owner-eject cooldown: player cannot re-enter that vehicle until timestamp expires
	private HashMap<Player, HashMap<String, Long>> ejectCooldown = new HashMap<>();

	// Admin takeover: player is waiting to click a vehicle and claim ownership
	private HashMap<Player, Integer> pendingTakeover = new HashMap<>();

	private OwnershipGUIManager ownershipGUI = new OwnershipGUIManager();
	
	private HashMap<Player, ActiveVehicle> tow = new HashMap<>();

	private HashMap<Player, ActiveVehicle> pendingEntityVehicle = new HashMap<>();
	private HashMap<Player, String> pendingEntitySeat = new HashMap<>();
	
	private HashMap<Entity, ActiveVehicle> vehicles = new HashMap<>();

	private Set<Entity> damagedEntities = new HashSet<>();

	public void setDamaged(Entity e, boolean damaged) {
		if (damaged) {
			damagedEntities.add(e);
		} else {
			damagedEntities.remove(e);
		}
	}

	//Managers
	public RepairManager getRepairManager() {
		return repairManager;
	}
	public SpawnManager getSpawnManager() {
		return spawnManager;
	}

	public HashMap<Entity, ActiveVehicle> get() {
		return vehicles;
	}

	public ActiveVehicle getByUUID(String UUID) {
		for (Map.Entry<Entity, ActiveVehicle> entry : vehicles.entrySet()) {
        	ActiveVehicle v = entry.getValue();
            if(v.getUUID().equalsIgnoreCase(UUID)) return v;
        }
		return null;
	}
	
	public ActiveVehicle get(Entity e) {
		if(vehicles.containsKey(e)) return vehicles.get(e);
		return null;
	}
	public ActiveVehicle getByPassenger(Entity e) {
		for (Map.Entry<Entity, ActiveVehicle> entry : vehicles.entrySet()) {
        	ActiveVehicle v = entry.getValue();
            if(v.isPassenger(e, true)) return v;
        }
		return null;
	}
	public ActiveVehicle get(String id) {
		for (Map.Entry<Entity, ActiveVehicle> entry : vehicles.entrySet()) {
        	ActiveVehicle v = entry.getValue();
            if(v.getUUID().equalsIgnoreCase(id)) return v;
        }
		return null;
	}
	public void unregister(Entity e) {
		if(vehicles.containsKey(e)) vehicles.remove(e);
	}
	
	private void register(ActiveVehicle v) {
		vehicles.put(v.getEntity(), v);
	}

	public int kill(Player p, Location loc, int radius) {
		World world = loc.getWorld();
		if (world == null) return 0;
		int count = 0;
		for (Entity entity : world.getNearbyEntities(loc, radius, radius, radius)) {
			if(get(entity) == null) continue;
			ActiveVehicle v = get(entity);
			v.remove(VehicleRemoveReason.ADMIN_KILL);
			count++;
			if(p != null) VFLogger.message(p, "§cKilled "+v.getName());
		}
		return count;
	}
	
	public ActiveVehicle spawn(Location loc, String s) {
		Vehicle v = VehicleLoader.getByString(s);
		if(v == null) {
			VFLogger.log("Attempted to spawn vehicle by the id "+s+" but no vehicle was found!");
			return null;
		}
		return spawn(loc, v);
	}
	
	public ActiveVehicle spawn(Location loc, Vehicle v) {
		return spawn(loc, v, null);
	}
	
	public ActiveVehicle spawn(Location loc, Vehicle v, IncompleteVehicle i) {
		ActiveVehicle vehicle = spawner.spawn(loc, v, this, i);
		register(vehicle);
		ConsistRelinker.tryLink(vehicle);
		PersistenceLog.spawned(vehicle, loc);
		Bukkit.getPluginManager().callEvent(new VehicleSpawnEvent(vehicle));
		return vehicle;
	}
	
	public void start() {
		if(db.isDirtyFlag()) db.restoreBackupSnapshot();
		spawnManager.start();
		vehicleFastTickCycle();
		vehicleSlowTickCycle();
		snapshotCycle();
	}

	public void reload() {
		PersistenceLog.append("VEHICLE_MANAGER_RELOAD");
		spawnManager.start();
	}
	private void vehicleSlowTickCycle() {
		new BukkitRunnable() {
	        @SuppressWarnings("unchecked")
			@Override
	        public void run() {
	            for (Map.Entry<Entity, ActiveVehicle> entry : vehicles.entrySet()) {
	            	ActiveVehicle v = entry.getValue();
	                try {
						v.slowTick();
					} catch (Exception e) {
						VFLogger.log(v.getId()+" has run into an issue");
					}
	            }
				for (Map.Entry<Player, NamingData> entry : ((HashMap<Player, NamingData>) naming.clone()).entrySet()) {
	            	if(entry.getValue().tick()) {
						naming.remove(entry.getKey());
						entry.getKey().sendMessage("§cNaming timed out.");
					}
	            }
	        }
	    }.runTaskTimer(VehicleFramework.plugin, 0L, 20L);
	}
	private void vehicleFastTickCycle() {
		new BukkitRunnable() {
			@Override
	        public void run() {
				updateInventory();
	            for (Map.Entry<Entity, ActiveVehicle> entry : vehicles.entrySet()) {
	            	ActiveVehicle v = entry.getValue();
	                try {
						v.tick();
					} catch (Exception e) {
						VFLogger.log(v.getId()+" has run into an issue");
					}
	            }
				TrainCollision.tick(vehicles.values());
	            Iterator<Map.Entry<Player, ActiveVehicle>> iterator = tow.entrySet().iterator();
	            while (iterator.hasNext()) {
	                Map.Entry<Player, ActiveVehicle> entry = iterator.next();
	                ActiveVehicle v = entry.getValue();
	                Player p = entry.getKey();

	                if (p.getLocation().distanceSquared(v.getEntity().getLocation()) > 64) {
	                    p.sendMessage("§7Deselected " + v.getName() + " §7for towing (Too far away)");
	                    p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
	                    iterator.remove(); // Safely remove the entry
	                }
	            }
	        }
	    }.runTaskTimer(VehicleFramework.plugin, 0L, 1L);
	}
	private void snapshotCycle() {
		new BukkitRunnable() {
			@Override
	        public void run() {
				VFLogger.info("Performing backup...");
				db.backupFiles();
				for(ActiveVehicle v : vehicles.values()) {
					db.saveBackup(v);
				}
	        }
	    }.runTaskTimer(VehicleFramework.plugin, 0L, 6000L);
	}
	
	public void updateInventory() {
		for(Player p : Bukkit.getOnlinePlayers()) {
			if(p.getOpenInventory().getTopInventory() == null) continue;
			Inventory i = p.getOpenInventory().getTopInventory();
			if(!(i.getHolder() instanceof VFInventoryHolder)) continue;
			VFInventoryHolder h = (VFInventoryHolder) i.getHolder();
			ActiveVehicle v = get(h.getId());
			if(v == null) continue;
			if(h.getType().equals(VFGUI.SEAT_SELECTION)) {
				inv.seatSelection(i, p, v, false);
			} else if(h.getType().equals(VFGUI.SKIN_SELECTION)) {
				inv.skinSelection(i, p, v, false);
			} else if(h.getType().equals(VFGUI.REPAIR)) {
				inv.repairWindow(i, p, v, false, repairManager.getTool(p));
			}
		}
	}
	
	public boolean isPassenger(Entity e) {
		for (Map.Entry<Entity, ActiveVehicle> entry : vehicles.entrySet()) {
        	ActiveVehicle v = entry.getValue();
            if(v.isPassenger(e, true)) return true;
        }
		return false;
	}
	
	public void dismount(Player p) {
		if(activeVehicle.containsKey(p)) activeVehicle.remove(p);
	}
	
	public void leashedInteract(Player p, ActiveVehicle v, LivingEntity e) {
		e.setLeashHolder(null);
		if(e instanceof Horse || e instanceof Donkey || e instanceof Mule) {
			if(v.getComponent(Component.HARNESS) != null) {
				Harness h = (Harness) v.getComponent(Component.HARNESS);
				h.mount(p, e);
			}
		} else {
			
		}
	}
	
	public boolean leadInteract(Player p, ActiveVehicle v) {
		if(v.getComponent(Component.HARNESS) != null) {
			Harness h = (Harness) v.getComponent(Component.HARNESS);
			return h.dismount(p);
		}
		return false;
	}
	
	public void seatInteract(Player p, ActiveVehicle v) {
		if(activeVehicle.containsKey(p)) return;
	    if(v.isPassenger(p, true)) {
	    	return;
	    }
	    // If whitelist mode is enabled, only owner or whitelisted players can open seat selection.
	    if(v.getOwnerData().isWhiteListed() && !v.getOwnerData().getOwner().equalsIgnoreCase("none")) {
	    	String ownerEntry = v.getOwnerData().getOwner();
	    	String playerEntry = "player_" + p.getName();
	    	boolean isOwner = ownerEntry != null && ownerEntry.equalsIgnoreCase(playerEntry);
	    	boolean isWhitelisted = false;
	    	for(String entry : v.getOwnerData().getWhiteList()) {
	    		if(entry == null) continue;
	    		if(entry.equalsIgnoreCase(playerEntry) || entry.equalsIgnoreCase(p.getName())) {
	    			isWhitelisted = true;
	    			break;
	    		}
	    	}
	    	if(!(isOwner || isWhitelisted)) {
	    		p.sendMessage("§cYou are not on this vehicle's whitelist.");
	    		return;
	    	}
	    }
	    // Check if this player was ejected by the owner and is still on cooldown for this vehicle
	    long ejectUntil = ejectUntil(p, v);
	    if (ejectUntil > System.currentTimeMillis()) {
	    	long remaining = (ejectUntil - System.currentTimeMillis() + 999) / 1000;
	    	p.sendMessage("§cYou cannot enter this vehicle for §e" + remaining + "§c more seconds.");
	    	return;
	    }
	    // Clear any pending entity mount when opening the seat menu again
	    pendingEntityVehicle.remove(p);
	    pendingEntitySeat.remove(p);
	    inv.seatSelection(null, p, v, true);
	    tempVehicle.put(p, v);
	}
	
	public void skinInteract(Player p, ActiveVehicle v) {
	    inv.skinSelection(null, p, v, true);
	    tempVehicle.put(p, v);
	}

	private void handlePendingEntityMount(Player p, Entity entity) {
		ActiveVehicle v = pendingEntityVehicle.remove(p);
		String seatBone = pendingEntitySeat.remove(p);
		if(v == null || seatBone == null) return;
		if(v.isDestroyed()) {
			p.sendMessage("§cThe vehicle has been destroyed");
			return;
		}
		if(!isEntityAllowed(entity, v.getEntitySeatWhitelist())) {
			p.sendMessage("§cThat entity is not allowed in this seat");
			return;
		}
		Seat seat = v.getSeat(seatBone);
		if(seat == null || seat.isOccupied()) {
			p.sendMessage("§cSeat is no longer available");
			return;
		}
		v.addPassenger(entity, seat);
		p.sendMessage("§aEntity mounted");
	}

	private boolean isEntityAllowed(Entity entity, List<String> whitelist) {
		if(whitelist == null || whitelist.isEmpty()) return false;
		for(String entry : whitelist) {
			if(entry.startsWith("mm.")) {
				String mmId = entry.substring(3);
				try {
					Optional<ActiveMob> mob = MythicBukkit.inst().getMobManager().getActiveMob(entity.getUniqueId());
					if(mob.isPresent() && mob.get().getType().getInternalName().equals(mmId)) return true;
				} catch(Exception ex) {}
			} else if(entry.startsWith("v.")) {
				String vanillaType = entry.substring(2).toUpperCase();
				if(entity.getType().name().equalsIgnoreCase(vanillaType)) return true;
			}
		}
		return false;
	}

	public void startTakeover(Player p) {
		cancelTakeover(p);
		int taskId = new BukkitRunnable() {
			@Override
			public void run() {
				if(pendingTakeover.containsKey(p)) {
					pendingTakeover.remove(p);
					p.sendMessage("\u00a7cTakeover expired.");
				}
			}
		}.runTaskLater(VehicleFramework.plugin, 300L).getTaskId();
		pendingTakeover.put(p, taskId);
		p.sendMessage("\u00a7aRight-click a vehicle within 15 seconds to claim ownership.");
	}

	private void cancelTakeover(Player p) {
		if(pendingTakeover.containsKey(p)) {
			VehicleFramework.plugin.getServer().getScheduler().cancelTask(pendingTakeover.get(p));
			pendingTakeover.remove(p);
		}
	}

	/**
	 * @return true if ownership was committed to the player
	 */
	private boolean claimOwnership(Player player, ActiveVehicle vehicle) {
		if (player == null || vehicle == null) {
			return false;
		}
		String previous = vehicle.getOwnerData().getOwner();
		if (previous != null && !previous.equalsIgnoreCase("none")) {
			return false;
		}
		String newOwner = "player_" + player.getName();
		VehicleOwnerClaimedEvent event =
				new VehicleOwnerClaimedEvent(player, vehicle, previous, newOwner);
		Bukkit.getPluginManager().callEvent(event);
		if (event.isCancelled()) {
			return false;
		}
		vehicle.getOwnerData().setOwner(newOwner);
		return true;
	}

	public void towSelect(Player p, ActiveVehicle v) {
		if(tow.containsKey(p)) {
			p.sendMessage("§7Deselected "+tow.get(p).getName()+" §7for towing");
			tow.remove(p);
		}
		p.sendMessage("§aSelected "+v.getName()+" §afor towing, right click a vehicle to attach it");
		p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, 1f, 1f);
		tow.put(p, v);
	}
	
	public void towAttach(Player p, ActiveVehicle v) {
		TowHandler h = v.getTowHandler();
		if(h.isOccupied()) {
			p.sendMessage("§cThis vehicle is already towing something");
			p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
			return;
		}
		ActiveVehicle selected = tow.get(p);
		if(h.getTowLocation().distanceSquared(selected.getEntity().getLocation()) > 16) {
			p.sendMessage("§cThis vehicle is too far away");
			p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
			return;
		}
		h.attach(tow.get(p));
		tow.remove(p);
		p.sendMessage("§aAttached "+selected.getName()+" §ato "+v.getName());
		p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, 1f, 1f);
	}
	
	
	//Events
	@EventHandler
	public void swap(PlayerSwapHandItemsEvent e) {
		Player p = e.getPlayer();
		if(get(p) == null) return;
		p.sendMessage("eeeee");
		ActiveVehicle v = get(p);
		v.key(p, Keybind.SWAP);
	}
	@EventHandler 
	public void vehicleInteract(PlayerInteractEntityEvent e){
		Entity entity = e.getRightClicked();
		Player p = e.getPlayer();
		for (Map.Entry<Entity, ActiveVehicle> entry : vehicles.entrySet()) {
        	ActiveVehicle v = entry.getValue();
            if(v.isPassenger(entity, true)) {
            	e.setCancelled(true);
            	return;
            }
        }
		// Handle pending entity mount: player selected an entity seat and is now right-clicking an entity
		if(pendingEntityVehicle.containsKey(p) && !vehicles.containsKey(entity)) {
			e.setCancelled(true);
			handlePendingEntityMount(p, entity);
			return;
		}
		if(!vehicles.containsKey(entity)) return;
		ActiveVehicle v = vehicles.get(entity);
		// Admin takeover
		if(pendingTakeover.containsKey(p)) {
			cancelTakeover(p);
			String previousOwner = v.getOwnerData().getOwner();
			boolean claimed;
			if (previousOwner == null || previousOwner.equalsIgnoreCase("none")) {
				claimed = claimOwnership(p, v);
			} else {
				v.getOwnerData().setOwner("player_" + p.getName());
				claimed = true;
			}
			if (claimed) {
				e.setCancelled(true);
				p.sendMessage("\u00a7aYou are now the owner of \u00a7e" + v.getName() + "\u00a7a.");
			}
			return;
		}
		VehiclePreInteractEvent preInteract = new VehiclePreInteractEvent(p, v);
		Bukkit.getPluginManager().callEvent(preInteract);
		if (preInteract.isCancelled()) {
			e.setCancelled(true);
			return;
		}
		if(cooldown.containsKey(p)) {
			if(cooldown.get(p) > System.currentTimeMillis()) {
				return;
			}
		}
		cooldown.put(p, System.currentTimeMillis()+100);
		if (TrainTapeInteract.handle(p, v)) {
			e.setCancelled(true);
			return;
		}
		if (VehicleTicketInteract.handle(p, v)) {
			e.setCancelled(true);
			return;
		}
		//Containers
		if(v.hasContainers()) {
			if(v.getContainerHandler().open(p)) return;
		}
		//Set Ownership
		claimOwnership(p, v);
		//Destroy
		if(api.getChecker().checkItemWithPath(p.getInventory().getItemInMainHand(), Cache.destroyItem)) {
			v.remove(VehicleRemoveReason.PLAYER_DESTROY);
			p.sendMessage("§cRemoved");
			return;
		}
		//Repair
		if(api.getChecker().checkItemWithPath(p.getInventory().getItemInMainHand(), Cache.repairItem)) {
			repairManager.repair(p, v);
			return;
		}
		//Fuel check
		String path = api.getChecker().getAsStringPath(p.getInventory().getItemInMainHand());
		if(v.usesFuel() && FuelLoader.itemIsFuel(path)) {
			v.refuel(p, path);
			return;
		}
		//all this is shit
		if(p.isSneaking()) {
			if(v.isTowable()) {
				towSelect(p, v);
				return;
			} else if(tow.containsKey(p) && !tow.get(p).equals(v)) {
				if(v.hasTowHandler()) {
					towAttach(p, v);
					return;
				} else if(v.isTrain()){
					boolean success = tow.get(p).getBehaviourHandler().getTrainHandler().attach(p, v);
					if(success) tow.remove(p);
					return;
				} else {
					p.sendMessage("§cThis vehicle cannot tow anything");
					p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
					return;
				}
			} else if(!tow.containsKey(p) && v.hasTowHandler() && v.getTowHandler().isOccupied()) {
				v.getTowHandler().unattach();
				p.sendMessage("§eStopped towing");
				p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, 1f, 1f);
				return;
			} else if(v.isTrain() && v.getBehaviourHandler().getTrainHandler().isAttachable()){
				towSelect(p, v);
			} else {
				p.sendMessage("§cThis vehicle cannot be towed");
				p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
				return;
			}
		}
		for(Entity nearbyEntity : p.getNearbyEntities(10, 10, 10)) {
	        if (nearbyEntity instanceof LivingEntity) {
	            LivingEntity livingEntity = (LivingEntity) nearbyEntity;
	            if(livingEntity.getPassengers().size() != 0) continue;
	            if (livingEntity.isLeashed() && livingEntity.getLeashHolder() instanceof Player) {
	                Player leashHolder = (Player) livingEntity.getLeashHolder();
	                if (leashHolder.equals(p)) {
	                	leashedInteract(p, v, livingEntity);
	                    return;
	                }
	            }
	        }
	    }
		if(p.getInventory().getItemInMainHand().getType().equals(Material.LEAD)) {
			if(leadInteract(p, v)) return;
		}
		if(activeVehicle.containsKey(p)) return;
		if(v.getSeatHandler().isPassenger(p)) {
			v.key(p, Keybind.RIGHT_CLICK);
			return;
		}
		if(api.getChecker().checkItemWithPath(p.getInventory().getItemInMainHand(), Cache.skinItem)) {
			skinInteract(p, v);
			return;
		}
		if(p.getInventory().getItemInMainHand().getType().equals(Material.NAME_TAG)) {
			naming.put(p, new NamingData(v));
			p.sendTitle("", "§eType the §aName §ein chat", 10, 80, 10);
			p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1f, 1f);
			return;
		}
		seatInteract(p, v);
	}
	@EventHandler
	public void ownershipClick(InventoryClickEvent e) {
		Player p = (Player) e.getWhoClicked();
		if(!(e.getView().getTopInventory().getHolder() instanceof VFInventoryHolder)) return;
		VFInventoryHolder h = (VFInventoryHolder) e.getView().getTopInventory().getHolder();
		if(!h.getType().equals(VFGUI.OWNERSHIP)) return;
		e.setCancelled(true);
		if(!h.getVehicle().isPresent()) return;
		ActiveVehicle v = h.getVehicle().get();
		if(v.isDestroyed()) { p.closeInventory(); return; }
		ItemStack item = e.getCurrentItem();
		if(item == null || item.getType().equals(Material.GRAY_STAINED_GLASS_PANE)) return;
		switch(e.getSlot()) {
			case 0: // Toggle whitelisting
				v.getOwnerData().setWhiteListed(!v.getOwnerData().isWhiteListed());
				ownershipGUI.ownershipGui(e.getView().getTopInventory(), p, v, false);
				break;
			case 2: // Add to whitelist
				p.closeInventory();
				addingToWhitelist.put(p, v);
				addingToWhitelistTimeout.put(p, 0);
				p.sendMessage("§aType the player name in chat to add them to the whitelist. Type §ccancel §ato abort.");
				break;
			case 4: // View whitelist
				p.closeInventory();
				ownershipGUI.whitelistGui(null, p, v, true);
				break;
			case 6: // Remove ownership
				v.getOwnerData().setOwner("none");
				v.getOwnerData().setWhiteListed(false);
				p.closeInventory();
				p.sendMessage("§7Ownership of §f" + v.getName() + "§7 has been removed.");
				break;
			case 8: // Toggle tickets on the consist loco when attached
				v.ticketSource().getOwnerData().toggleTickets();
				ownershipGUI.ownershipGui(e.getView().getTopInventory(), p, v, false);
				break;
			default:
				break;
		}
	}

	@EventHandler
	public void whitelistClick(InventoryClickEvent e) {
		Player p = (Player) e.getWhoClicked();
		if(!(e.getView().getTopInventory().getHolder() instanceof VFInventoryHolder)) return;
		VFInventoryHolder h = (VFInventoryHolder) e.getView().getTopInventory().getHolder();
		if(!h.getType().equals(VFGUI.WHITELIST)) return;
		e.setCancelled(true);
		if(!h.getVehicle().isPresent()) return;
		ActiveVehicle v = h.getVehicle().get();
		if(v.isDestroyed()) { p.closeInventory(); return; }
		ItemStack item = e.getCurrentItem();
		if(item == null || item.getType().equals(Material.GRAY_STAINED_GLASS_PANE)) return;
		if(e.getSlot() == 26) {
			// Back button
			p.closeInventory();
			ownershipGUI.ownershipGui(null, p, v, true);
			return;
		}
		// Player head – remove from whitelist
		if(item.getType().equals(Material.PLAYER_HEAD)) {
			NamespacedKey key = new NamespacedKey(VehicleFramework.plugin, "vf_whitelist_entry");
			String entry = item.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.STRING);
			if(entry != null) {
				v.getOwnerData().removeFromWhiteList(entry);
				p.sendMessage("§eRemoved §f" + (entry.startsWith("player_") ? entry.substring(7) : entry) + "§e from the whitelist.");
				ownershipGUI.whitelistGui(e.getView().getTopInventory(), p, v, false);
			}
		}
	}

	@EventHandler
	public void nameVehicle(AsyncPlayerChatEvent e) {
		Player p = e.getPlayer();
		if(!naming.containsKey(p) && !addingToWhitelist.containsKey(p)) return;
		e.setCancelled(true);
		// Whitelist add
		if(addingToWhitelist.containsKey(p)) {
			new BukkitRunnable() {
				@Override
				public void run() {
					ActiveVehicle v = addingToWhitelist.get(p);
					addingToWhitelist.remove(p);
					addingToWhitelistTimeout.remove(p);
					if(e.getMessage().equalsIgnoreCase("cancel")) {
						p.sendMessage("§cCancelled.");
						return;
					}
					String playerName = e.getMessage().trim();
					String entry = "player_" + playerName;
					if(v.getOwnerData().getWhiteList().contains(entry)) {
						p.sendMessage("§c" + playerName + " is already on the whitelist.");
						return;
					}
					v.getOwnerData().addToWhiteList(entry);
					p.sendMessage("§aAdded §f" + playerName + "§a to the whitelist.");
				}
			}.runTask(VehicleFramework.plugin);
			return;
		}
		if(!naming.containsKey(p)) return;
		new BukkitRunnable() {
			@Override
			public void run() {
				ActiveVehicle v = naming.get(p).getVehicle();
				if(get(v.getEntity()) == null) return;
				v.setName(StringFormatter.formatHex(e.getMessage().replace("_", " ")));
				naming.remove(p);
				p.sendMessage("§aRenamed the vehicle to "+v.getName());
				p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1f, 1f);
			}
		}.runTask(VehicleFramework.plugin);
	}
	@EventHandler
	public void playerLeave(PlayerQuitEvent e) {
		Player p = e.getPlayer();
		pendingEntityVehicle.remove(p);
		pendingEntitySeat.remove(p);
		addingToWhitelist.remove(p);
		addingToWhitelistTimeout.remove(p);
		ejectCooldown.remove(p);
		cancelTakeover(p);
		for (Map.Entry<Entity, ActiveVehicle> entry : vehicles.entrySet()) {
        	ActiveVehicle v = entry.getValue();
            if(v.isPassenger(p, false)) {
            	v.dismountPassenger(p, false);
            	return;
            }
        }
	}
	@EventHandler
	public void passengerDeath(PlayerDeathEvent e) {
		Player p = e.getEntity();
		for (Map.Entry<Entity, ActiveVehicle> entry : vehicles.entrySet()) {
        	ActiveVehicle v = entry.getValue();
            if(v.isPassenger(p, false)) {
            	v.dismountPassenger(p, false);
            	return;
            }
        }
	}
	@EventHandler(priority = EventPriority.HIGHEST)
	public void damagePassenger(EntityDamageEvent e) {
		Entity entity = e.getEntity();
		if(damagedEntities.contains(entity)) return;
		for (Map.Entry<Entity, ActiveVehicle> entry : vehicles.entrySet()) {
        	ActiveVehicle v = entry.getValue();
            if(v.isPassenger(entity, SeatType.HARNESS)) {
            	if(e.getCause().equals(DamageCause.SUFFOCATION)) e.setCancelled(true);
            	return;
            }
        }
		if(!(vehicles.containsKey(entity) || getByPassenger(entity) != null)) return;
		VFEntityDamageEvent event = new VFEntityDamageEvent(e.getEntity(), null, e.getCause().toString(), e.getDamage());
        Bukkit.getPluginManager().callEvent(event);
        if (!event.isCancelled()) {
        	if(e.getEntity() instanceof LivingEntity) {
				if(e.isCancelled()) return; 
				e.setCancelled(true);
        		LivingEntity l = (LivingEntity) e.getEntity();
        		try {
					Damager.damage(l, event.getDamage());
				} catch (Exception ex) {
					ex.printStackTrace();
				}
        	}
        }
	}
	@EventHandler(ignoreCancelled = true)
	public void damageVehicle(VFEntityDamageEvent e) {
		Entity entity = e.getEntity();
		if(vehicles.containsKey(entity)) {
			e.setCancelled(true);
			double damage = e.getDamage();
			ActiveVehicle v = vehicles.get(entity);
			v.damage(e.getCause(), damage);
		}
		if(entity instanceof Player) {
			Player p = (Player) entity;
			if(p.getGameMode().equals(GameMode.SPECTATOR) || p.getGameMode().equals(GameMode.CREATIVE)) {
				e.setCancelled(true);
				return;
			}
			for(Map.Entry<Entity, ActiveVehicle> entry : vehicles.entrySet()) {
				if(entry.getValue().isPassenger(p, false)) {
					double finalDamage = Math.min(e.getDamage()/2, 18);
					e.setDamage(finalDamage);
				}
			}
		}
	}
	
	@EventHandler
	public void closeInv(InventoryCloseEvent e){
		Player p = (Player) e.getPlayer();
		if(tempVehicle.containsKey(p)) tempVehicle.remove(p);
	}
	@EventHandler
	public void clickWhileMounted(PlayerInteractEvent e) {
		Player p = e.getPlayer();
		if(!activeVehicle.containsKey(p)) return;
		ActiveVehicle v = vehicles.get(activeVehicle.get(p).getEntity());
		if(!v.isPassenger(p, false)) return;
		Action a = e.getAction();
		if(p.isSneaking()) {
			if(a.equals(Action.RIGHT_CLICK_AIR) || a.equals(Action.RIGHT_CLICK_BLOCK)) v.key(p, Keybind.SHIFT_RIGHT_CLICK);
			if(a.equals(Action.LEFT_CLICK_AIR) || a.equals(Action.LEFT_CLICK_BLOCK)) v.key(p, Keybind.SHIFT_LEFT_CLICK);
			return;
		}
		if(a.equals(Action.RIGHT_CLICK_AIR) || a.equals(Action.RIGHT_CLICK_BLOCK)) v.key(p, Keybind.RIGHT_CLICK);
		if(a.equals(Action.LEFT_CLICK_AIR) || a.equals(Action.LEFT_CLICK_BLOCK)) v.key(p, Keybind.LEFT_CLICK);
		
	}

	private boolean allowTicketSeat(Player p, ActiveVehicle v, Seat seat) {
		if (p == null || v == null || seat == null) {
			return true;
		}
		ActiveVehicle source = v.ticketSource();
		boolean exempt = VehicleTicketRules.ownerOrWhitelisted(source.getOwnerData(), p.getName());
		boolean has = VehicleTicketItems.inventoryHas(p, source.getOwnerData().getTicketId());
		return VehicleTicketRules.mayEnter(source.getOwnerData().isTicketsEnabled(), seat.getType(), exempt, has);
	}

	private void putEjectCooldown(Player player, ActiveVehicle vehicle) {
		if (player == null || vehicle == null || vehicle.getUUID() == null) {
			return;
		}
		ejectCooldown.computeIfAbsent(player, ignored -> new HashMap<>())
				.put(vehicle.getUUID(), System.currentTimeMillis() + 60000L);
	}

	private long ejectUntil(Player player, ActiveVehicle vehicle) {
		if (player == null || vehicle == null || vehicle.getUUID() == null) {
			return 0;
		}
		HashMap<String, Long> byVehicle = ejectCooldown.get(player);
		if (byVehicle == null) {
			return 0;
		}
		Long until = byVehicle.get(vehicle.getUUID());
		if (until == null) {
			return 0;
		}
		if (until <= System.currentTimeMillis()) {
			byVehicle.remove(vehicle.getUUID());
			if (byVehicle.isEmpty()) {
				ejectCooldown.remove(player);
			}
			return 0;
		}
		return until;
	}

	public void mount(Player p, String seat, ActiveVehicle v) {
		Seat s = v.getSeatHandler().getSeat(seat);
		if(s == null) return;
		if (ejectUntil(p, v) > System.currentTimeMillis()) {
			long remaining = (ejectUntil(p, v) - System.currentTimeMillis() + 999) / 1000;
			p.sendMessage("§cYou cannot enter this vehicle for §e" + remaining + "§c more seconds.");
			return;
		}
		if (!allowTicketSeat(p, v, s)) {
			p.sendMessage("§cYou need a ticket for this vehicle.");
			return;
		}
		if(!v.isPassenger(p, true)) {
	    	v.addPassenger(p, s);
	    } else {
	    	v.changeSeat(p, s);
	    }
	    if(tempVehicle.containsKey(p)) tempVehicle.remove(p);
	    if(!activeVehicle.containsKey(p)) activeVehicle.put(p, v);
	}

	@EventHandler
	public void seatSelect(InventoryClickEvent e) {
		Player p = (Player) e.getWhoClicked();
		if(!(e.getView().getTopInventory().getHolder() instanceof VFInventoryHolder)) return;
		VFInventoryHolder h = (VFInventoryHolder) e.getView().getTopInventory().getHolder();
		if(!h.getType().equals(VFGUI.SEAT_SELECTION)) return;
		e.setCancelled(true);
		ActiveVehicle v = null;
		if(tempVehicle.containsKey(p)) v = tempVehicle.get(p);
		if(activeVehicle.containsKey(p)) v = activeVehicle.get(p);
		if(v == null) return;
		ItemStack i = e.getCurrentItem();
		if(i == null) return;
		if(i.getType().equals(Material.GRAY_STAINED_GLASS_PANE)) return;
		if(v.isDestroyed()) {
	    	p.sendMessage("Vehicle is destroyed");
	    	return;
	    }
		if(e.getSlot() == 25) {
			// Ownership settings – only owner can open this
			if(v.getOwnerData().getOwner().equalsIgnoreCase("player_" + p.getName())) {
				p.closeInventory();
				ownershipGUI.ownershipGui(null, p, v, true);
			}
			return;
		}
		if(e.getSlot() == 26) {
			v.dismountPassenger(p, false);
			return;
		}
		NamespacedKey key = new NamespacedKey(VehicleFramework.plugin, "vf_seat_id");
		String id = i.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.STRING);
		if(id == null) return;
		Seat seat = v.getSeat(id);
		
		// Occupied entity seat: click to dismount the entity and teleport it to the player
		if(i.getType().equals(Material.GRAY_CONCRETE) && seat != null && seat.getType().equals(SeatType.ENTITY)) {
			if(seat.isOccupied()) {
				Entity mounted = seat.getEntity();
				v.dismountPassenger(mounted, false);
				mounted.teleport(p.getLocation());
				p.sendMessage("§eEntity dismounted");
			}
			p.closeInventory();
			return;
		}
		// Empty entity seat: enter pending entity mount mode
		if(i.getType().equals(Material.CYAN_CONCRETE) && seat != null && seat.getType().equals(SeatType.ENTITY)) {
			pendingEntityVehicle.put(p, v);
			pendingEntitySeat.put(p, id);
			p.closeInventory();
			p.sendMessage("§eRight-click an entity to mount it in this seat");
			return;
		}
		if(i.getType().equals(Material.YELLOW_CONCRETE)) {
			// Owner can eject the player occupying this seat
			if(seat != null && !seat.getType().equals(SeatType.ENTITY)
					&& v.getOwnerData().getOwner().equalsIgnoreCase("player_" + p.getName())) {
				Entity occupant = seat.getEntity();
				if(occupant instanceof Player) {
					Player ejected = (Player) occupant;
					if (ejected.getUniqueId().equals(p.getUniqueId())) {
						p.sendMessage("§cYou cannot eject yourself");
						return;
					}
					v.dismountPassenger(ejected, false);
					putEjectCooldown(ejected, v);
					ejected.sendMessage("§cYou have been removed from this vehicle by the owner and cannot re-enter it for 60 seconds.");
					p.sendMessage("§aEjected §e" + ejected.getName() + "§a from the vehicle.");
					inv.seatSelection(p.getOpenInventory().getTopInventory(), p, v, false);
					return;
				}
			}
			p.sendMessage("§cSeat is occupied");
			return;
		}
		if (!allowTicketSeat(p, v, seat)) {
			p.sendMessage("§cYou need a ticket for this vehicle.");
			return;
		}
		if (ejectUntil(p, v) > System.currentTimeMillis()) {
			long remaining = (ejectUntil(p, v) - System.currentTimeMillis() + 999) / 1000;
			p.sendMessage("§cYou cannot enter this vehicle for §e" + remaining + "§c more seconds.");
			return;
		}
	    if(!v.isPassenger(p, true)) {
	    	v.addPassenger(p, seat);
	    } else {
	    	v.changeSeat(p, seat);
	    }
	    if(tempVehicle.containsKey(p)) tempVehicle.remove(p);
	    if(!activeVehicle.containsKey(p)) activeVehicle.put(p, v);
		inv.seatSelection(p.getOpenInventory().getTopInventory(), p, activeVehicle.get(p), false);
	}
	@EventHandler
	public void skinSelect(InventoryClickEvent e) {
		Player p = (Player) e.getWhoClicked();
		if(!(e.getView().getTopInventory().getHolder() instanceof VFInventoryHolder)) return;
		VFInventoryHolder h = (VFInventoryHolder) e.getView().getTopInventory().getHolder();
		if(!h.getType().equals(VFGUI.SKIN_SELECTION)) return;
		e.setCancelled(true);
		ActiveVehicle v = null;
		if(tempVehicle.containsKey(p)) v = tempVehicle.get(p);
		if(activeVehicle.containsKey(p)) v = activeVehicle.get(p);
		if(v == null) return;
		ItemStack i = e.getCurrentItem();
		if(i == null) return;
		if(i.getType().equals(Material.GRAY_STAINED_GLASS_PANE)) return;
	    if(v.isDestroyed()) {
	    	p.sendMessage("Vehicle is destroyed");
	    	return;
	    }
		
		if(i.getType().equals(Material.YELLOW_CONCRETE)) {
			p.sendMessage("§cAlready using this skin");
			return;
		}
		NamespacedKey key = new NamespacedKey(VehicleFramework.plugin, "vf_skin_id");
		v.changeSkin(i.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.STRING));
		inv.skinSelection(p.getOpenInventory().getTopInventory(), p, v, false);
	}

	@EventHandler
	public void containerClick(InventoryClickEvent e) {
		if (!(e.getView().getTopInventory().getHolder() instanceof VFInventoryHolder h)) {
			return;
		}
		if (!h.getType().equals(VFGUI.CONTAINER)) {
			return;
		}
		Optional<ActiveVehicle> opt = h.getVehicle();
		if (opt.isEmpty() || !opt.get().hasContainers()) {
			return;
		}
		Container c = opt.get().getContainerHandler().get(h.getId());
		if (c == null) {
			return;
		}
		ItemStack incoming = incomingToContainer(e);
		if (incoming == null || c.allows(incoming)) {
			return;
		}
		e.setCancelled(true);
		e.getWhoClicked().sendMessage("§cThat item is not allowed in this container.");
	}

	@EventHandler
	public void containerDrag(InventoryDragEvent e) {
		if (!(e.getView().getTopInventory().getHolder() instanceof VFInventoryHolder h)) {
			return;
		}
		if (!h.getType().equals(VFGUI.CONTAINER)) {
			return;
		}
		Optional<ActiveVehicle> opt = h.getVehicle();
		if (opt.isEmpty() || !opt.get().hasContainers()) {
			return;
		}
		Container c = opt.get().getContainerHandler().get(h.getId());
		if (c == null || c.allows(e.getOldCursor())) {
			return;
		}
		int topSize = e.getView().getTopInventory().getSize();
		for (int slot : e.getRawSlots()) {
			if (slot < topSize) {
				e.setCancelled(true);
				e.getWhoClicked().sendMessage("§cThat item is not allowed in this container.");
				return;
			}
		}
	}

	private static ItemStack incomingToContainer(InventoryClickEvent e) {
		Inventory top = e.getView().getTopInventory();
		Inventory clicked = e.getClickedInventory();
		InventoryAction action = e.getAction();
		if (action == InventoryAction.PLACE_ALL
				|| action == InventoryAction.PLACE_ONE
				|| action == InventoryAction.PLACE_SOME
				|| action == InventoryAction.SWAP_WITH_CURSOR) {
			if (clicked != null && clicked.equals(top)) {
				return e.getCursor();
			}
		}
		if (action == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
			if (clicked != null && !clicked.equals(top)) {
				return e.getCurrentItem();
			}
		}
		if (action == InventoryAction.HOTBAR_SWAP || action == InventoryAction.HOTBAR_MOVE_AND_READD) {
			if (clicked != null && clicked.equals(top)) {
				int hotbar = e.getHotbarButton();
				if (hotbar >= 0) {
					return e.getWhoClicked().getInventory().getItem(hotbar);
				}
			}
		}
		return null;
	}

	@EventHandler
	public void saveContainer(InventoryCloseEvent e) {
		if(e.getView().getTopInventory().getHolder() == null) return;
		if(!(e.getView().getTopInventory().getHolder() instanceof VFInventoryHolder)) return;
		VFInventoryHolder h = (VFInventoryHolder) e.getView().getTopInventory().getHolder();
		if(!h.getType().equals(VFGUI.CONTAINER)) return;
		Optional<ActiveVehicle> opt = h.getVehicle();
		if(opt.isEmpty()) return;
		Container c = opt.get().getContainerHandler().get(h.getId());
		if(c == null) return;
		if (e.getPlayer() instanceof Player player) {
			c.stripDisallowed(e.getView().getTopInventory(), player);
		}
		c.close(e.getView().getTopInventory());
	}
	
	public void inputPacket(Player p, float sideways, float forward, boolean space, boolean sneak) {
	    ActiveVehicle vehicle = activeVehicle.get(p);
	    if (vehicle == null) return;

	    if (vehicle.isTrain()) {
	    	ActiveVehicle loco = vehicle.ticketSource();
	    	if (loco.getSeatHandler() != null && loco.getSeatHandler().isCaptain(p)) {
	    		if (sideways > 0) {
	    			loco.getTrainHandler().holdJunction(TrackJunction.Side.LEFT);
	    		} else if (sideways < 0) {
	    			loco.getTrainHandler().holdJunction(TrackJunction.Side.RIGHT);
	    		}
	    	}
	    }

	    List<Keybind> keybinds = converter.convert(sideways, forward, space, sneak);
	    for (Keybind key : keybinds) {
	        vehicle.key(p, key);
	    }
	}

	//Database and persistence, unload vehicle safely and store them in on disk
	//Chunks and stuff is managed in the spawnmanager

	@SuppressWarnings("unchecked")
	public void unloadAll() {
		HashMap<Entity, ActiveVehicle> vc = (HashMap<Entity, ActiveVehicle>) vehicles.clone();
		for(Map.Entry<Entity, ActiveVehicle> entry : vc.entrySet()) {
			ActiveVehicle v = entry.getValue();
			unload(v);
		}
	}

	public void unload(ActiveVehicle v) {
		PersistenceLog.unload(v, v.isDestroyed() ? "destroyed" : "unload");
		if(!v.isDestroyed()) {
			db.saveVehicle(v);
			v.remove(VehicleRemoveReason.UNLOAD);
		} else {
			v.remove(VehicleRemoveReason.UNLOAD);
		}
	}

	public Map<Vehicle, Integer> getVehiclesByOwner(String owner) {
		Map<Vehicle, Integer> owned = new HashMap<>();
		for(Map.Entry<Entity, ActiveVehicle> entry : vehicles.entrySet()) {
			ActiveVehicle v = entry.getValue();
			if(v.getOwnerData().getOwner().equalsIgnoreCase(owner)) {
				Vehicle base = VehicleLoader.getByString(v.getId());
				if(base == null) continue;
				owned.put(base, owned.getOrDefault(base, 0) + 1);
			}
		}

		Map<String, Integer> stored = db.getStoredVehicleCountsByOwner(owner);
		for(Map.Entry<String, Integer> entry : stored.entrySet()) {
			Vehicle base = VehicleLoader.getByString(entry.getKey());
			if(base == null) continue;
			owned.put(base, owned.getOrDefault(base, 0) + entry.getValue());
		}

		return owned;
	}

	public Optional<Location> getOfflineLocation(String vehicleUuid) {
		if (vehicleUuid == null || vehicleUuid.isBlank()) {
			return Optional.empty();
		}
		ActiveVehicle live = get(vehicleUuid);
		if (live != null) {
			return Optional.of(live.getLocation());
		}
		Optional<Location> pending = SpawnManager.findSpawnLocation(vehicleUuid);
		if (pending.isPresent()) {
			return pending;
		}
		return db.getStoredSpawnLocation(vehicleUuid);
	}

	public List<OwnedVehicleSummary> listOwnedVehicles(String owner) {
		if (owner == null || owner.isBlank()) {
			return List.of();
		}
		return collectOwnedVehicles(
				liveOwner -> liveOwner != null && liveOwner.equalsIgnoreCase(owner),
				db.listStoredVehiclesByOwner(owner));
	}

	public List<OwnedVehicleSummary> listAllPlayerOwnedVehicles() {
		return collectOwnedVehicles(Database::isPlayerOwner, db.listStoredPlayerOwnedVehicles());
	}

	private List<OwnedVehicleSummary> collectOwnedVehicles(
			java.util.function.Predicate<String> liveOwnerMatch,
			List<StoredVehicleMeta> stored) {
		Map<String, OwnedVehicleSummary> byUuid = new LinkedHashMap<>();
		for (ActiveVehicle vehicle : vehicles.values()) {
			String liveOwner = vehicle.getOwnerData().getOwner();
			if (!liveOwnerMatch.test(liveOwner)) {
				continue;
			}
			String uuid = vehicle.getUUID();
			byUuid.put(
					uuid.toLowerCase(),
					new OwnedVehicleSummary(
							uuid,
							vehicle.getName(),
							vehicle.getId(),
							Optional.of(vehicle.getLocation()),
							true,
							liveOwner));
		}
		for (StoredVehicleMeta meta : stored) {
			String key = meta.getUuid().toLowerCase();
			if (byUuid.containsKey(key)) {
				continue;
			}
			byUuid.put(
					key,
					new OwnedVehicleSummary(
							meta.getUuid(),
							meta.getName(),
							meta.getTypeId(),
							getOfflineLocation(meta.getUuid()),
							false,
							meta.getOwner()));
		}
		return new ArrayList<>(byUuid.values());
	}

	public Optional<StoredVehicleMeta> readStoredVehicle(String vehicleUuid) {
		return db.readStoredVehicle(vehicleUuid);
	}

	public void clearOwnership(String vehicleUuid) {
		if (vehicleUuid == null || vehicleUuid.isBlank()) {
			return;
		}
		ActiveVehicle live = get(vehicleUuid);
		if (live != null) {
			live.getOwnerData().setOwner("none");
			live.getOwnerData().setWhiteListed(false);
		}
		db.clearStoredOwnership(vehicleUuid);
	}
}
