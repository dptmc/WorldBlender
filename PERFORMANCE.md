# PERFORMANCE.md — making World Blender's dimension fast

The World Blender dimension is inherently heavier than a normal one: it loads **every** biome's
features into the same chunks, so it is expected to be slower than vanilla. That said, most of the
"lag" people hit is avoidable. This file lists what the mod already does and what you can tune.

---

## What the mod now does automatically (1.20.1)

| Area | Change | Why it helps |
|---|---|---|
| Surface pass | Band surfaces stop scanning once the band is placed instead of walking to the world bottom; whole-column surfaces (nether road / end borders) use section-local access and skip empty sections | Removes thousands of redundant block lookups per chunk |
| Whole-column surfaces | Fast path replaces blocks section-by-section and skips sections that contain no default block | Nether/end columns no longer cost a full 384-block walk with `chunk.setBlockState` each step |
| Anti-floating-blocks pass | Skips whole 16³ sections that contain no replaceable block, fluid or falling block | Most underground sections are pure stone and are now skipped entirely |
| Item clearing | Server-only, skips empty sections, no cascading block/light ticks | Chunks finish quickly instead of cascading updates |
| Biome source | Per-thread cached noise, no per-sample allocation | Fewer allocations during biome sampling |
| Noise settings | World Blender now uses its **own** `world_blender:world_blender` noise settings with aquifers and ore veins disabled | Aquifers are one of the most expensive parts of 1.18+ gen and also cause most of the floating-lava/water mess the containment pass had to fix |
| Structures | Blacklisted/disabled structures no longer spawn in the dimension | Fewer structures to place |

## Options you can tune

### 1. Blacklist heavy worldgen content
`config/world_blender-blending.toml` → `blacklistedFeatures` / `blacklistedStructures` /
`blacklistedBiomes`. For a big modpack this is the single biggest lever. Blacklist:
* huge/underground structure mods you don't need in the blender dimension,
* features that place tens of thousands of blocks (custom trees, boulders, geodes),
* anything that spawns fluids.

Use `resourceLocationDump = true` once to get `config/world_blender-identifier_dump.txt` with every
id.

### 2. Turn off the cleanup passes if you don't need them
`config/world_blender-dimension.toml`:
* `preventFallingBlocks`, `containFloatingLiquids`, `preventLavaTouchingWater` — these do extra work
  per chunk to stop gravel/sand/liquids from cascading. If your pack doesn't have much of that,
  turning them off saves a chunk-gen pass. (Keeping `preventLavaTouchingWater` is recommended if you
  keep lava features, otherwise you get widespread water/lava conversion lag.)

### 3. Reduce what gets imported
`config/world_blender-blending.toml`:
* `disallowFireLavaBasaltFeatures = true` (default) — keep it on.
* `allowModdedBiomes = false` — only import from vanilla biomes. Big speedup in huge packs, but the
  dimension becomes much more boring.

### 4. Surface size
`config/world_blender-dimension.toml` → `surfaceScale`. Higher = larger, calmer surface bands (fewer
transitions and fewer random structures/features near seams). Also purely visual taste.

### 5. Dimension-level / pack-level
* The dimension is a normal overworld-shaped dimension (y −64…320). Lowering the height range would
  cut gen cost, but that requires editing `data/world_blender/worldgen/noise_settings/world_blender.json`
  (and matching the dimension type `height`/`min_y`).
* The dimension still uses all structure sets. In a huge pack, structure generation (locate + piece
  placement + mob spawning around structures) is often the biggest remaining cost. Blacklisting
  structures is the fix.
* Client-side: the dimension's skylight + 384-block height are normal; general client perf mods
  (Embeddium/Sodium, ModernFix, etc.) apply as usual. The mod itself is not client-heavy.

---

## Ideas not yet implemented (future work)

* **Opt-in "vanilla features only" dimension datapack** — a variant of the biome jsons that imports
  nothing, so players can toggle performance by datapack swap.
* **Import a curated, configurable subset per biome** instead of everything, with a "maximum
  features per stage" cap.
* **Skip whole `GenerationStep.Decoration` stages** via config (e.g. drop `UNDERGROUND_DECORATION`).
* **Cache blended `HolderSet`s** across worlds (currently rebuilt per world load, but that is at
  startup, not per tick).
* Profiling pass with a real modpack to find the next hotspot (spark), then targeted fix.
