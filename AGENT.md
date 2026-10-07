# AGENT.md — World Blender (agent/dev brief)

This file is the single source of truth for **what this mod is, how it works, and how to work on it**.
Read this before touching code. `HISTORY.md` records what has been done; `CHANGELOG.md` is the
player-facing changelog; `README.md` is the player/modpack-facing description.

---

## 1. The idea in one paragraph

**World Blender** adds a single custom dimension that is a *mashup of every other biome in the game*.
At world load the mod walks every registered biome (vanilla **and** modded), steals its
**features** (trees, ores, plants…), **structures**, **carvers** (caves/ravines), **natural mob
spawns** and **surface materials**, and pours all of it into 5 of its own biomes at once. The
result is a chaotic, overpowered, exploration dimension: nether pathways snaking between end-stone
borders, forests growing out of deserts, every mod's ores in the same stone, every mod's mobs
spawning in the same place.

Entry is gated behind an endgame ritual (a 2×2×2 cube of chests filled with unique blocks,
activated with a Nether Star by default). A portal ties the Overworld and the dimension together
and there is always an unbreakable escape portal at world origin.

The mod is **client + server** required.

---

## 2. Core concepts

| Concept | Where | What it does |
|---|---|---|
| **The Blender** | `theblender/TheBlender.java` | Collects worldgen data from all biomes and applies it to the mod's own biomes. The heart of the mod. |
| **Config blacklisting** | `theblender/ConfigBlacklisting.java` | Regex/mod-id/resource-location/`#category`/`@biomeDictionary` matching used to exclude things from being blended. |
| **Feature grouping** | `theblender/FeatureGrouping.java` | Detects "small plants", "large plants (trees)", bamboo, fire/lava/basalt features so they can be ordered/removed sensibly. Uses JSON codec round-trips + a cache. |
| **Identifier dump** | `theblender/IdentifierPrinting.java` | Optional `config/world_blender-identifier_dump.txt` listing every biome/feature/structure/carver/entity/block id, to make blacklisting easy. |
| **Biome source** | `dimension/WBBiomeProvider.java`, `dimension/MainBiomeLayer.java` | Custom biome source placing the 5 WB biomes with two perlin noise fields (one for the land/plateau layout, one for cold hills). |
| **Surface blender** | `surfacebuilder/*` | Picks one contributing surface material per x/z based on perlin bands and paints it, including the signature nether "road" with end-stone borders and whole-column replacement for nether/end/modded surfaces. |
| **Portal** | `blocks/*` | `world_blender_portal` block + block entity (cooldown, non-removable flag, face-culling optimisation). Right-click a full 2×2×2 of chests while crouching with the activation item to build it. |
| **Portal spawning logic** | `blocks/WBPortalSpawning.java` | Validates the chest cube, counts unique block items, consumes/drops the chests. |
| **Portal altar** | `features/WBPortalAltar.java` | Places the unbreakable escape portal at world origin (structure NBT `portal_altar.nbt`). |
| **Anti floating blocks / liquid separation** | `features/AntiFloatingBlocksAndSeparateLiquids.java` | Post-gen pass: props up falling blocks with terracotta, walls in floating fluids, and separates lava/water with obsidian. |
| **Item clearing** | `features/ItemClearingFeature.java` + `entities/ItemClearingEntity.java` | Invisible ticking entity that force-ticks a chunk so broken plants/blocks self-destruct, then deletes stray item entities (kills item spam from broken worldgen). |
| **Ender dragon fight** | `dimension/EnderDragonFightModification.java` + mixins | Optionally spawns/respawns the dragon at world origin, and stops vanilla dragon code from loading a ton of chunks on entry. |
| **Altar manager** | `dimension/AltarManager.java` | Per-`ServerLevel` state that remembers whether the altar/dragon have been placed. |
| **Configs** | `configs/*` | 3 Forge config files: blending, dimension, portal. |

---

## 3. Data flow

```
datapack worldgen (biomes, features, structures, carvers, noise settings)
        │
        ▼
Minecraft builds dynamic registries (RegistryAccess)
        │
        ▼
[TheBlender]  ── walks every biome, filters via config blacklist ──┐
        │                                                          │
        └──────────► blended feature/structure/carver/spawn/surface data
        │
        ▼
WB biomes read the blended data (see §4) → the dimension generates
```

* The blending must happen **after** all mods' biome-modification hooks have run, i.e. once the
  dynamic registries are complete but **before** any level ticks.
* Blended data is rebuilt every time a world is loaded so single-player world switching works.

---

## 4. 1.20.1 implementation notes (current target)

Minecraft 1.16.5 (the original target) had mutable biomes and `SurfaceBuilder`s. Both are gone in
1.20.1, so parts of this project are **reimplementations rather than translations**:

* **Biomes are immutable.** Instead of mutating biome objects, the mod's own biomes report the
  blended generation settings / mob spawns through a mixin on `Biome`
  (`mixin/worldgen/BiomeMixin`). The blended data is built once at server start.
* **Structure "configured features" do not exist.** Structures now live in `StructureSet`s that are
  per-biome `HolderSet<StructureSet>`. The old "spawn every configured variant" hack
  (`ChunkGeneratorBehavior`) is replaced by simply blending the full `HolderSet`s.
* **`SurfaceBuilder` is gone.** The blended surface is applied by the mod's own
  `WBChunkGenerator` (extends `NoiseBasedChunkGenerator`), which overrides `buildSurface` and paints
  the blended surface bands instead of using vanilla `SurfaceRules`.
* **Carver accessors / feature ordering** are ported with modern mixin accessors where the old
  Forge/MCP access-widener paths no longer apply.

Anything that could not be reproduced 1:1 is listed under "Known gaps" in `HISTORY.md`.

---

## 5. Building

Requires **JDK 17** and network access to Maven/Forge/Gradle.

```
# Windows (PowerShell)
$env:JAVA_HOME = "<path to a JDK 17>"
.\gradlew.bat build

# Linux/macOS
JAVA_HOME=<path to a JDK 17> ./gradlew build
```

* Version/Forge/mappings live in `gradle.properties`.
* Run configs (`runClient`, `runServer`, `runData`) are defined in `build.gradle`.
* If you are behind a proxy, add `systemProp.https.proxyHost/-Port` to
  `%USERPROFILE%\.gradle\gradle.properties` (user-level, never commit this).

### Where things go

```
src/main/java/com/telepathicgrunt/worldblender/
    WorldBlender.java            mod entrypoint, registration + config
    WorldBlenderClient.java      client-only event subscriptions
    WBIdentifiers.java           all resource locations / registry keys
    blocks/                      portal block, block entity, renderer, spawning logic
    biomes/                      WB biome placeholders (real data is in datapack json)
    configs/                     the 3 ForgeConfigSpecs
    dimension/                   biome source, altar, dragon fight, chunk generator behaviour
    entities/                    item-clearing entity
    features/                    portal altar, anti-floating-blocks, item clearing
    mixin/                       all mixins (see world_blender.mixins.json)
    surfacebuilder/              blended surface logic
    theblender/                  the blender, blacklisting, feature grouping, id dump
    utils/                       network handler, codec cache, noise, seed holder
src/main/resources/
    META-INF/mods.toml           mod metadata
    data/world_blender/          dimension, dimension_type, noise_settings, biomes, structures
    assets/world_blender/        blockstate, model, lang
    world_blender.mixins.json    mixin config
```

---

## 6. Conventions / gotchas

* Keep the public API names (`WorldBlender`, `TheBlender`, `WBIdentifiers`, …) stable — other mods
  and the maven artifact reference them.
* Registries use `DeferredRegister`; nothing is registered in a static initialiser that touches
  worldgen.
* The mod's own biomes are **dummies registered by code** whose ids are then **overwritten by
  datapack json** — do not remove the `WBBiomes` registration or the numeric ids shift.
* Configs are `COMMON` type and require a full game restart for blending changes to apply.
* Never blacklist the mod's own `portal_altar` feature: it is the only guaranteed escape.
* `anti_floating_blocks_and_separate_liquids` / `item_clearing` are intentionally added **last** so
  they can contain other features' liquids and clean up after everything else.

## 7. Testing checklist

1. `runServer`, create a world, `/execute in world_blender:world_blender run tp ~ 70 ~` — dimension
   loads, altar exists at origin, escaping works.
2. Build a portal in the Overworld with 216 unique blocks + a Nether Star (or lower
   `uniqueBlocksNeeded` in the portal config for a quick test).
3. `/reload` a datapack that overrides a WB biome — must not crash.
4. Config: blacklist a mod's namespace, restart, confirm nothing from it is imported.
5. `resourceLocationDump=true` — confirm `config/world_blender-identifier_dump.txt` appears.
