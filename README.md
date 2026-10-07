# World Blender

![The banner logo for World Blender with a picture of the crazy landscape behind the title.](https://i.imgur.com/HLLklJ6.png)

**A dimension filled with every single biome's features, structures, carvers, surfaces and natural
spawns — vanilla *and* modded — shoved into one glorious mess.**

**Currently for Minecraft 1.20.1 (Forge).** Needs to be on **both client and server** to work.

> Looking for the old 1.16.5 build? See the `latest-released` branch / the 4.0.2 release.
> Looking for the Fabric version? <https://modrinth.com/mod/worldblender-fabric>

---

## What is World Blender?

World Blender is a dimension where every biome's features (trees, plants, ores…), structures,
natural mob spawns, carvers (caves) and terrain surfaces have been blended together. Even modded
features and structures get added.

Inside the dimension you'll find massive webs of different surfaces, all stuffed with trees, grass
and structures. The **nether surface pathway** is the main walkway — nothing else can interrupt it —
with thin **end-stone borders** on each side. Follow that red road and explore the insanity!

The more worldgen mods you have, the crazier it gets. Too many mods and chunk generation gets slow;
use the config to blacklist the heaviest worldgen content. Large modpacks *will* need config tuning.

Some mods hardcode their features/structures to their own biomes or dimensions. Where a mod exposes
a config option controlling that, set it so the content is not restricted — that's usually the
easiest fix.

Want to develop against it or just read the code? See **[AGENT.md](AGENT.md)** for the architecture
and **[HISTORY.md](HISTORY.md)** for what has been ported/changed.

---

## How do I enter this world?

You must prove you're in the endgame: place **8 chests in a 2×2×2 cube** and fill *every* slot with
a **unique block item** (stacks/duplicates don't count, items with no block form don't count).
Then **sneak + right-click any of the chests while holding a Nether Star**. The chests and their
contents are consumed as a sacrifice and the portal is created.

The portal has a short cooldown (it looks dark red) before it will teleport you. Walk in after it
cools down. Crouch-right-click a portal block with an empty hand to vaporise it (the portal at
world origin is permanent).

Enter the dimension quickly with:
`/execute in world_blender:world_blender run tp ~ 70 ~`

There is always a quartz altar with an unbreakable portal at world origin so you can never get
stuck. Leaving always returns you to the Overworld. The portal can teleport any entity or dropped
item, one at a time, with a cooldown.

By default, features containing lava/fire are **not** imported so the dimension doesn't burn down.
If you enable them, set `/gamerule doFireTick false`.

---

## Configuration

The mod has three config files (in the `config` folder, outside world saves):

| File | Purpose |
|---|---|
| `world_blender-blending.toml` | what gets imported (vanilla/modded features, structures, carvers, spawns, surfaces) + blacklists + identifier dump |
| `world_blender-dimension.toml` | surface scale, dragon spawn, carver reach, liquid/falling-block containment, structure trimming |
| `world_blender-portal.toml` | unique blocks needed, activation item(s), required blocks, whether the chests are consumed |

Highlights:

* Turn importing of vanilla/modded **features, structures, carvers, surfaces and natural spawns**
  on or off.
* **Blacklist** by key term (regex), by resource location, by mod id (`modid*`), by biome category
  (`#MUSHROOM`) or by Forge biome dictionary (`@OCEAN`).
* **Dump** every registered id to `config/world_blender-identifier_dump.txt` to make blacklisting
  easy.
* Change how many unique blocks the portal needs, what activates it, required blocks, and whether
  the chests drop instead of being consumed.
* Cap dragon spawning, surface band size, and whether carvers can carve netherrack/end stone/modded
  blocks.

**Config changes require a full Minecraft restart** because of how the blending is done at load.

**Performance:** if the dimension is too heavy in your pack, see **[PERFORMANCE.md](PERFORMANCE.md)**
for what to blacklist and which options to tune.

The dimension and its biomes can also be overridden by **datapacks** — see
`src/main/resources/data/world_blender`.

---

## For developers — adding it to your workspace

<blockquote>repositories {

&nbsp; maven {

&nbsp; &nbsp; url "https://nexus.resourcefulbees.com/repository/maven-public/"

&nbsp; }

}</blockquote>

&nbsp;

Replace the version with the latest published one, e.g. `1.20.1-5.0.0`.

<blockquote>dependencies {

&nbsp; implementation fg.deobf("com.telepathicgrunt:WorldBlender:1.20.1-5.0.0")

}</blockquote>

&nbsp;

**Add these two properties to both of your run configs so World Blender's mixins work, then refresh
Gradle and regenerate your IDE runs.**

<blockquote>minecraft {

&nbsp; runs {

&nbsp; &nbsp; client {

&nbsp; &nbsp; &nbsp; property 'mixin.env.remapRefMap', 'true'

&nbsp; &nbsp; &nbsp; property 'mixin.env.refMapRemappingFile', "${projectDir}/build/createSrgToMcp/output.srg"

&nbsp; &nbsp; }

&nbsp; &nbsp; server {

&nbsp; &nbsp; &nbsp; property 'mixin.env.remapRefMap', 'true'

&nbsp; &nbsp; &nbsp; property 'mixin.env.refMapRemappingFile', "${projectDir}/build/createSrgToMcp/output.srg"

&nbsp; &nbsp; }

&nbsp; }

}</blockquote>

### Building from source

Requires **JDK 17**.

```
.\gradlew.bat build          # Windows
./gradlew build              # Linux/macOS
```

Version, Forge and mappings are set in `gradle.properties`. See [AGENT.md](AGENT.md) for the full
project layout.

---

## License

LGPLv3 — see [LICENSE](LICENSE). You may use this mod in modpacks and modify the source.

For the list of changes, see [CHANGELOG.md](CHANGELOG.md).

If you find an issue or have a suggestion, open an issue on GitHub.

**Discord:** <https://discord.gg/SM7WBT6FGu>
