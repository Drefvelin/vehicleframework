package net.tfminecraft.VehicleFramework.Protocol;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.InternalStructure;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.reflect.StructureModifier;

import net.tfminecraft.VehicleFramework.VehicleFramework;
import net.tfminecraft.VehicleFramework.Managers.VehicleManager;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class VehiclePacketListener {

    private final VehicleManager vehicleManager;
    private final ProtocolManager protocolManager;
    private final JavaPlugin plugin;

    private final Map<UUID, PlayerInputState> inputStates = new ConcurrentHashMap<>();

    public VehiclePacketListener(VehicleManager vehicleManager) {
        this.vehicleManager = vehicleManager;
        this.protocolManager = ProtocolLibrary.getProtocolManager();
        this.plugin = VehicleFramework.plugin;
    }

    public void register() {
        // Listen to STEER_VEHICLE packets
        protocolManager.addPacketListener(new PacketAdapter(plugin, PacketType.Play.Client.STEER_VEHICLE) {
            @Override
            public void onPacketReceiving(PacketEvent event) {
                Player player = event.getPlayer();
                var packet = event.getPacket();

                StructureModifier<InternalStructure> structures = packet.getStructures();
                if (structures.size() == 0) return;

                InternalStructure input = structures.read(0);

                boolean forward = input.getBooleans().read(0);
                boolean backward = input.getBooleans().read(1);
                boolean left = input.getBooleans().read(2);
                boolean right = input.getBooleans().read(3);
                boolean jump = input.getBooleans().read(4);
                boolean sneak = input.getBooleans().read(5);

                float sideways = 0;
                if (left) sideways += 1;
                if (right) sideways -= 1;

                float forwardMotion = 0;
                if (forward) forwardMotion += 1;
                if (backward) forwardMotion -= 1;

                inputStates.put(player.getUniqueId(), new PlayerInputState(sideways, forwardMotion, jump, sneak));
            }
        });

        // Start repeating task to simulate held input
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                PlayerInputState state = inputStates.get(player.getUniqueId());
                if (state == null) continue;

                vehicleManager.inputPacket(player, state.sideways, state.forward, state.jump, state.sneak);
            }
        }, 1L, 1L); // every tick
    }

    // Optional: Clear player input when they quit
    public void unregisterPlayer(Player player) {
        inputStates.remove(player.getUniqueId());
    }

    private static class PlayerInputState {
        public final float sideways;
        public final float forward;
        public final boolean jump;
        public final boolean sneak;

        public PlayerInputState(float sideways, float forward, boolean jump, boolean sneak) {
            this.sideways = sideways;
            this.forward = forward;
            this.jump = jump;
            this.sneak = sneak;
        }
    }
}
