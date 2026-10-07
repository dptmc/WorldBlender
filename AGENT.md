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
| **Config blacklisting** | `theblender/ConfigBlacklisting.java` | Regex/mod-id/resource-location matching used to exclude things from being blended. |
| **Feature grouping** | `theblender/FeatureGrouping.java` | Detects "small plants", "large plants (trees)", bamboo, fire/lava/basalt features by resource-location keywords so they can be ordered/removed sensibly. |
| **Blender data** | `theblender/BlenderData.java` | Static holder that stores the blended features/carvers/spawns keyed by the WB biomes' settings instances, plus the extended biome sets for structures. |
| **Identifier dump** | `theblender/IdentifierPrinting.java` | Optional `config/world_blender-identifier_dump.txt` listing every biome/feature/structure/carver/entity/block id, to make blacklisting easy. |
| **Biome source** | `dimension/WBBiomeProvider.java` | Custom biome source placing the 5 WB biomes with two `PerlinSimplexNoise` fields (one for the land/plateau layout, one for cold hills). |
| **Surface blender** | `surfacebuilder/SurfaceBlender.java`, `surfacebuilder/WBSurfaceSystem.java` | Picks one surface material per x/z based on perlin bands and paints it, including the signature nether "road" with end-stone borders and whole-column replacement for nether/end materials. |
| **Portal** | `blocks/*` | `world_blender_portal` block + block entity (cooldown, non-removable flag). Right-click a full 2×2×2 of chests while crouching with the activation item to build it. |
| **Portal spawning logic** | `blocks/WBPortalSpawning.java` | Validates the chest cube, counts unique block items, consumes/drops the chests. |
| **Portal altar** | `dimension/AltarManager.java` | Places the unbreakable escape portal at world origin (structure NBT `portal_altar.nbt`) and remembers it in `WBWorldSavedData`. |
| **Anti floating blocks / liquid separation** | `features/AntiFloatingBlocksAndSeparateLiquids.java` | Post-gen pass: props up falling blocks with terracotta, walls in floating fluids, and separates lava/water with obsidian. |
| **Item clearing** | `features/ItemClearingFeature.java` + `entities/ItemClearingEntity.java` | Invisible ticking entity that force-ticks a chunk so broken plants/blocks self-destruct, then deletes stray item entities (kills item spam from broken worldgen). |
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
1.20.1, so parts of this project are **reimplementations rather than translations**. The trick that
makes the mod work is that **1.20.1 biomes are immutable**, so instead of mutating them the mod's own
biomes report blended content through mixins.

* **The Blender** (`theblender/TheBlender.java`) collects `PlacedFeature` holders, configured carvers
  and mob spawns from every non-WB biome and stores them in `theblender/BlenderData.java`.
* **Biomes are immutable.** `mixin/worldgen/BiomeGenerationSettingsMixin` returns the blended
  features/carvers and `MobSpawnSettingsMixin` returns the blended mobs whenever the game asks a WB
  biome's settings for them. The blobs are keyed by the WB biome's settings instance (identity map).
* **Structures accept WB biomes** via `mixin/worldgen/StructureMixin#biomes` — the structure's
  `HolderSet<Biome>` is extended with the five WB biomes. The dimension uses all structure sets.
* **SurfaceBuilder is gone.** `surfacebuilder/WBSurfaceSystem` paints the blended surface bands; a
  `mixin/worldgen/NoiseBasedChunkGeneratorMixin` cancels vanilla `buildSurface` for the WB biome
  source and also flags carving so `WorldCarverMixin` can let carvers cut through the painted blocks.
* **Biome source** uses `PerlinSimplexNoise` (two fields) instead of the removed `Layer`/`LazyArea`
  biome layer system.
* **Altar** placement is driven by `dimension/AltarManager` ticked from the Forge tick event.

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
    configs/                     the 3 ForgeConfigSpecs
    dimension/                   biome source, altar manager, sky effects, saved data
    entities/                    item-clearing entity
    features/                    anti-floating-blocks, item clearing (datapack-configured)
    mixin/                       all mixins (see world_blender.mixins.json)
    surfacebuilder/              blended surface logic
    theblender/                  the blender, blender data, blacklisting, feature grouping, id dump
src/main/resources/
    META-INF/mods.toml           mod metadata
    data/world_blender/          dimension, dimension_type, biomes, configured/placed features, structures
    assets/world_blender/        blockstate, model, lang
    world_blender.mixins.json    mixin config
```

---

## 6. Conventions / gotchas

* Keep the public API names (`WorldBlender`, `TheBlender`, `WBIdentifiers`, …) stable — other mods
  and the maven artifact reference them.
* Registries use `DeferredRegister`; nothing is registered in a static initialiser that touches
  worldgen.
* The five World Blender biomes are **defined entirely in datapack json**
  (`data/world_blender/worldgen/biome/`); there is no code-side biome registration any more.
* Blended content is applied by **mixins** into those biomes' settings (see §4), *not* by mutating
  biomes — 1.20.1 biomes are immutable and shared by reference.
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
