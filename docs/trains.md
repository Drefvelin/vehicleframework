# Trains: spline tracks (locked design)

Ground kinematic follow is **done**. Trains are a separate system. Do not mix terrain-follow into rail motion.

This document is the source of truth for spline tracks. Vanilla `Rail` blocks are not used for trains.

## Goals (product)

- Scenic routes with **gentle turns** (about 25 degrees by default), not only Minecraft 90-degree rail shapes.
- **Passenger consists** later: pay to sit, travel station to station.
- Tracks can be **damaged** (bomb, break). Motion and unloaded logic read **data**, not whether a display entity exists.

## Two phases

| Phase | In scope | Out of scope |
|-------|----------|----------------|
| **1** | Spline object, persist tracks, persist consist links, generate between anchors, 1x3 (then optional longer straights) visuals, travel by lerp on the spline, existing chunk load/unload | Recorder item, tickets/stations/fares, whitelist coal inventory, loco pulling coal from the car behind, force-loaded corridors, unloaded `s += dt` autopilot |
| **2** | Circuit recorder, whitelist containers, coal car to loco, tickets, optional chunk tickets / unloaded advance | - |

Phase 1 **does** save the consist and the spline. Phase 1 **does not** invent a new vehicle streamer. Vehicles keep `SpawnManager` + per-car JSON. Track **displays** spawn/despawn on **chunk** load/unload.

## Core rule: spline is the object

```
Spline (JSON, always loaded or loaded per world)
  samples     motion + bake source
  segments    health / broken (bombs write here)
  visuals     ItemDisplay / ModelEngine, optional, chunk-local
Consist       ordered cars + splineId + arc length s
```

- **Anything that touches the rail** (explosion, break, admin repair) updates **segment flags** on the spline. Displays are rebuilt from that, or hidden if the chunk is unloaded.
- A missing or unloaded display **must not** change the path. Unloaded trains can later advance `s` on this same data; phase 1 still only moves loaded vehicles.
- Cars **do not** physics-ride the mesh. Each car is a point at arc length `s - spacing` with rotation from the sample tangent. At a turnout, cars stay on the stem until they reach the frog (they are not copied onto the loco’s spline).

### Motion (1D)

- Locomotive: `s +=` signed panel speed (throttle already signed; reverse is negative). `travelSign` is `+1` / `-1` from that speed for junctions and car rewind. No `travelSign * speed` (that inverted forward when bind facing was wrong).
- Car `i`: `s_i` is coupler spacing **behind** the parent along stem + optional branch, not always `s_loco` on one spline.
- Position: teleport the armor stand **XYZ** only. Heading is the **+s** sample tangent (`ConvertedAngle.fromDirection`), not the teleport delta and not `travelSign` (reverse does not spin the loco). Bone yaw is inverted; pitch is the sample pitch.
- **Circuit:** if `loop` is true, `advance` wraps past the start/end join. Ends within `tracks.join-distance` persist `loop: true` (extend, JSON load, or `closeLoop`).
- **Junctions:** captain holds **A** or **D** within `junction-arm-distance` (default 16) along `s` of the next frog to arm that side. The arm latches until the frog. Matching turnout `side` diverges, otherwise through. Chat tells you if you armed, if the frog is too far, and through vs diverge with why. Backing on the branch to `s = 0` rejoins the stem. Reverse clears the arm.
- **Broken segment:** stop (or clamp `s` before the break). Stay on the spline.

### Spacing

Reuse `behaviour.train` front/back connector bones ([`Connector`](../src/main/java/net/tfminecraft/VehicleFramework/Vehicles/Handlers/Train/Connector.java)). Spacing is bone distance along the spline.

## Visuals (phase 1)

- **Source of truth:** samples. **Meshes:** anchored to the spline at bake intervals.
- **v1 meshes:** 1x3 JSON / ModelEngine piece (one plank, rails on either side), about **one per block** on curves and slopes.
- **Later in phase 1 (optional batch):** merge long collinear, same-grade runs into 2x3 / 3x3 (cap at whatever the model allows; 3x3 is the expected max). 1x3 remains for turns and grade changes.
- Spawn displays when the **chunk loads**; remove when it unloads. No custom proximity streamer in phase 1.
- Bake is **cached** on the spline (`visuals()`). Chunk spawn iterates that list; it does not rebake per chunk. `replace` invalidates the cache.
- `/vf reload` reloads spline JSON and vehicle configs. It does **not** despawn or respawn track displays. Lay, join, dig, and break still rebake.
- Target: a player at max render distance might see a few hundred 1x3 displays. If that hitchs, merge straights before changing spawn rules.

## Authoring (phase 1)

Track items, lay rules, and train debug logging live in [`trains.yml`](../src/main/resources/trains.yml) (`plugins/VehicleFramework/trains.yml`). `debug-logging: true` writes `logs/track.log`, `logs/persistence.log`, and `logs/recorder.log`. Persistence pose lines are entity location only. Recorder `POSE` / `SAMPLE` / `JUNCTION` include `eyaw` (stand), `dyaw` (bone `driveYaw`), and on `POSE` also `myaw`/`mpitch` (move `fromDirection`) and `pyaw`/`ppitch` (sample). `POSE` is rate-limited (~250 ms) so it logs without the tape item.

One spline per track (no stored sections). A **stroke** is one lay with the configured layer item (`item-layer`, default `v.iron_shovel`):

- Left-click: **start location** (block or existing track). Click an existing end to join that track.
- Right-click: **end location**. New track is a straight line in XZ from start to end (player look is ignored). Click within `join-distance` of an existing **end** to join: same track extends, or **two tracks link into one** if start is on one end and end is on another. Join curves from the **track** heading. Crossing the middle of a track still refuses (use the junction item for a turnout).
- **Creative / spectator:** the spline is saved, then displays rebake in one step. One place sound + particles at the last sample (`build` in `trains.yml`).
- **Survival / adventure:** same save, but displays grow along the new stroke one sample every `build.interval-ticks` (default 4, five per second). Prefix rebakes so collinear runs become medium then large. Each step plays `build.sound` and particles at the new sample, and swings the main hand if `build.swing` is true. Set `build.interval-ticks` to `0` to always place instantly. Connecting two tracks or closing a loop is still instant plus one burst.
- Remover item (`item-remover`, default `v.iron_pickaxe`): left-click **digs** a sample (interior dig **splits** into two tracks).
- Junction item (`item-junction`, default `v.diamond_shovel`): right-click **existing track** (interior allowed) to start a junction (nothing is saved yet). Then layer **right-click** lays **one** turnout from that frog. The junction is saved only if that branch lays. Through stays the original spline. Layer **left-click** while a junction is pending cancels it and marks a normal start.
- `min-junction-spacing` (default 16) along stem arc `s` (loop wrap). One branch per junction (no 3-way). Joining a branch tip into another track is not shipped.
- `max-junction-length` (default 32): the turnout from frog to click cannot be longer than that (straight-line or along the laid curve).
- `junction-arm-distance` (default 16): press A/D this far before the frog (facing approach only) to throw the switch. A = LEFT, D = RIGHT vs the stored lay-time side: matching side throws diverge, the other key throws through. The lever stays until thrown again. Every train follows `thrown` (including unmanned). Tape is throttle only.
- `item-switch` (default `ia.tfmc:railroad_switch`) plus `switch.offset-along` / `offset-out` / `offset-y` / `yaw-inward` / `throw-degrees` / `throw-degrees-per-second` place and animate the ItemDisplay on the through side of the frog. Chunk load respawns it at the saved pose; the entity is not persistent.
- A junction branch still needs a 3-wide by 3-tall corridor of passable blocks. It may cross existing track (including the stem); overlapping tracks do not refuse a turnout.
- `max-turn-degrees` (default 25) and `min-lay-distance` (default 8): refuse if the stroke is too short, or if a **join** turn (heading change from the existing end) is too sharp.
- Grade: stay **flat** as long as possible, then climb at `desired-grade-degrees` (default 6), never steeper than `max-grade-degrees` (default 10). Chat says **slope is too steep** if the end is too high for the run.
- Clearance: a 3-wide by 3-tall corridor must be passable (air and plants are fine; solids and overlapping tracks are not).
- Moving trains spawn `fx` gravel `BLOCK_CRACK` crumbs on that 3-wide ballast (rotated with the rail yaw). `fx.sound` is a string (vanilla `minecraft:block.stone.break` or a custom namespaced sound). Copy `fx` and `build` into an existing `plugins/VehicleFramework/trains.yml`.
- `/vf track delete <uuid>` removes the whole track. `/vf track start` and `/vf track end` use your current position.
- `/vf track dump` appends a network snapshot to `logs/track.log` (`DUMP`, `SPLINE`, `PT` every 32 along `s`, then `JUNCTION` frogs including `thrown`). The same dump runs on plugin load. `JUNCTION_DROP` is written if a junction JSON is skipped (`no-stem` / `no-branch`) or cancelled as incomplete.

No free-walk recorder tape in phase 1.

## Persistence

### Spline

`plugins/VehicleFramework/data/tracks/<world>/<splineUuid>.json`

Suggested fields: `id`, `world`, `loop`, `samples[]` (`x,y,z,yaw,pitch,s`), `segments[]` (`fromIndex`, `broken`, `health`). `loop` is **true** for a circuit: `closeLoop`, or when first and last samples are within `tracks.join-distance` after extend / load. Optional `visuals[]` in JSON is regenerable; runtime bake lives on the spline object.

Index which chunks a spline touches so `ChunkLoadEvent` can spawn only local displays.

Junctions are **not** nested in spline JSON:

`plugins/VehicleFramework/data/tracks/<world>/junctions/<junctionUuid>.json`

Fields: `id`, `stem`, `s`, `facing`, `side`, `thrown` (diverge lever; missing = through), optional `branch`. Prepend, split, and connect remap `s`. Deleting a stem drops its junctions; deleting a branch spline clears `branch`.

### Consist (still chunk-spawned cars)

On save, write at least:

- `splineId`, `s` (loco origin along the path)
- `child` UUID (and `parent` on trailing cars)
- On the loco, optional `junction` and `diverge` when the consist is mid-turnout

On load: spawn cars as today. When both ends of a link exist in memory, `setChild` / `setParent` again. If the child chunk loads first, the car waits; it does not need a special global spawn.

Do **not** require spawning the whole consist when one chunk loads. Accept temporary split until the other chunks load.

## Phase 1 batches (implement in order)

### Batch T1 - Spline math and JSON

- Types: sample, segment, spline, `sampleAt(s)`, `advance(s, ds)`, nearest-`s` to a location.
- Load/save JSON. Unit tests: cardinal heading, stop at broken segment, wrap only if `loop`.
- No Bukkit entities yet.

### Batch T2 - Generate between anchors

- Staff/player item or command: two points in the same world, generate samples, persist spline.
- Debug particles or a command to dump sample count / length.
- No train motion yet.

### Batch T3 - Chunk displays (1x3)

- On chunk load, spawn 1x3 pieces for samples in that chunk; despawn on unload.
- Breaking or exploding a piece sets **segment broken** (or health) on the spline and updates/hides the display. The sample list stays unless an admin deletes the spline.
- T3 visuals are `ItemDisplay` entities using `item-small` in `trains.yml` (`ia.tfmc:track_small`), not ModelEngine.

### Batch T4 - Consist save/load

- Serialize parent/child UUIDs (and spline binding when present).
- Reattach when the other vehicle becomes an `ActiveVehicle`.
- No change to `SpawnManager` policy.
- Vehicle JSON holds the consist (`parent`, `child`, `splineId`, `s`). Spline JSON stays the path. Cars still spawn per chunk like any other vehicle.

### Batch T5 - Travel on spline

- If the loco is bound to a spline (snapped when close enough), advance `s` by signed engine speed and teleport XYZ. Bone heading from the move vector.
- Place child cars by connector spacing along the consist path (stem, then branch if the loco took the turnout). Cars stay on the stem until they reach the frog.
- Facing frog: captain **A** arms left, **D** a right, within `junction-arm-distance`. Matching `side` diverges; otherwise **through**. Backing on the branch to `s = 0` rejoins the stem.
- Broken segment: halt on path.
- Bound locos follow the spline. Unbound trains stay still (not on a track).

### Batch T6 (optional, still phase 1) - Longer straight pieces

- After bake, merge collinear samples into 2x3 / 3x3. Motion samples stay dense. Displays get sparser on straights.
- Displays merge collinear unbroken runs up to 3x3 (`item-medium` / `item-large`). Samples stay dense for motion.

## Phase 2 (do not start in T1-T5)

- P1: container `allow-items` (TLibs paths).
- P2: loco YAML `fuel-cars` (vehicle ids). Each engine slow-tick, the loco drains one matching fuel item from containers on the car directly behind if that car's id is on the list. Empty list = no auto-drain. Cargo GUIs share one inventory so every viewer sees the take.
- P3: recorder item (`tracks.item-recorder`) stores throttle vs spline `s`, travel sign, and **hold ticks** at a stop (engine off still counts). Circuits only: recording runs until one full lap of the **origin** loop (or you cancel). Time on a siding is recorded but does not finish the lap. Samples may include `splineId` and `junction`. Playback ramps throttle one step per tick (autopilot). Captain seat is manual (A/D still choose the frog); leaving resumes the tape from current `s`. Without a captain, a tape that recorded that junction diverges; legacy tapes stay through. Loaded consist. Not a second path.
- Same-spline collision: if a locomotive dummy hitbox overlaps another train piece that is not on the same consist, those **two** vehicles explode. The rest of each consist stays and uncouples. Through vs branch at the frog does **not** explode (no frog AABB). Junction display meshes are not shipped.
- P4: generic vehicle tickets (planes too). Owner toggles in the ownership GUI. Passenger seats need a matching ticket; captain, gunner, mechanic, owner, and whitelist skip. Consist uses the loco ticket id. Tickets are not consumed.
- Force-load window or unloaded `s` advance (spline data already supports it).

## Code pointers

- Train YAML: `behaviour.train` in [`BehaviourHandler`](../src/main/java/net/tfminecraft/VehicleFramework/Vehicles/Handlers/BehaviourHandler.java)
- Movement entry: [`VehicleMovementController`](../src/main/java/net/tfminecraft/VehicleFramework/Vehicles/Controller/VehicleMovementController.java) `v.isTrain()` -> `splineTick`
- Vehicle JSON: [`Database.saveVehicle`](../src/main/java/net/tfminecraft/VehicleFramework/Database/Database.java)
- Chunk spawn: [`SpawnManager`](../src/main/java/net/tfminecraft/VehicleFramework/Managers/SpawnManager.java)
- Junctions: [`TrackJunction`](../src/main/java/net/tfminecraft/VehicleFramework/Tracks/TrackJunction.java), [`TrackRegistry`](../src/main/java/net/tfminecraft/VehicleFramework/Tracks/TrackRegistry.java), [`TrackJunctionTravel`](../src/main/java/net/tfminecraft/VehicleFramework/Tracks/TrackJunctionTravel.java)
