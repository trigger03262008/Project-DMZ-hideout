# Project DMZ: Hideout v0.2.0 — Facilities

**Minecraft 1.20.1 / Forge 47.4.28 / Java 17**. Independent mod `dmzhideout`, without mandatory P.D.A/Health/Inventory dependencies. The original Project DMZ design notes were reviewed; only the **Core and Core-generated zone** are part of this first slice.

## What's new in v0.2.0 — three facility blocks

- `dmzhideout:stash`: independent, saved super-sized storage with private/team sharing, an initial 270 slots and upgrades to 1350 slots (5 tiers, each +270). Upgrades cost tier×16 iron ingots, editable in future data-driven upgrade packs.
- Search by display name or item registry ID, paging, sort, shift-click deposits, owner-only sharing mode switch. Nonempty stash cannot be normally mined; it must be emptied first.
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

Expected output `build/libs/project-dmz-hideout-0.2.0.jar`. GitHub Actions builds and publishes a downloadable JAR.
