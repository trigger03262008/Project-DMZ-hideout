# Project DMZ: Loot v0.1.0

Minecraft 1.20.1 / Forge 47.4.28 / Java 17. Standalone mod id `dmzloot`.

**This subdirectory is an independent Gradle/Forge mod** temporarily hosted inside
the Hideout repository while a dedicated Project-DMZ-Loot repository is created.
The result is a separate JAR, not bundled into Hideout.

- 24 baseline items, plus 8 extra rough/professional tool variants.
- Four tool families with durability; damaged tools do not vanish at 0.
- Five-item-category (six including valuables) item stats stored in
  `src/main/resources/data/dmzloot/catalog/items.json`, including future
  Inventory mass/grid fields. Item IDs and max stack/durability are fixed
  at game launch by the catalog; changing these requires a mod rebuild and
  restart. JSON datapacks cannot add a new Forge item registry ID at runtime.
- Server-authoritative salvage of **held** junk: SNEAK + RIGHT CLICK in open
  space, with required tools in the player's inventory. Uses durability.
- Salvage at Hideout's `dmzhideout:salvage_workbench`: hold salvageable junk
  and right-click the block; no tools or durability required.
- Repair: hold a damaged tool and right-click the Hideout workbench; consumes
  defined inventory supplies. Tools remain even when durability reaches zero.
- JSON salvage recipes in `data/dmzloot/salvage/*.json`; no world/chest loot
  tables yet. Structure-generation integration will be designed separately,
  including The Lost Cities.
- Tools are found at full durability in Creative and `/give` for this phase.
  Randomly damaged loot tools will be introduced only with future loot tables.

At this point, salvage only uses **player inventory** (not Stash) and
requires enough space for all outputs; failure restores the original inventory.
Do not apply this to a live world without backup until gameplay-tested.

The item textures are simple generated 16x16 placeholders, distinguishable
by category and item variant. To override, place PNGs at:
`src/main/resources/assets/dmzloot/textures/item/<id>.png` and rebuild.

## Build

```powershell
cd loot
gradle clean build --no-daemon --max-workers=1
```

Output: `build/libs/project-dmz-loot-0.1.0.jar`.

Optional cross-mod API: `dev.dmzloot.api.LootApi`.
