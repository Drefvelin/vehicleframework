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

## AI Tools
I used **ChatGPT** primarily for math-heavy components, such as understanding and verifying quaternion operations in the **BoneRotator**. It was especially helpful for breaking down complex concepts and providing alternate explanations while I learned JOML’s rotation systems.

For architecture, class design, and the overall structure of the plugin, I relied on my own judgment. In my experience, AI struggles with maintaining coherent object-oriented structure in larger projects, so all high-level design, class relationships, and system architecture were created and implemented by me.

Most of the plugin was written manually for efficiency and to maintain full control over the design. **ChatGPT** was also used to help format and refine this README.

## Configuration
The framework uses YAML files to define vehicle behavior, components, seats, and weapons.

Fuel types are in `fuel.yml`. Optional `sound` (namespaced key, volume, pitch) plays on click-refuel. `refuel-while-running: true` lets you add fuel while throttle is up (coal). Omit both to keep bucket-fill and idle-engine. Engine `refuel-states` still limits which vehicle states accept fuel.

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
    aa_turret:
      name: "§eAnti-Air Turret"
      seat: aa_turret_gunner        # Which seat controls this weapon
      health: 100.0                 # Weapon component health (default 100)
      turn-rate: 0.5                # Turret follow speed (manual and cursor); scales down with health
      aim-mode: cursor              # manual (WASD) or cursor (crosshair tracking; WASD aim disabled)
      cursor-range: 120             # Max ray distance when cursor aim finds no block/entity
      aim-vector: exit1.exit_align1 # Barrel direction bone pair (falls back to first bones entry)
      body-bone: "aa_turret_body"
      head-bone: "gun"
      head-axis: x                  # Pitch axis; use z for roll-based cannons
      rotation-limits:
        min-pitch: 0
        max-pitch: 85
      reload-time: 10
      cooldown: 10                  # ticks between shots (20 ticks = 1 second)
      projectile-damage: 12         # optional; outgoing hit/explosion damage (not vehicle incoming `damage:`)
      projectile-speed: 9.0         # optional; outgoing projectile speed (ammo speed or data.velocity)
      accepted-ammunition:
        - bullet
      bones:
        - exit1.exit_align1
      animations:
        shoot:
          - shoot
      data:
        shoot-sounds:
          sound:
            sound: "minecraft:guns.musket_1"
            pitch: 1.0
            volume: 6.0
      keybinds:
        W: WEAPON_UP
        S: WEAPON_DOWN
        A: WEAPON_LEFT
        D: WEAPON_RIGHT
        RIGHT_CLICK: WEAPON_RELOAD
        SPACE: WEAPON_SHOOT

    large_cannon:                   # Roll-axis cannon (opt-in cursor aim; see Weapon aim mode below)
      name: "§eFront Cannon"
      seat: gunner
      # aim-mode: cursor            # Uncomment to enable crosshair tracking
      # cursor-range: 120
      # turn-rate: 0.5
      body-bone: "weapon_body"
      head-bone: "cannon_controller"
      head-axis: z                  # Roll for elevation; use x for pitch turrets
      rotation-limits:
        min-roll: -75
        max-roll: 15
      reload-time: 5
      accepted-ammunition:
        - cannonball
      bones:
        - exit.exit_align
      keybinds:
        RIGHT_CLICK: WEAPON_RELOAD
        SPACE: WEAPON_SHOOT
```

### Weapon templates (optional)

Shared turrets live in `plugins/VehicleFramework/templates/weapons/*.yml`. Root keys are template ids. Vehicles reference them with `template:` and overlay per-model keys. Merge is instance-wins: nested sections (`data`, `keybinds`, `rotation-limits`, `aim-offset`, `animations`) deep-merge; lists (`bones`, `accepted-ammunition`, `damage`) replace when the vehicle sets them. Weapon YAML `damage:` is still incoming vulnerability, not outgoing projectile damage.

Unknown `template` ids skip that weapon and log an error. `/vf reload` reloads templates before vehicles. Templates cannot reference other templates.

**Built-in template ids**

| Template | Aim | Ammo | Notes |
|---|---|---|---|
| `gun_turret` | cursor | bullet | Light rifle turret, cursor-tracked |
| `aa_turret` | WASD | bullet | Anti-air, WASD-controlled |
| `naval_cannon` | WASD | cannonball | Cannon base, `head-axis: x` default |
| `flak_cannon` | WASD | flak_bullet | Emplacement flak, cooldown 15 |
| `autocannon` | WASD | flak_bullet | Dual-barrel flak, cooldown 12 |

**Projectile stat overrides** - set on the vehicle weapon (not the template) to tier up:

| Key | Type | Effect |
|---|---|---|
| `projectile-yield` | float | Explosion power (overrides ammo `yield`) |
| `projectile-radius` | int | Explosion radius in blocks (overrides ammo `radius`) |
| `projectile-explosive` | boolean | Force enable/disable explosion (overrides ammo `explosive`) |
| `projectile-cluster-amount` | int | Number of cluster bomblets spawned (overrides ammo `amount`) |

Balance `cooldown`, `projectile-damage`, `projectile-speed`, sounds, and limits in the template. Leave seat, bones, and barrel vector on the vehicle:

```yaml
# templates/weapons/gun_turret.yml
gun_turret:
  cooldown: 4
  projectile-damage: 12
  aim-mode: cursor
  # ... sounds, keybinds, limits
```

```yaml
# Light cannon - template only
naval_gun:
  template: naval_cannon
  seat: front_gunner
  body-bone: "gun_body"
  head-bone: "gun_rotator"
  rotation-limits:
    min-pitch: -60
    max-pitch: 20
  bones:
    - exit.exitalign

# Heavy cannon - template + stat overrides
large_cannon:
  template: naval_cannon
  seat: gunner
  body-bone: "weapon_body"
  head-bone: "cannon_controller"
  head-axis: z
  cooldown: 8
  projectile-damage: 28
  projectile-yield: 2.7
  projectile-radius: 10
  accepted-ammunition:
    - cannonball
    - clusterbomb
  bones:
    - exit.exit_align

# Capital cannon - highest tier
front_turret:
  template: naval_cannon
  seat: front_turret_gunner
  body-bone: "body"
  head-bone: "gun_front"
  head-axis: z
  cooldown: 12
  projectile-damage: 36
  projectile-yield: 3.5
  projectile-radius: 12
  projectile-cluster-amount: 26
  accepted-ammunition:
    - cannonball
    - clusterbomb
  bones:
    - exit.exitalign
```

### Armor and role templates

Incoming component vulnerability is a cause-to-multiplier map. Shared profiles live in `plugins/VehicleFramework/templates/armor/*.yml` and `plugins/VehicleFramework/templates/roles/*.yml`. Components set `armor:` and `role:`. Merge order is armor, then role, then an optional `damage:` map on the component. Later keys win.

Ship-wide blast numbers (torpedo, cannonball, bomb, explosion) live on the armor class. Change `torpedo` on `wooden`, `airship`, or `armored` instead of every hull and engine.

| Armor | Used by |
|---|---|
| `wooden` | gunboat, sloop |
| `airship` | cloudskimmer, gyrobomber, behemoth (wooden blast plus flak) |
| `armored` | ironclad, cruiser, torpedoboat, locomotive engine, small_car engine |
| `aircraft` | biplane, monoplane, bomber |
| `emplacement` | static guns, train cars, locomotive hull |
| `wagon` | horse cart, wooden cart, small_car hull |

Roles cover projectile and bullet (pumps also set `small_bomb`). Example:

```yaml
hull:
  health: 400.0
  armor: armored
  role: hull_armored
  damage:
    bullet: 0.2
```

A `damage:` list is still accepted when `armor` and `role` are omitted (weapons and leftover vehicle lists). Weapon YAML `damage:` is incoming weapon HP and does not use these templates.

### Death templates

Shared wreck sequences live in `plugins/VehicleFramework/templates/death/*.yml`. Vehicles set `death.template` and overlay nested keys. Instance-wins: nested `explode` / `sink` / `crash` deep-merge. `/vf reload` loads death templates before vehicles.

| Template | fragments | Extra |
|---|---|---|
| `explode_small` | 4 | explode sound |
| `explode_medium` | 10 | explode sound |
| `explode_large` | 30 | explode sound |
| `ship` | 30 | explode plus sink sounds |

Planes overlay crash on `explode_medium`:

```yaml
death:
  template: explode_medium
  explode:
    overrides:
      crashing:
        type: crash
        conditions:
          - state(flying)
  crash:
    nop: true
```

### Land terrain-follow (opt-in)

Default ground movement is still velocity-based. The climb hop is gone for every vehicle.

Carts and cars opt in per state. Planes and ships should omit the flag.

```yaml
states:
  ground:
    terrain-follow: true   # required to use kinematic snap + step-up
    step-height: 1         # max blocks to step up in the travel direction
    snap-speed: 0.25       # climb cap at rest (omit to use this default)
    climb-lead-ticks: 3    # extra down-rays this many ticks ahead at current speed
    climb-lead-factor: 1   # extra climb per tick = speed * this, capped by step-height
    air-gravity: 0.08      # Y acceleration per tick when airborne
    air-drag: 0.98         # XZ multiplier per tick when airborne
    ground-probes:         # optional; empty or missing bones fall back to the body
      - ground_fl
      - ground_fr
      - ground_bl
      - ground_br
```

When enabled, the vehicle **teleports** along flattened `move.movealign` (Y stripped). Visual tilt is `rotateToTarget` with stored steering yaw plus probe pitch/roll, so Euler extract cannot yaw the bone. Velocity is client interpol only and **Y is always 0 while grounded**, so vanilla gravity cannot add airtime. Extra down-rays sample ahead along heading (`speed * climb-lead-ticks`) so climb starts before the wheels reach a rise. Climb rate is `min(step-height, snap-speed + speed * climb-lead-factor)`. A 1-block STEP still raises in place until dest Y is at the step top, then slides horizontally in 0.02-block steps. If raise-in-place is blocked by the first STEP lip, it may ease back (about 0.1) then raise. Reverse does not climb-unstick upward. If the heading is blocked after that, it slides along X then Z (scrape along an edge). While XZ is blocked, dest Y is held (no downhill undo). Tilt does not move the hitbox.

If a destination AABB would overlap a solid, the slide stops at the last clear XZ instead of freezing or launching with a combined XZ+Y teleport. Hitbox dest Y uses the **highest** probe hit (support), not the average.

Vertical snap is clamped by `snap-speed` plus speed-scaled lead on **climb**. **Downhill** snaps to probe support in one tick when the destination AABB is clear (no snap-speed cap). After two consecutive probe misses the vehicle is **airborne**: engine XZ is frozen, last grounded momentum is kept, XZ is multiplied by `air-drag` each tick, and Y accelerates by `air-gravity` (velocity Y is not zeroed). The next probe hit returns to kinematic ground.

One-block water stays **ground**: dummy FLOATING (no YAML `floating:` state) is never used, and FLOATING requires water at the feet **and** one block above. Dummy FLYING is also ignored (cars stay GROUND if air is under a raised hitbox). Down-rays collide with fluids so a shallow river surface counts as support; water is still passable for XZ.

Parent probe locators to non-spinning wheel groups (`front_wheels` / `back_wheels`, or the car's `front_axle_turn` / `back_axle_turn`), not spinning rims. Rays are always world-down. Order is front-left, front-right, back-left, back-right looking along the move direction. With all four hits, pitch and roll are visual only (`ConvertedAngle.fromDirection` on the world front-back and left-right axes, clamped to ±25°); they do not move the hitbox. `body_controller` yaw is left to turning. Missing probe bones are logged once and skipped. If none resolve, snap uses the `body` bone as in Batch 1.

### Trains (custom spline tracks)

Land cars/carts use terrain-follow. Trains do not. Design, phases, and implementation batches: [`docs/trains.md`](docs/trains.md).

Phase 1: persist a **spline** (samples + segment health) separate from track **displays**; persist consist parent/child UUIDs; generate track between two anchors; 1x3 meshes on chunk load; loco/cars lerp along arc length `s`. Phase 2: recorder, tickets, coal-car inventory. Vanilla `Rail` motion is legacy until T5.

### Global config (`config.yml`)

- `weapon-degraded-reload-multiplier` (default `2.0`) - At 0% weapon health, reload takes this many times longer than at 100% health. Scales linearly between full and zero health.
- `weapon-aim-debug` (default `false`) - When true, shows an `END_ROD` particle at the resolved cursor aim target for the gunner and logs aim angles/target to console every 0.5s.
- `terrain-follow-debug` (default `false`) - When true, shows `END_ROD` particles at terrain-follow probe (or body) starts and down-ray hits for nearby players.
- `ground-engine-logging` (default `true`) - When true, appends each terrain-follow step (`state=` plus dest/vel, `lookaheadY`, `effSnap`, `air`, airborne `vx`, and geared-engine `gear`/`thr` when present) and each state swap (`state=A->B reason=... isDefault=...`) to `plugins/VehicleFramework/logs/ground_engine.log`.
- `wipe-log` (default `true`) - When true, deletes `ground_engine.log` and `track.log` on plugin start and `/vf reload`. Live `config.yml` is not overwritten; add these keys if they are missing.

### Weapon aim mode (optional)

| Key | Default | Purpose |
|-----|---------|---------|
| `aim-mode` | `manual` | `manual` uses WASD aim keybinds; `cursor` slews the turret toward the gunner's crosshair each tick while controlled |
| `aim-vector` | first `bones` entry | `base.align` bone pair defining barrel aim direction in world space (logs a warning when falling back) |
| `cursor-range` | `80` | Fallback distance along the look ray when no block or entity is hit |
| `turn-rate` | `0.5` | Follow speed in both modes; scales down with weapon health |

Optional `aim-offset` section (degrees) biases world yaw/pitch before alignment:

| Key | Default | Purpose |
|-----|---------|---------|
| `body-yaw` | `0` | Added to desired world yaw (use `180` if the barrel bone pair points backward) |
| `head-pitch` | `0` | World pitch bias (`head-axis: x`) |
| `head-yaw` | `0` | Extra yaw bias (`head-axis: y`) |
| `head-roll` | `0` | World pitch bias for roll-elevation cannons (`head-axis: z`) |

```yaml
aim-mode: cursor
aim-vector: exit.exit_align
aim-offset:
  body-yaw: 180
```

In `cursor` mode, WASD aim keys are ignored; shoot, reload, and weapon switch still work. The raycast ignores the firing vehicle and its passengers. Each tick converts the `aim-vector` world direction and the crosshair target into Minecraft yaw/pitch (`ConvertedAngle`) and drives body yaw + head elevation through the same incremental rotation path as manual aim. `head-axis: x` (pitch) suits AA turrets; `head-axis: z` (roll) suits front cannons with roll-based elevation. If the barrel points backward, set `aim-offset.body-yaw: 180` instead of flipping the aim vector.

**Enable cursor aim on a weapon:** add these keys to that weapon's block in the vehicle YAML (defaults keep manual WASD aim):

```yaml
aim-mode: cursor      # required
aim-vector: exit.exit_align  # recommended; falls back to first bones entry with a log warning
cursor-range: 120     # optional; fallback ray length (default 80)
turn-rate: 0.5        # optional; follow speed (default 0.5)
```

Reload or respawn the vehicle after editing. For first-time tuning, set `weapon-aim-debug: true` in `config.yml` to show the resolved target particle for the gunner.

**In-game checklist (cursor weapons):**

1. Turret follows crosshair smoothly (no snap)
2. Barrel direction (`aim-vector`) aligns toward the crosshair target
3. Block hit aims at impact point; open sky aims at `cursor-range`
4. Rotation limits are respected at the limit edge
5. Shoot and reload still work; WASD does not move the turret
6. Damaged weapons turn slower
7. Own vehicle is ignored by the aim raycast
8. `/vf reload` while seated does not jump or invert aim-offset tuning

### Weapon projectile damage (optional)

Outgoing hit and explosion damage comes from ammunition by default. A weapon can override that without mutating the shared ammo singleton. Weapon YAML `damage:` is incoming vulnerability (`DamageData`) and is not used here.

| Key | Default | Purpose |
|-----|---------|---------|
| `projectile-damage` | ammo `damage` | Outgoing projectile/explosion damage for this weapon only |
| `projectile-damage-type` | ammo `damage-type` | Outgoing damage type for hits and explosions from this weapon |

```yaml
projectile-damage: 12
# projectile-damage-type: bullet   # optional; omit to keep ammo type
```

### Weapon projectile speed (optional)

Outgoing projectile speed can also be overridden per weapon without copying ammunition. For `BULLET` ammo this replaces ammo `speed`. For cannonballs, clusters, and torpedoes it replaces `data.velocity` when set.

| Key | Default | Purpose |
|-----|---------|---------|
| `projectile-speed` | ammo `speed` or `data.velocity` | Outgoing projectile speed for this weapon only |
| `projectile-velocity` | same as `projectile-speed` | Alias of `projectile-speed` |

```yaml
projectile-speed: 9.0
# data.velocity: 3.0   # still used for entity projectiles when projectile-speed is omitted
```

### Bullet ammunition (`ammunition/*.yml`, type `BULLET`)

Vehicle bullets use per-tick simulated projectiles with gravity and raycast hit detection (close-range hits work from the muzzle onward).

| Key | Default | Purpose |
|-----|---------|---------|
| `range` | `80.0` | Max travel distance in blocks |
| `speed` | `7.0` | Initial velocity (blocks per tick) |
| `gravity` | `-0.05` | Vertical velocity added each tick |

Example:

```yaml
bullet:
  type: BULLET
  range: 80
  speed: 7.0
  gravity: -0.05
  damage: 4
  damage-type: bullet
```

Weapons may override outgoing damage and speed without copying the ammo definition. Example on `gun_turret`: `projectile-damage: 12` and `projectile-speed: 9.0`. Omit both keys on other weapons (for example a machine gun) to keep the ammunition defaults.
