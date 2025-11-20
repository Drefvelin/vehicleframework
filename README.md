# VehicleFramework
A highly configurable system to allow semi-realistic vehicles with weapons in Minecraft

You will not be able to run this as a standalone program, due to it being dependent on other plugins and the spigot server environment.

## Why This Project Is Interesting
This framework implements several systems that do not exist in the standard Spigot API, including:

- **Full 3D rotation (pitch/yaw/roll)** - Minecraft normally exposes only yaw and limited pitch control. I implemented full rotational freedom for vehicles and weapon systems.
- **Player-controlled turrets** - Instead of simply spawning a projectile in the direction the player is looking (the default behavior), the turret physically rotates, aims, and fires based on its own orientation. The projectile direction is derived from the turret’s current rotation, not the player’s.

To achieve this, I leveraged the external plugin **ModelEngine**, but used parts of its API in unconventional ways. For example, the control system is built using the plugin’s manual bone animation interface, which is normally meant for running animations — not for real-time input-driven rotation. Because ModelEngine uses **JOML** for its math, I integrated JOML into my calculations to ensure compatibility and seamless model manipulation.

## Features
- Highly customizable vehicles based on YAML configuration files (Controlled from [ActiveVehicle.java](src/main/java/net/tfminecraft/VehicleFramework/Vehicles/ActiveVehicle.java))
- Advanced movement and rotation logic with Joml and ModelEngine ([BoneRotator.java](src/main/java/net/tfminecraft/VehicleFramework/Bones/BoneRotator.java))
- Json database with crash handling and dynamic loading based on chunks ([Database.java](src/main/java/net/tfminecraft/VehicleFramework/Database/Database.java))


## Technical Overview
- Java 17, Spigot API 1.20
- Built using Maven
### Architecture:
- Main class intializes managers and plugin setup ([VehicleFramework.java](src/main/java/net/tfminecraft/VehicleFramework/VehicleFramework.java))
- Configuration files loaded and stored as templates on boot ([Loaders](src/main/java/net/tfminecraft/VehicleFramework/Loaders/))
- VehicleManager handles spawning, despawning, persistence and general input and packets ([VehicleManager.java](src/main/java/net/tfminecraft/VehicleFramework/Managers/VehicleManager.java))
- ActiveVehicle is a very deep class with several Handler classes that handle various areas of operation ([Handlers](src/main/java/net/tfminecraft/VehicleFramework/Vehicles/Handlers/))
- Weapons can exist on vehicles (in the WeaponHandler), they are connected by seat ([Weapons](src/main/java/net/tfminecraft/VehicleFramework/Weapons/) [WeaponHandler.java](src/main/java/net/tfminecraft/VehicleFramework/Vehicles/Handlers/WeaponHandler.java))


## Key Challenges Solved

### Robust Persistence & Crash Recovery
Since all runtime data is lost when a Minecraft server restarts, I implemented a custom JSON-based persistence system. A major challenge was handling unexpected server crashes, where no clean save could occur. I solved this by adding:

- a 5-minute snapshot system  
- a **dirty bit** to detect unclean shutdowns  
- automatic restoration of the last known good snapshot  

To reduce RAM usage, vehicles are not fully loaded until a player is close enough. I built a two-tier loading system where lightweight `SpawnLocation` objects are stored, and the full `ActiveVehicle` is created only when needed. This originally introduced memory leaks, which I resolved by ensuring both objects share the same UUID and by tightening lifecycle management.

### Full Project Refactor as Scope Expanded
The current plugin is far larger and more complex than what I originally planned. As I expanded into more advanced usage of the ModelEngine API, the initial architecture became a bottleneck. To fix this, I performed a full project refactor - keeping the original concept, but rebuilding the entire foundation.

This rewrite greatly improved maintainability, allowed much cleaner separation of concerns, and enabled the advanced features the project has today.

### Quaternions
One of the more challenging parts of this project was implementing smooth, physically consistent rotation for aircraft. What began as a simple idea - making the tail of a plane dip during landing - turned into a deep dive into the JOML math library and the fundamentals of quaternion-based rotation.

Along the way, I learned the practical differences between Euler angles and quaternions, why quaternions avoid gimbal lock, and how to blend rotations smoothly regardless of input. After about a week of experimentation, testing, and reading documentation, I built a fully consistent quaternion-driven rotation system that works regardless of the starting rotation.

## Configuration
The framework uses YAML files to define vehicle behavior, components, seats, and weapons.  
Here is a short example excerpt from the configuration of the first vehicle I made, a fixed artillery piece:

```yaml
fixed_artillery: # First vehicle I made
  name: "§l§eFixed Artillery"
  model: fixed_artillery            # Refers to the skin id
  fixed: true

  skins:
    fixed_artillery:
      name: "Fixed Artillery"
      model: fixed_artillery        # Refers to the actual ModelEngine model

  death:
    explode:                        # Behavior when destroyed by explosion
      fragments: 4
      duration: 320
      sounds:
        explode:
          sound: "vehicleframework:explosion"
          pitch: 1.0
          volume: 8.0

  behaviour:                        # Fixed weapon; only rotators are active
    rotator: weapon_body
    vector: weapon_body.weapon_body # Same bone = zero vector = no movement

  states:
    ground:                    # Other vehicles might define air/sea states
      keybinds:
        SHIFT: SEAT_SELECTION

  components:                       # Vehicle components
    hull:
      health: 80.0
      repair-time: 100
      damage-chance: 1.0
      damage:
        - entity_attack(0.1)
        - projectile(0.3)
        - entity_explosion(1.3)
        - bullet(0.4)
        - torpedo(0.2)
        - small_bomb(3.0)
        - cannonball(2.0)
      vfx:                          # Fire VFX when hull is burning
        - weapon_body

  seats:                            # Seat list with seat types
    - captain(gunner)

  weapons:                          # Weapon definitions
    flak_cannon:
      name: "§eFlak Cannon"
      seat: gunner                  # Which seat controls this weapon
      body-bone: "weapon_body"
      head-bone: "cannon_controller"
      head-axis: z                  # Axis used for pitch rotation
      rotation-limits:              # Limits keep aiming realistic
        min-roll: -75
        max-roll: 15
      reload-time: 5
      cooldown: 15
      accepted-ammunition:
        - cannonball
        - clusterbomb
      bones:
        - exit.exitalign        # Projectile is fired along this bone vector
      animations:
        shoot:
          - shoot
      data:                         # Sound and particle data
        shoot-sounds:
          sound:
            sound: "minecraft:entity.generic.explode"
            pitch: 1.0
            volume: 6.0
        reload-sounds:
          sound:
            sound: "minecraft:block.anvil.land"
            pitch: 1.0
            volume: 1.0
        reload-start-sounds:
          sound:
            sound: "minecraft:block.anvil.land"
            pitch: 1.0
            volume: 1.0
        particles:
          particle1:
            particle: FLAME
            amount: 100
            spread: 0.3
            speed: 1.2
          particle2:
            particle: CAMPFIRE_COSY_SMOKE
            amount: 100
            spread: 1.4
            speed: 0.2
          particle3:
            particle: CAMPFIRE_COSY_SMOKE
            amount: 50
            spread: 0.7
            speed: 0.4
      keybinds:
        W: WEAPON_UP
        S: WEAPON_DOWN
        A: WEAPON_LEFT
        D: WEAPON_RIGHT
        SPACE_W: WEAPON_UP
        SPACE_S: WEAPON_DOWN
        SPACE_A: WEAPON_LEFT
        SPACE_D: WEAPON_RIGHT
        RIGHT_CLICK: WEAPON_RELOAD
        SPACE: WEAPON_SHOOT
