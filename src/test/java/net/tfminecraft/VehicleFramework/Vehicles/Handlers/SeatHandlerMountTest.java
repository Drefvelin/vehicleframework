package net.tfminecraft.VehicleFramework.Vehicles.Handlers;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.junit.jupiter.api.Test;

import com.ticxo.modelengine.api.model.bone.manager.MountManager;
import com.ticxo.modelengine.api.model.bone.type.Mount;
import com.ticxo.modelengine.api.mount.controller.MountControllerTypes;

import net.tfminecraft.VehicleFramework.Enums.SeatType;
import net.tfminecraft.VehicleFramework.Vehicles.Seat.Seat;

class SeatHandlerMountTest {
    @Test
    void rejectedMountDoesNotOccupySeatOrRegisterPassenger() throws Exception {
        Fixture f = new Fixture();
        f.accept = false;
        f.handler.addPassenger(f.entity, f.seat);
        assertFalse(f.seat.isOccupied());
        assertFalse(f.handler.isPassenger(f.entity));
    }

    @Test
    void missingManagerDoesNotRegisterPassenger() throws Exception {
        Fixture f = new Fixture();
        f.setManager(null);
        f.handler.addPassenger(f.entity, f.seat);
        assertFalse(f.seat.isOccupied());
        assertFalse(f.handler.hasPassengers());
    }

    @Test
    void successfulMountRegistersPassengerAndLeavesShiftToVehicleFramework() throws Exception {
        Fixture f = new Fixture();
        f.handler.addPassenger(f.entity, f.seat);
        assertSame(f.entity, f.seat.getEntity());
        assertTrue(f.handler.isPassenger(f.entity));
        assertTrue(f.handler.isMounted(f.entity));
        assertSame(MountControllerTypes.WALKING_FORCE, f.controller);
    }

    @Test
    void occupiedSeatIsNotMountedOrOverwritten() throws Exception {
        Fixture f = new Fixture();
        Entity occupant = entity(UUID.randomUUID(), new boolean[]{true});
        f.seat.mount(occupant);
        f.handler.addPassenger(f.entity, f.seat);
        assertSame(occupant, f.seat.getEntity());
        assertFalse(f.handler.isPassenger(f.entity));
        assertEquals(0, f.mounts);
    }

    @Test
    void detachedPassengerWithStaleMapIsDismountedBeforeRecovery() throws Exception {
        Fixture f = new Fixture();
        f.handler.addPassenger(f.entity, f.seat);
        f.attached[0] = false; // ME still lists the rider, Bukkit no longer does.
        assertFalse(f.handler.isMounted(f.entity), "Detached riders must not control the train");
        f.handler.slowTick();
        assertEquals(2, f.mounts);
        assertEquals(1, f.dismounts);
        assertTrue(f.attached[0]);
        assertTrue(f.handler.isMounted(f.entity));
        assertTrue(f.handler.isPassenger(f.entity));
        assertSame(MountControllerTypes.WALKING_FORCE, f.controller);
    }

    @Test
    void failedRecoveryClearsSeatAndPassenger() throws Exception {
        Fixture f = new Fixture();
        f.handler.addPassenger(f.entity, f.seat);
        f.attached[0] = false;
        f.accept = false;
        f.handler.slowTick();
        assertFalse(f.seat.isOccupied());
        assertFalse(f.handler.isPassenger(f.entity));
        assertFalse(f.map.containsKey(f.entity));
    }

    @Test
    void healthyPassengerIsNotRemounted() throws Exception {
        Fixture f = new Fixture();
        f.handler.addPassenger(f.entity, f.seat);
        f.handler.slowTick();
        assertEquals(1, f.mounts);
        assertEquals(0, f.dismounts);
    }

    @Test
    void modelReplacementRemountsPassengerOnNewManager() throws Exception {
        Fixture f = new Fixture();
        f.handler.addPassenger(f.entity, f.seat);
        f.map.clear(); // New model has no passenger entry yet.
        f.attached[0] = false;
        f.handler.slowTick();
        assertEquals(2, f.mounts);
        assertTrue(f.handler.isPassenger(f.entity));
        assertTrue(f.attached[0]);
    }

    @Test
    void physicalMountWithoutManagerEntryDoesNotGrantControl() throws Exception {
        Fixture f = new Fixture();
        f.handler.addPassenger(f.entity, f.seat);
        f.map.clear();
        assertFalse(f.handler.isMounted(f.entity));
    }

    @Test
    void mountManagerLossClearsOccupancy() throws Exception {
        Fixture f = new Fixture();
        f.handler.addPassenger(f.entity, f.seat);
        f.setManager(null);
        f.handler.slowTick();
        assertFalse(f.handler.hasPassengers());
        assertFalse(f.seat.isOccupied());
        assertFalse(f.handler.isMounted(f.entity));
    }

    @Test
    void switchingToOccupiedSeatKeepsOriginalSeat() throws Exception {
        Fixture f = new Fixture();
        f.handler.addPassenger(f.entity, f.seat);
        Seat occupied = new Seat(SeatType.PASSENGER, "passenger");
        occupied.mount(entity(UUID.randomUUID(), new boolean[]{true}));
        f.handler.getSeats().add(occupied);
        f.handler.changeSeat(f.entity, occupied);
        assertSame(f.entity, f.seat.getEntity());
        assertTrue(f.handler.isMounted(f.entity));
        assertEquals(0, f.dismounts);
    }

    @Test
    void rejectedSeatSwitchDoesNotLeaveGhostPassenger() throws Exception {
        Fixture f = new Fixture();
        f.handler.addPassenger(f.entity, f.seat);
        Seat other = new Seat(SeatType.PASSENGER, "passenger");
        f.handler.getSeats().add(other);
        f.accept = false;
        f.handler.changeSeat(f.entity, other);
        assertFalse(f.handler.hasPassengers());
        assertFalse(f.seat.isOccupied());
        assertFalse(other.isOccupied());
    }

    private static class Fixture {
        final SeatHandler handler = new SeatHandler(List.of(), null);
        final Seat seat = new Seat(SeatType.CAPTAIN, "driver");
        final boolean[] attached = {false};
        final Entity entity = entity(UUID.randomUUID(), attached);
        final Map<Entity, Mount> map = new HashMap<>();
        boolean accept = true;
        int mounts;
        int dismounts;
        Object controller;

        Fixture() throws Exception {
            handler.getSeats().add(seat);
            setManager((MountManager) Proxy.newProxyInstance(MountManager.class.getClassLoader(),
                    new Class<?>[]{MountManager.class}, (proxy, method, args) -> {
                        switch (method.getName()) {
                            case "mountPassenger":
                                mounts++;
                                controller = args[2];
                                if (accept) {
                                    map.put(entity, null);
                                    attached[0] = true;
                                }
                                return accept;
                            case "dismountPassenger":
                                dismounts++;
                                map.remove(entity);
                                attached[0] = false;
                                return null;
                            case "getPassengerSeatMap": return map;
                            default: throw new AssertionError("Unexpected mount API call: " + method);
                        }
                    }));
        }

        void setManager(MountManager manager) throws Exception {
            Field field = SeatHandler.class.getDeclaredField("manager");
            field.setAccessible(true);
            field.set(handler, manager);
        }
    }

    private static Entity entity(UUID id, boolean[] attached) {
        return (Entity) Proxy.newProxyInstance(Entity.class.getClassLoader(), new Class<?>[]{Entity.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getUniqueId" -> id;
                    case "getLocation" -> new Location(null, 10, 64, 20);
                    case "isInsideVehicle" -> attached[0];
                    case "hashCode" -> id.hashCode();
                    case "equals" -> proxy == args[0];
                    case "toString" -> "passenger-" + id;
                    default -> throw new AssertionError("Unexpected entity API call: " + method);
                });
    }
}
