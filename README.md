# Project DMZ: Hideout v0.4.0 — Stash rig equipment controls

**v0.4.0:** the reserved **RIG** equipment box is now functional when Inventory v0.8.0 is installed; every other reserved equipment box remains locked. From the Stash UI, move a rig item into the player's real 9×3 main inventory, then **left-click RIG** to equip it (mouse wheel over RIG cycles among available rigs), **right-click RIG** to unequip, or **Shift+right-click RIG** to cancel a pending change. A clickable 9-position size strip above the real hotbar permits picking two slot types to swap, without moving the held items. Inventory validates and owns all server-side changes; Hideout has no direct dependency and never duplicates equipment.

This first integration does **not** claim native drag-and-drop from a Stash storage cell directly onto RIG; transfer to player inventory first. The Stash's server-owned slots, search, sharing, item locks and compatibility are unchanged. Test with a world backup and matching client/server Inventory/P.D.A builds.

# Project DMZ: Hideout v0.3.3 — Dedicated Creative Tab

**Minecraft 1.20.1 / Forge 47.4.28 / Java 17**. Independent mod `dmzhideout`, without mandatory P.D.A/Health/Inventory dependencies. The original Project DMZ design notes were reviewed; only the **Core and Core-generated zone** are part of this first slice.

## v0.3.3: Dedicated Hideout Creative Tab

- Adds `Project DMZ: Hideout` as its own Creative Mode tab, separate from
  the Loot and Health inventories.
- The Hideout tab contains all five facility blocks: Hideout Core, Stash,
  Med Station, Trading Center, and Salvage Workbench.
- Existing item and block registry IDs are unchanged. The block items are
  no longer injected into vanilla Functional Blocks.
- Loot has moved to its own repository:
  https://github.com/trigger03262008/Project-DMZ-Loot\n- The temporary \`loot/\` directory has been removed from this repository.\n  Loot now builds exclusively from its own repository and CI.

## v0.3.2: Salvage Workbench

- New `dmzhideout:salvage_workbench` placeable block with a crafting recipe.
- Right-click while holding Loot salvageable junk to disassemble without tool
  durability cost; right-click while holding a damaged Loot tool to repair.
- Uses optional reflective `dev.dmzloot.api.LootApi.useWorkbench` so Hideout
  does not require Loot to launch.
- Outputs and repair materials are processed by the Loot mod on the server.
- No The Lost Cities / structure loot tables are included yet.

## v0.3.1: Med Station treatment and CASH display fix

- Med Station and Trading Center now show the player's current CASH balance
  through P.D.A's existing synchronized read-only client cache. No extra
  wallet ledger or P.D.A protocol changes.
- Med Station now checks displayed CASH against the server's treatment quote,
  disabling APPLY when funds are insufficient.
- The treatment bridge tries Health v1.1.4+ `HealthIntegrationApi` first,
  and supports earlier Health 1.1.x versions that lack the new quote/heal
  methods using their own public read/write APIs.
- Health API reflection errors now emit a diagnostic in the server log
  (once per error type), instead of silently returning an unexplained -1.
- The HEAL tab reports insufficient funds or missing Health/P.D.A more
  clearly. CASH is still debited/credited only by server-side P.D.A CashApi.
- No changes to Hideout world data, Stash NBT, Traders, Inventory or P.D.A.
  Replace Hideout v0.3.0 with **v0.3.1** on both client and server.

## v0.3.0: Tarkov-inspired Facility UI

**Version compatibility:** Minecraft 1.20.1, Forge 47.4.28, Java 17.
Replace Hideout v0.2.1 with v0.3.0 on both sides, plus Traders v0.3.0.
Health v1.1.4 and P.D.A v0.7.0 are unchanged.

### Med Station
- Opens to CRAFT; top tabs switch to HEAL and back.
- Searchable, category-filtered vertical crafting list, ingredient availability
  (current Stash plus player), instant crafting, output amount and START buttons.
- Inputs are taken from nearby authorized Stash first, then player inventory.
  Outputs prefer Stash by default, and automatically fall back to inventory.
  A button reverses the destination preference. Failed transactions restore
  both item sources. Locked Stash items cannot be consumed.
- HEAL displays Health client body-part HP, server quote and one APPLY action
  for existing paid full-body treatment. No new Health APIs were required.
- JSON recipe supports optional `category`; `craft_time: 0` is reserved for
  future time-based crafting, but the current service always crafts instantly.

### Stash
- Left: locked, dim placeholders for future Inventory/Combat equipment.
  These are not fake functional equipment slots.
- Middle: real player model, vanilla armor + offhand + 27 inventory cells
  + hotbar. Moves items using the vanilla server inventory.
- Right: search, filter, sort, vertical mouse-wheel/dragbar row scrolling
  instead of page buttons. Existing 270–1350 saved item slots are retained;
  only 54 display cells synchronize at a time.
- Owner/team sharing, upgrades and Shift+right-click item locks are retained.

### Trading Center
- Upper row of trader cards; FIVE cards per page with previous/next buttons.
  New traders are read dynamically from JSON. Switching trader clears the
  pending sale basket; no item is consumed by this operation.
- BUY and SELL tabs. BUY still supports CASH, barter and Stash payment/output.
- SELL has a 24-item-line virtual basket and right-hand scrollable Stash:
  left click adds one, Shift+click adds a stack, drag from Stash into the basket,
  right click basket to remove one or Shift+click to clear a line.
- The buyback JSON `buy_from_player` list is the allowlist. Unbuyable and
  locked Stash items are dimmed; the server rechecks everything on ADD and SELL.
- The basket is a non-destructive preview until SELL is pressed. Total CASH
  comes from Traders v0.3.0 and its server-side price multipliers. Failed
  batch settlements roll back Stash items, rather than dropping them.
- SELL requires an adjacent authorized Stash; BUY can still use player items.

The three interfaces use original dark/industrial UI styling, not copied
copyrighted images or textures. Actual screen appearance is subject to
Minecraft GUI-scale preferences. Test with a world backup.

## v0.2.1 hotfix: Trading Center item selection

- Fixed a GUI input ordering bug that prevented left-clicking a sell/buy offer
  from selecting it. Custom stock rows now receive clicks before
  `AbstractContainerScreen.mouseClicked` handles general screen interaction.
- The selected offer is highlighted and its exact price/barter requirements
  are shown in the center. Both sale and buyback offers can be selected.
- Added page-aware hit-testing regression coverage; clicks on the 2px gaps,
  the stash preview and UI navigation buttons do not select an offer.
- No world/save data migration and no changes to Health, Traders or PDA.
  Replace only Hideout v0.2.0 with v0.2.1 on client and server.

## What's new in v0.2.0 — three facility blocks

- `dmzhideout:stash`: independent, saved super-sized storage with private/team sharing, an initial 270 slots and upgrades to 1350 slots (5 tiers, each +270). Upgrades cost tier×16 iron ingots, editable in future data-driven upgrade packs.
- Search by display name or registry ID, paging, three sort choices and shift-click deposits. Five category views (ALL/BLOCK/FOOD/GEAR/MED). **Shift + right-click** a stash item to lock/unlock it; locked items cannot be removed or spent by Trading Center or Med Station. Locks persist across saves.
- Owner-only private/team sharing switch; teammates get access after explicitly enabling sharing. A nonempty stash cannot be normally mined; it must be emptied first. Owner-only empty-stash mining still works.
- Starter upgrade costs: tier 1→2 = 16 iron, 2→3 = 32, 3→4 = 48, 4→5 = 64. Final economic balancing and data-driven upgrade materials are deferred.
- `dmzhideout:med_station`: requires Health v1.0.1 and P.D.A v0.7.0 for full paid injury recovery; independent JSON-based medical crafting. Includes one starter bandage recipe using Minecraft materials, and has `required_mods` filtering for future Loot recipes.
- `dmzhideout:trading_center`: three-column interface (trader/offer, settlement, adjacent stash) that delegates pricing and transactional authority to Traders v0.2.1.
- Adjacent accessible stashes are discovered in the six face-adjacent positions. StashApi is publicly available for future Inventory integration; Inventory v0.0.0 still has no 2D grid system, so the stash uses normal slot storage until that API exists.
- Vanilla artwork placeholders for the three blocks; no survival recipes for the block machines yet.

The v0.1.1 safe zone and boundary particles are unchanged. Make a world backup when upgrading.

## Previous v0.1.1: particle outlines

- The ACTIVE Core's safe-zone boundary is shown using **soft dust particles**: a dotted circular ring at the viewing player's height, with four vertical marker columns indicating the cylinder's height.
- **Teal** = a safe zone owned by your current scoreboard team (or your solo UUID). **Amber** = another owner's protected zone.
- The ring is intentionally a visualization, not a force field or a changed collision shape. Actual protection remains the same circular horizontal radius (default **12**) with vertical **±12**.
- The ring is only sent to players within the configured display margin of the active zone. The server emits the particles; no additional client mod or protocol is required beyond Hideout itself.
- Relocating a Core stops the old outline; removing the active Core stops its outline. World-save behavior and the Hideout Core block remain unchanged.
- Server config `world/serverconfig/dmzhideout-server.toml`, section `[visualization]`:
  - `showBoundaryParticles = true` — disable all boundary particles if desired.
  - `particleIntervalTicks = 20` — how often particles are emitted (5–100).
  - `particleRingPoints = 40` — number of dots along the circle (12–128).
  - `particleViewMargin = 16` — how far outside the boundary a player can be and still see it (0–64).
- Sending is capped by vanilla's 32-block non-forced particle visibility per point, and never scans terrain or force-loads chunks.
- For performance, increase `particleIntervalTicks` or reduce `particleRingPoints` if many people have bases close together.

**Visual smoke test:** walk across the boundary to check the teal/orange ring, climb to view top/bottom markers, change the radius in TOML, move the Core and check the old ring disappears. The particle color follows the player's present team membership.

## What this prototype implements

- `dmzhideout:hideout_core`: a placeable, mineable, non-piston-movable Hideout Core, registered in the Functional Blocks creative tab. Uses **vanilla iron/observer textures as placeholders**; custom artwork can come later.
- Place a Core as a player: an area around it becomes a safe zone. Default horizontal **radius 12**, vertical **±12**, configurable server-side (3–64). Horizontal geometry is circular, not a full square or an infinite column.
- Uses **vanilla scoreboard teams** as an initial team provider (`/team add`, `/team join`). No team means a personal safe zone keyed by the player's UUID. All members of a team share the zone and are allowed to build/break/use containers in it.
- Other teams cannot build/break/place in the area or interact with its chests, machines, doors and most directly-interacted entities. Unattributed automated block placement is conservatively blocked. Piston moves touching protected blocks are rejected; explosion terrain damage is filtered; fluid-triggered block generation is suppressed where Forge supports the hook.
- Players inside the zone take **no damage** from conventional damage sources (including PvE/PvP/environment) and are excluded from explosion knockback lists. To prevent shooting from invulnerability, damage **originating from a player within a zone** is also blocked.
- **One active Core per team/player key**. Placing another Core for the same team relocates protection immediately: the previous Core remains physically present but is inert. Breaking the current active Core unregisters its zone. Front-line/secondary bases have no protection.
- Saved server-side to overworld `data/dmzhideout_safezones.dat`; cross-dimension locations and owner keys are persisted. Works across player logout and respawn. `/hideout status` checks the current area and your registered Core.
- Server-only `HideoutApi.isSafeZone(...)`, `HideoutApi.canModify(...)`, `HideoutApi.homeOf(...)` to let future Health / P.D.A implementations consult zones.

## First test

1. Install the Hideout JAR on **client and server**. It should be available in creative inventory.
2. `/give @s dmzhideout:hideout_core` then **place it normally as a player** (not `/setblock`). No survival recipe is provided yet.
3. `/hideout status` shows the Core location, area and radius. Verify damage immunity while standing inside.
4. Test building, mining and chest interaction as an authorized teammate and an unauthorized player. You can form test teams with `/team add alpha` and `/team join alpha PlayerName`.
5. Place a second Core for your team, then inspect the original location: it must no longer be protected. The old block itself is inert, not deleted.
6. Save/reopen the world. Verify the active protection persists. Check TNT, piston and outside-team access.
7. Edit `world/serverconfig/dmzhideout-server.toml` and restart/reload server configuration to test radius.

## Prior design constraints preserved

- **One protected headquarters per team**; a relocated base's old protected area must disappear.
- **Team members may build, break and use facilities**, outsiders may not. **Player damage is disabled inside**.
- Front-line outposts may be built freely, but must not become protected automatically.
- **The planned high-cost special bed, bed carry/transport rules, spawn protection and bed-linked respawn relocation are not yet implemented.** The earlier 32-block spawn area was a *provisional* radius for the spawn system, **not a final Core radius**.

### Temporary implementation choices / limits

- **Radius 12** was chosen for this prototype in response to "around the Core, several blocks"; not a settled final game-balance value.
- The vanilla scoreboard team is a temporary team provider. Switching team names/membership does not automatically migrate historical claims; keep team identities stable.
- Vanilla/Forge protection is best-effort. Some non-Forge world-editing, modded machines, abnormal damage sources, fake players, and fluid engines can bypass conventional events. A broader protection provider and integration tests will be needed before treating this as complete anti-grief software.
- Piston machines crossing claims are intentionally blocked regardless of owning team, because automation has no validated team identity.
- Vanilla damage immunity does not refill HP, blood, stamina or status effects. Planned Health recovery is a separate future integration.
- Core has no fuel, durability, crafting recipe, UI, player spawn linkage or P.D.A BASE integration in this first slice.

## Build

Java **17**, Gradle **8.8**, ForgeGradle 6:

```powershell
gradle clean build --no-daemon --max-workers=1
```

Expected output `build/libs/project-dmz-hideout-0.3.0.jar`. GitHub Actions builds and publishes a downloadable JAR.
