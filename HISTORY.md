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

**Why it is a reimplementation and not a straight translation:** between 1.16.5 and 1.20.1 Mojang
rewrote worldgen *three times*. Concretely, the following no longer exist and had to be replaced:

| 1.16.5 | 1.20.1 |
|---|---|
| mutable `Biome` objects | immutable `Biome`, registered as datapack codecs |
| `ConfiguredFeature` / `ConfiguredStructureFeature` registries | `ConfiguredFeature` + separate `PlacedFeature` + `Structure` / `StructureSet` |
| `SurfaceBuilder` + `ISurfaceBuilderConfig` | surface rules inside `NoiseGeneratorSettings` |
| `worldgen.biome` layer classes | mostly unchanged, but the API surface moved |
| `net.minecraft.util.registry.*` (ObfuscationReflectionHelper-era MCP names) | `net.minecraft.core.Registry`, `ResourceKey`, `Holder` |
| `AbstractBlock.Properties` | `BlockBehaviour.Properties` |
| `TileEntity` | `BlockEntity` |
| `ContainerBlock` | `BaseContainerBlock` |
| `IInventory` / `ItemStack.getDisplayName` | `Container` / `ItemStack.getHoverName` |
| Forge `RegistryObject` via `net.minecraftforge.fml.RegistryObject` | `net.minecraftforge.registries.RegistryObject` |
| `SimpleChannel` in `net.minecraftforge.fml.network` | `net.minecraftforge.network` |

### Work log

#### Build system ✅
* Replaced the 1.16.5 `buildscript {}` + `apply plugin:` setup with the modern `plugins {}` block
  (`net.minecraftforge.gradle` `[6.0,6.2)`, `org.spongepowered.mixin` `0.7.+`).
* Added `settings.gradle` (pluginManagement + foojay toolchain resolver).
* `gradle.properties`: MC `1.20.1`, Forge `47.3.0`, official mappings `1.20.1`, Java 17.
* Wrapper bumped `6.8.3 → 8.1.1`.
* Folded `gradle/processresources.gradle`, `manifest.gradle`, `maven.gradle` into `build.gradle`;
  **deleted** `gradle/curseforge.gradle` and `gradle/modrinth.gradle` (they pinned MC 1.16.5 and
  the CurseGradle/Minotaur versions are long dead). Publishing target/credentials kept in
  `build.gradle`.
* `mods.toml` rewritten to the `${...}`-expanded 1.20.1 template; `pack.mcmeta` `pack_format` 6 → 15.
* This branch builds against a real JDK 17 workspace (see §Verification).

#### Source port
*See the running notes below — this section is appended to as work lands.*


## Verification

* `.\gradlew.bat build` must succeed with JDK 17.
  (Exact result of the latest run is recorded at the bottom of this file.)


## Known gaps / TODO

*To be filled in as the port progresses.*
