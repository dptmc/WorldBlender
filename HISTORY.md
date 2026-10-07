# HISTORY.md — development log

A chronological record of **what was actually changed in the source** and why. Player-facing
release notes live in `CHANGELOG.md`; this file is for developers (and agents) to understand the
current state of the tree at a glance.

Legend: ✅ done · 🟡 partially done / simplified · ⬜ not started

---

## Pre-1.20 history (compressed from CHANGELOG.md)

* **1.x (1.14 → 1.15, Fabric)** — original mod: dimension, portal, configs, world type.
* **2.0.0** — Fabric → **Forge 1.16.3** port.
* **2.x** — mod-compat fixes (Vampirism, Terraforged, Dimensional Dungeons), portal fixes,
  surface/carver fixes, unregistered worldgen object handling.
* **3.x** — big blending rework: `Set` based dedup, clean-slate of WB biomes, all village/ocean-ruin
  variants in one biome, datapack-configurable biome size/seed, structure spacing, lighting-thread
  crash fix, portal rendering optimisation (comp500), mob weight capping.
* **4.0.x** — config system cleanup, regex blacklisting, blanket blacklist by biome category /
  biome dictionary, structure-piece removal at world bottom, safer teleport landing, liquid
  containment improvements, separate noise settings file.

Last upstream commit retained in this repo: `a674c92 Create FUNDING.yml` (2021-12-15, 1.16.5,
mod version 4.0.2).

---

## The 1.20.1 Forge port (mod version 5.0.0) — branch `1.20.1-forge`

**Why this is a reimplementation, not a translation:** between 1.16.5 and 1.20.1 Mojang rewrote
worldgen three times. The following no longer exist and had to be replaced:

| 1.16.5 | 1.20.1 |
|---|---|
| mutable `Biome` (`BiomeGenerationSettings` could be mutated) | immutable, datapack-codec `Biome` |
| `ConfiguredFeature` / `ConfiguredStructureFeature` registries | `ConfiguredFeature` + `PlacedFeature` + `Structure` / `StructureSet` |
| per-biome `SurfaceBuilder` + `ISurfaceBuilderConfig` | global `surface_rule` in `NoiseGeneratorSettings` |
| `Layer` / `LazyArea` biome layer system | `BiomeSource` with climate/noise based placement |
| structures configured per-biome | `Structure#biomes()` `HolderSet<Biome>` + `StructureSet` placement |
| `net.minecraft.util.registry.*`, MCP names | `net.minecraft.core.Registry`, `ResourceKey`, `Holder` |
| `AbstractBlock.Properties` / `TileEntity` / `ContainerBlock` / `IInventory` | `BlockBehaviour.Properties` / `BlockEntity` / `BaseContainerBlock` / `Container` |
| `net.minecraftforge.fml.RegistryObject`, `fml.network` | `net.minecraftforge.registries.RegistryObject`, `net.minecraftforge.network` |
| Forge `BiomeDictionary`, `Biome#getBiomeCategory()` | removed by Mojang/Forge |

### Work log

#### Build system ✅
* Replaced the 1.16.5 `buildscript {}` + `apply plugin:` setup with the modern `plugins {}` block
  (`net.minecraftforge.gradle` `[6.0,6.2)`, `org.spongepowered.mixin` `0.7.+`).
* Added `settings.gradle`; `gradle.properties`: MC `1.20.1`, Forge `47.3.0`, official mappings,
  Java 17, mod version `5.0.0`.
* Wrapper bumped `6.8.3 → 8.1.1`; `mods.toml` rewritten to the `${...}`-expanded template;
  `pack.mcmeta` `pack_format` 6 → 15.
* Folded the old `gradle/*.gradle` helpers into `build.gradle` and deleted the CurseGradle/Minotaur
  publish scripts.

#### Source port ✅ (with documented gaps)
* **Registries** — dropped the `WBBiomes` placeholder registration (1.16 numeric-id hack, no longer
  needed); biomes are pure datapack now. `WBBlocks`/`WBEntities`/`WBFeatures` use the 1.20.1
  `DeferredRegister` (registries moved to `net.minecraft.core.registries.Registries`).
* **Biome source** — `WBBiomeProvider` rewritten with `PerlinSimplexNoise` (two noise fields) to
  pick one of the five biomes; registered through the Forge `RegisterEvent` into
  `Registries.BIOME_SOURCE`. `MainBiomeLayer` deleted.
* **Blending** — `TheBlender` collects placed features / carvers / mob spawns from every non-WB biome
  and stores them in `BlenderData`. Because 1.20.1 biomes are immutable, new mixins feed the data
  back: `BiomeGenerationSettingsMixin` (features + carvers) and `MobSpawnSettingsMixin` (mobs +
  spawn costs). Feature ordering (trees first, small plants last) and fire/bamboo filtering use
  resource-location keywords (`FeatureGrouping`) instead of 1.16.5's JSON-codec introspection.
* **Structures** — `StructureMixin#biomes` returns the structure's biome set with the five WB biomes
  added, so every structure accepts WB biomes; the dimension uses all structure sets. The old
  `ChunkGeneratorBehavior` "all configured variants in one biome" hack is gone (1.20.1 structures are
  no longer per-biome configured features).
* **Surfaces** — new `SurfaceBlender` + `WBSurfaceSystem`; a `NoiseBasedChunkGeneratorMixin` cancels
  vanilla `buildSurface` for the WB biome source and paints the blended bands (nether road, end
  borders, whole-column replacement, sandstone banding). `BlendedSurfaceBuilder`/`WBSurfaceBuilders`
  deleted.
* **Carvers** — `WorldCarverMixin#canReplaceBlock` allows carvers to carve the painted blocks while a
  WB chunk is generating (flagged from `applyCarvers`). The old `CarverAccessor` block-set hack was
  removed as `WorldCarver#carvableBlocks` no longer exists.
* **Portal** — block/entity/behaviour ported to `BlockEntity`, `EntityBlock`, `InteractionResult`,
  `Container`, `BuiltInRegistries`, `ClientboundBlockEntityDataPacket`; the custom sync packet
  (`MessageHandler`) was dropped because block-entity update packets already sync the cooldown.
* **Portal rendering** — `WBPortalBlockEntityRenderer` now simply draws the cube with vanilla's
  `RenderType.endPortal()`; the 1.16.5 multi-pass custom render type and screen overlay (and their
  mixins) were removed.
* **Altar** — placed by `AltarManager` (a static per-level manager ticked from the Forge
  `TickEvent.LevelTickEvent`), persisted in `WBWorldSavedData` (`SavedData`). The `WBPortalAltar`
  feature and `ServerWorld` mixin were removed.
* **Cleanup features** — `AntiFloatingBlocksAndSeparateLiquids` and `ItemClearingFeature` ported to
  `FeaturePlaceContext`/`Feature<NoneFeatureConfiguration>` and made available as datapack
  configured+placed features that the blender injects into WB biomes (LOCAL_MODIFICATIONS).
* **Client** — `WBSkyProperty` → `WBSkyEffects` (`DimensionSpecialEffects`), registered with Forge's
  `RegisterDimensionSpecialEffectsEvent` instead of an accessor mixin.
* **Datapack** — dimension, dimension_type, noise settings (now `minecraft:overworld`), biomes and
  the two placed features rewritten for 1.20.1. The altar structure NBT is unchanged.

#### Verification ✅
* `.\gradlew.bat build` succeeds with **JDK 17** and produces `world_blender-1.20.1-5.0.0.jar`
  (mixin refmap generated correctly).
* A dedicated **Forge 1.20.1-47.3.0 server boots to "Done"** with the mod installed: datapack
  dimension/biomes parse, all mixins apply.
* Force-loading the WB dimension generated region files with no exceptions and the server stayed
  responsive.

#### Performance pass ✅
* `world_blender:world_blender` noise settings with `aquifers_enabled=false` and
  `ore_veins_enabled=false`.
* `WBSurfaceSystem`: band surfaces stop scanning after the band; whole-column surfaces replace
  section-by-section and skip empty sections (was a full 384-block `setBlockState` walk per column).
* `AntiFloatingBlocksAndSeparateLiquids`: skips sections with no replaceable block/fluid/falling
  block.
* `StructureMixin`: disabled/blacklisted structures don't spawn in WB and the decision is memoised.
* Documentation in `PERFORMANCE.md`.

#### Client-hang hardening ✅
* `ItemClearingEntity` was casting to `ServerLevel` unconditionally (throws every tick on the
  client) and scanned the whole column; now strictly server-side and section-aware.
* `AltarManager` retried a full structure paste every tick forever if generation failed; now backs
  off and gives up.
* `WBSurfaceSystem` noise is per-thread (was a data race between chunk-gen workers).
* `WBSkyEffects` cloud height `NaN` → `192.0F`.

## Known gaps / TODO (differences from the 1.16.5 mod)

* **Surfaces are curated, not imported per biome.** 1.16.5 stole every biome's surface config. 1.20.1
  biomes have no surface config at all, so `SurfaceBlender` ships a fixed blend (netherrack, end
  stone, grass/podzol/sand/mycelium/snow/gravel…) that still honours the surface blacklist.
* **`#CATEGORY` / `@BiomeDictionary` blanket blacklisting removed** — `Biome#getBiomeCategory()` and
  Forge's `BiomeDictionary` were removed. Mod-id (`modid*`), resource-location and regex-term
  blacklisting still work.
* **EnderDragon spawn is not implemented** (`spawnEnderDragon` config is kept but unused). The old
  dragon-fight mixins depended on internals that changed heavily.
* **Structure trimming**: `removeWorldBottomStructures` is implemented for template structures
  (`StructureTemplateMixin`); `removeStructurePillars` is not (the target method no longer exists).
* **Rendering polish**: the portal uses the vanilla end-portal render type; the animated custom
  texture and the screen overlay were dropped, as were the block-face-culling optimisation mixins.
* **Micro-optimisations dropped**: the `WeightedStateProvider` lock mixin and the lighting-thread
  crash workaround (the target method no longer exists).
* World is now a normal overworld-shaped dimension (`minecraft:overworld` noise settings, y −64…320)
  instead of the old custom 0…256 noise settings, because the old router could not be reused
  unchanged.
