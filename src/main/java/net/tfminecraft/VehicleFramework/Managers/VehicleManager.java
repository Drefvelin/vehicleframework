package net.tfminecraft.VehicleFramework.Managers;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Boat;
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
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.vehicle.VehicleMoveEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import io.lumine.mythic.bukkit.events.MythicMobDespawnEvent;
import me.Plugins.TLibs.TLibs;
import me.Plugins.TLibs.Enums.APIType;
import me.Plugins.TLibs.Objects.API.ItemAPI;
import me.Plugins.TLibs.Objects.API.SubAPI.StringFormatter;
import net.Indyuce.mmoitems.stat.type.NameData;
import net.tfminecraft.VehicleFramework.VFLogger;
import net.tfminecraft.VehicleFramework.VehicleFramework;
import net.tfminecraft.VehicleFramework.Cache.Cache;
import net.tfminecraft.VehicleFramework.Data.NamingData;
import net.tfminecraft.VehicleFramework.Database.Database;
import net.tfminecraft.VehicleFramework.Database.IncompleteVehicle;
import net.tfminecraft.VehicleFramework.Enums.Component;
import net.tfminecraft.VehicleFramework.Enums.Input;
import net.tfminecraft.VehicleFramework.Enums.Keybind;
import net.tfminecraft.VehicleFramework.Enums.SeatType;
import net.tfminecraft.VehicleFramework.Enums.VFGUI;
import net.tfminecraft.VehicleFramework.Events.VFEntityDamageEvent;
import net.tfminecraft.VehicleFramework.Loaders.FuelLoader;
import net.tfminecraft.VehicleFramework.Loaders.VehicleLoader;
import net.tfminecraft.VehicleFramework.Managers.Inventory.VFInventoryHolder;
import net.tfminecraft.VehicleFramework.Managers.Spawner.VehicleSpawner;
import net.tfminecraft.VehicleFramework.Protocol.PacketConverter;
import net.tfminecraft.VehicleFramework.Vehicles.ActiveVehicle;
import net.tfminecraft.VehicleFramework.Vehicles.Vehicle;
import net.tfminecraft.VehicleFramework.Vehicles.Component.Harness;
import net.tfminecraft.VehicleFramework.Vehicles.Handlers.TowHandler;
import net.tfminecraft.VehicleFramework.Vehicles.Handlers.Container.Container;
import net.tfminecraft.VehicleFramework.Vehicles.Seat.Seat;

public class VehicleManager implements Listener{
	private ItemAPI api = (ItemAPI) TLibs.getApiInstance(APIType.ITEM_API);
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
	
	private HashMap<Player, ActiveVehicle> tow = new HashMap<>();
	
	private HashMap<Entity, ActiveVehicle> vehicles = new HashMap<>();

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
		return vehicle;
	}
	
	public void start() {
		spawnManager.start();
		vehicleFastTickCycle();
		vehicleSlowTickCycle();
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
	    inv.seatSelection(null, p, v, true);
	    tempVehicle.put(p, v);
	}
	
	public void skinInteract(Player p, ActiveVehicle v) {
	    inv.skinSelection(null, p, v, true);
	    tempVehicle.put(p, v);
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
	public void vehicleInteract(PlayerInteractEntityEvent e){
		Entity entity = e.getRightClicked();
		for (Map.Entry<Entity, ActiveVehicle> entry : vehicles.entrySet()) {
        	ActiveVehicle v = entry.getValue();
            if(v.isPassenger(entity, true)) {
            	e.setCancelled(true);
            	return;
            }
        }
		if(!vehicles.containsKey(entity)) return;
		ActiveVehicle v = vehicles.get(entity);
		Player p = e.getPlayer();
		if(cooldown.containsKey(p)) {
			if(cooldown.get(p) > System.currentTimeMillis()) {
				return;
			}
		}
		cooldown.put(p, System.currentTimeMillis()+100);
		//Containers
		if(v.hasContainers()) {
			if(v.getContainerHandler().open(p)) return;
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
	public void nameVehicle(AsyncPlayerChatEvent e) {
		Player p = e.getPlayer();
		if(!naming.containsKey(p)) return;
		e.setCancelled(true);
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
        		l.setHealth(Math.max(0, l.getHealth() - event.getDamage()));
        	}
        }
	}
	@EventHandler
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
			if(p.getGameMode().equals(GameMode.SPECTATOR) || p.getGameMode().equals(GameMode.CREATIVE)) return;
			for(Map.Entry<Entity, ActiveVehicle> entry : vehicles.entrySet()) {
				if(entry.getValue().isPassenger(p, false)) {
					e.setDamage(e.getDamage()/2);
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

	public void mount(Player p, String seat, ActiveVehicle v) {
		Seat s = v.getSeatHandler().getSeat(seat);
		if(s == null) return;
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
		if(e.getSlot() == 26) {
			v.dismountPassenger(p, false);
			return;
		}
		NamespacedKey key = new NamespacedKey(VehicleFramework.plugin, "vf_seat_id");
		String id = i.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.STRING);
		if(id == null) return;
		Seat seat = v.getSeat(id);
		
		if(i.getType().equals(Material.YELLOW_CONCRETE)) {
			p.sendMessage("§cSeat is occupied");
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
	public void saveContainer(InventoryCloseEvent e) {
		if(e.getView().getTopInventory().getHolder() == null) return;
		if(!(e.getView().getTopInventory().getHolder() instanceof VFInventoryHolder)) return;
		VFInventoryHolder h = (VFInventoryHolder) e.getView().getTopInventory().getHolder();
		if(!h.getType().equals(VFGUI.CONTAINER)) return;
		Optional<ActiveVehicle> opt = h.getVehicle();
		if(opt.isEmpty()) return;
		Container c = opt.get().getContainerHandler().get(h.getId());
		if(c == null) return;
		c.close(e.getView().getTopInventory());
	}

	@EventHandler
    public void onBoatMove(VehicleMoveEvent event) {
        if (!(event.getVehicle() instanceof Boat boat)) return;

        Vector velocity = boat.getVelocity();
        double speed = velocity.length();

        if (speed > 0.1) {
            Vector capped = velocity.clone().normalize().multiply(0.1);
            boat.setVelocity(capped);
        }
    }
	
	public void inputPacket(Player p, float sideways, float forward, boolean space, boolean sneak) {
	    ActiveVehicle vehicle = activeVehicle.get(p);
	    if (vehicle == null) return;

	    List<Keybind> keybinds = converter.convert(sideways, forward, space, sneak);

	    if (!keybinds.isEmpty()) {
	        Bukkit.getScheduler().runTask(VehicleFramework.plugin, () -> {
	            for (Keybind key : keybinds) {
	                vehicle.key(p, key);
	            }
	        });
	    }
	}

	//Database and persistence, unload vehicle safely and store them in on disk
	//Chunks and stuff is managed in the spawnmanager

	@EventHandler
	public void despawnEvent(MythicMobDespawnEvent e) {
		ActiveVehicle v = get(e.getEntity());
		if(get(e.getEntity()) != null) {
			unload(v);
		}
	}

	@SuppressWarnings("unchecked")
	public void unloadAll() {
		HashMap<Entity, ActiveVehicle> vc = (HashMap<Entity, ActiveVehicle>) vehicles.clone();
		for(Map.Entry<Entity, ActiveVehicle> entry : vc.entrySet()) {
			ActiveVehicle v = entry.getValue();
			unload(v);
		}
	}

	public void unload(ActiveVehicle v) {
		if(!v.isDestroyed()) {
			db.saveVehicle(v);
			v.remove();
		} else {
			v.getEntity().remove();
		}
	}
}
