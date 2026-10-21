# Sophisticated Storage Legacy: 1.12.2 backport plan

An unofficial port of P3pp3rF1y's Sophisticated Storage (upgradeable barrels, chests and shulker boxes) to Minecraft 1.12.2 / Forge 14.23.5. It's the sister mod of Sophisticated Backpacks Legacy (`../SophisticatedBackpacksLegacy/PLAN.md`), and the two share one library jar.

## 1. Source and license

| | Storage | Core (needed for reference only) |
|---|---|---|
| Repo | `P3pp3rF1y/SophisticatedStorage`, branch `1.18.x` | `P3pp3rF1y/SophisticatedCore`, branch `1.18.x` |
| Pinned commit | `891b0d9e` (2024-10-13, mod 0.9.8, MC 1.18.2) | `42394d4f` (2024-09-22, 0.6.4) |
| License at the pin | GPLv3 (LICENSE file + `mods.toml`) | GPLv3 |
| Local checkout | `Workspaces/SS118Reference` | `Workspaces/SC118Reference` |

Rules:
- Same obligations as Backpacks Legacy: GPLv3, public source, credit "Original mod by P3pp3rF1y", a header on changed files, distinct name and modid, described as unofficial.
- **Never read or copy anything committed after 2025-02-10**, on any branch. That includes 1.19.x, even though its LICENSE file was never switched.
- Storage's LICENSE file only appeared on 2024-03-03. Before that, the repo had no license file, so we don't use older Storage commits either.
- Why 1.18.x and not 1.19.x: the last GPL 1.19.x commit (`810001f2`, 2024-04-27) has the same content and an older version. 1.18.x is the newest GPL code and the closest to 1.12.

There was never a 1.16 Storage, so unlike Backpacks, this code is written against **Sophisticated Core 1.18**, not against the 1.16 in-mod framework that Backpacks Legacy ported. See §3.

## 2. Identity and toolchain

| Setting | Value |
|---|---|
| Workspace | `E:\MincraftModding\Workspaces\SophisticatedStorageLegacy` |
| modid | `sstoragelegacy` (proposed) |
| Package | `com.exiledradio.sstoragelegacy` |
| Forge (build) | `14.23.5.2847`, never raised (FG2.3 ceiling) |
| Forge (test instance) | 14.23.5.2859 is fine at runtime |
| Mappings / Gradle | `stable_39`, same ForgeGradle2 fork and build guards as Backpacks Legacy |
| Dependencies | **Sophisticated Core Legacy** (required, §3; its `-dev` jar is put into `deps/` by Core's `copyToDependents`). Soft: JEI/HEI, Quark (optional) |
| Test instance | `C:\Users\Radio\curseforge\minecraft\Instances\SophisticatedStorage` (empty, shared with Backpacks Legacy) |

## 3. The shared core library (decided 2026-10-16)

The real mods are three jars: Core, Backpacks, Storage. We follow that:

- **Sophisticated Core Legacy** (`scorelegacy`, exists since 2026-10-16): storage GUI framework (screen, tabs, controls), upgrade base classes and response interfaces, the shared upgrades (pickup, filter, magnet, void, feeding, compacting, stack, crafting, smelting, jukebox, tank, battery, pump, XP pump), filters, inventory handler with int-sized counts and >127 container sync, its own network channel, liquid experience. Settings (memory, no-sort, item display), sorting and the controller still have to be ported into it from Core 1.18.
- **Backpacks Legacy** keeps: backpack items/block/tile, worn layer, Baubles, backpack-only upgrades (deposit, restock, refill, everlasting, inception, tool swapper, as in the original), backpack recipes.
- **Storage Legacy** (this mod) adds what's below in §4.

How we get there: the code that would become Core Legacy already exists in the Backpacks Legacy workspace (`api`, `common`, `upgrades`, `util`, `network`, and the GUI parts of `client`). It was ported from 1.16 Backpacks, which is the code Core 1.18 was split from, so the shapes match. **The extraction is a refactor of the Backpacks workspace and is not done from this session while that workspace is being worked on elsewhere.** Best time is before Backpacks Phase 4: every Phase 4 upgrade (crafting, smelting, jukebox, tank, battery, pumps) is a Core upgrade that Storage also needs, so doing them after the split means writing them once.

Where Storage relies on Core 1.18 features that the 1.16 code doesn't have, we add them to Core Legacy, using `SC118Reference` as the source:
- `controller/` (storage controller + linking, the network of storages)
- `IStorageWrapper` as an interface (1.16 has `BackpackWrapper` as a concrete class). Generalising this is the main job of the extraction.
- Compression inventory part hooks, hopper upgrade's IO mode, and `INeighborChangeListenerUpgrade`

Until the extraction is done, Storage work that doesn't touch Core can still go ahead: blocks, tiles, models and art (Phases S1 and S2 below).

## 4. Scope

The pinned Storage source: 159 Java files, 210 textures.

### Kept

| Group | Content | 1.12 notes |
|---|---|---|
| Barrels | Wood barrel in every wood type, plus Iron/Gold/Diamond/5th tier. Front display of up to 4 items, open/closed lid, flat-top toggle | 1.12 has no vanilla barrel. Oak/spruce/birch/jungle/acacia/dark oak only (no crimson/warped). Item display needs a TESR |
| Limited barrels | 1/2/3/4-slot "drawer-style" barrels with a big count display, click to insert/extract | Fits 1.12 packs well (Storage Drawers players). TESR count rendering |
| Chests | Wood + tiers, double chests, item renderer | Own model and TESR, like vanilla chests |
| Shulker boxes | Base + tiers, keep contents when broken, dyeable | 1.12 has vanilla shulker boxes, so placement/peek works the same way |
| Barrel materials | Craft a barrel from any log/planks combo, tinted main/accent colours | OreDictionary `logWood`/`plankWood`; tints via `IBlockColor` |
| Tier upgrades | `basic_to_iron_tier_upgrade` etc., applied in-world by right-clicking | Copper skipped, so 3 + 1 tiers instead of 5 |
| Storage-only upgrades | Hopper (+Adv), Compression | Hopper uses `IItemHandler` on the neighbour tile |
| Shared upgrades | All Core ones, from Core Legacy | Stonecutter, smoking, blasting dropped, as in Backpacks |
| Controller + Storage Link | Multiblock network: one controller sees every connected storage | Port from Core 1.18 `controller/` |
| Tools | Packing tape (pick up a full chest), Storage tool (lock, toggle display, toggle count) | Lock rendering via TESR |
| Misc | Contents tooltip on shulker items, sorting, JEI recipes | |

### Dropped or replaced

| Original | Replacement |
|---|---|
| Copper tier | Dropped (tiers: base, iron, gold, diamond, 5th) |
| Netherite tier | Same 5th-tier material as Backpacks Legacy, so the two mods match |
| Crimson/warped wood | Dropped |
| Smithing-table upgrade recipe | Plain crafting recipe (no smithing table in 1.12) |
| Chipped, Crafting Tweaks compat | Dropped |
| Data generators | Hand-written JSON, converted once from the reference |

## 5. Rendering (the biggest new part)

1.18 Storage uses dynamic baked models a lot (`BarrelDynamicModel`, `CompositeElementsModel`). The 1.12 plan:

| Thing | 1.12 approach |
|---|---|
| Barrel body (wood type × tier × tint × open/flat-top) | `ICustomModelLoader` + `IModel`, baked once per variant and cached, same method as the backpack item model in Backpacks Legacy Phase 2 |
| Wood type, tints, tier | Extended block state (`IExtendedBlockState` + unlisted properties from the tile), so placed barrels don't need a TESR for the body |
| Front display items, limited-barrel counts, lock icon | TESR, only for tiles that show something (`DynamicRenderTracker` keeps the rest off the TESR list) |
| Chests | TESR with a `ModelBase` lid, like vanilla `TileEntityChestRenderer`; item form via `TileEntityItemStackRenderer` |
| Shulker boxes | Same as chests, with the shulker model |

## 6. Phases

Every phase runs in `runClient` **and** `runServer`, then in the CurseForge test instance.

| Phase | Content | Needs Core Legacy? |
|---|---|---|
| S0 | Workspace from the Backpacks template, LICENSE, README, asset-import script (textures, models, lang → `.lang` + `pack.mcmeta`) | No |
| S1 | Wood barrel block + tile + inventory (no upgrades), every wood type, dye and flat-top recipes, open/close lid. Breaking spills the contents like the original (keeping them is packing tape, S4) | No (plain `ItemStackHandler` first, swapped for Core's handler in S3) |
| S2 | Barrel dynamic model, tiers, front item display TESR. **Done 2026-10-16** (tier corners via multipart instead of a dynamic model) | No (in the end the interim GUI uses Core's background) |
| **C** | **Extract Sophisticated Core Legacy from the Backpacks workspace** (§3), generalise the wrapper to `IStorageWrapper`. **Done 2026-10-16**: `../SophisticatedCoreLegacy`, see its PLAN.md | Is the extraction |
| S3 | Barrels on Core: storage GUI, upgrade slots, all shared upgrades, tier upgrade items. **Done 2026-10-16** | Yes |
| S4 | Chests (incl. double), shulker boxes, packing tape. **Done 2026-10-16** (no double chests: not in the pinned original) | Yes |
| S5 | Limited barrels, storage tool, lock, count display. **Done 2026-10-16** | Yes |
| S6 | Hopper + compression upgrades, controller + storage link. **Done 2026-10-16** (some parts not seen in game, see status) | Yes (controller goes into Core) |
| S7 | JEI, config, polish, test-instance soak, release (GPL, CurseForge). **Code done 2026-10-17**; soak in the user's instances and the release itself still to do | |

### Status

**S0: done 2026-10-16.** `tools/import_assets.py` imported 158 textures, 39 item models and 10 lang files (copper, netherite, crimson, warped, stonecutter, smoking, blasting and Chipped content left out).

**S1: done 2026-10-16, verified in game on a dedicated server.** How it works:
- `tools/gen_barrel_models.py` turns the original's custom-loader barrel model into a vanilla **multipart** blockstate. The block has FACING + OPEN in metadata and FLAT_TOP, WOOD, TINT (none/main/accent/both) as actual-state properties read from the tile. Parts are stacked in the original's order: wood base unless both colours are dyed, then the accent overlay, then the main overlay. Tint index 0 = main, 1 = accent (the original uses 1000/1001). Render layer is CUTOUT_MIPPED for the overlays.
- Items can't use multipart, so the script also writes 38 merged item models (`models/item/barrel/<wood>[_main|_accent]|tinted[_flat]`), picked by a custom mesh definition.
- Stack NBT uses the original's tag names (`woodType`, `mainColor`, `accentColor`, `flatTop`). Recipes: one shaped recipe per wood (planks, matching slab, redstone torch, as in the original), plus `storage_dye` and `flat_top_barrel_toggle` as custom `IRecipe`s.
- No 1.12 barrel sounds exist, so opening/closing uses the chest sounds at the original's volume and pitch.

Checked so far:
- [x] Builds; boots on a dedicated server (no sided-class crash) and in runClient with no model or texture errors
- [x] Tile NBT round trip via `/setblock … {woodType:"oak",mainColor:…,flatTop:1b}` and `/blockdata`
- [x] `setblock … destroy` drops one barrel carrying wood, colour and flat top
- [x] Reobfuscated jar copied to the CurseForge test instance
- [x] Rendering: all six woods, main-only, accent-only and both-dyed, flat top (wood and dyed), open lid, all six facings; item icons
- [x] GUI titled "Oak Barrel" / "Birch Barrel"; items move in and are saved; lid opens with the GUI and closes on exit
- [x] Hopper inserts from above and extracts from below; comparator reads 1 with 84 items and drops to 0 when emptied
- [x] Recipes: oak barrel from planks/slabs/torch; red dye left + lapis right gives red body and blue trim; barrel alone toggles "(Flat Top)"
- [x] Hand placement faces the player; a survival break gives exactly one barrel with wood, both colours and flat top
- [x] Breaking a filled barrel with its GUI open closes the GUI and spills exactly its contents (5 diamonds, 7 gold) plus one barrel
- [x] JEI (2026-10-16, `compat/jei/StorageJeiPlugin`, as the original's StoragePlugin): subtypes on wood/colours/flat top, so JEI lists all 24 creative entries; display-only dye recipes (16 single colours + yellow/lime) and flat-top toggle recipes. `BarrelBlock.getSubBlocks` delegates to the item, or JEI adds a stray wood-less barrel.
- [ ] Not checked: open/close sounds (no audio in the harness), booting the CurseForge instance

**S2: done 2026-10-16, verified in the dev client on a dedicated server.** What it added:
- **Tiers** as separate blocks, like the original: `barrel` (27 slots), `iron_barrel` (54), `gold_barrel` (81), `diamond_barrel` (108), `nether_star_barrel` (132). Upgrade-slot counts (1-5) are in `BarrelTier` for S3. Copper is dropped; the 5th tier is the Nether Star, as in Backpacks Legacy.
- **Models:** no dynamic model needed. `tools/gen_barrel_models.py` adds the original's "tier" part (metal corners on every face, normal and flat) as the last multipart layer of a per-tier blockstate, plus merged item models `barrel/<tier>_<look>` (190 item models in all). The wood tier's corner textures are fully transparent in the original, so the wood barrel leaves that part out. `tools/make_nether_star_barrel.py` recolours the diamond corners with Backpacks' Nether Star palette.
- **Recipes:** `storage_tier_upgrade` (shaped, keeps the barrel's NBT: wood, colours, flat top, name): 8 iron / 8 gold / 8 diamonds around the previous tier, and obsidian + nether star around the diamond barrel (the original's netherite step needs a smithing table).
- **GUI:** Storage now depends on Core Legacy. The interim barrel screen uses Core's storage background: 9 columns up to 81 slots, 12 above (a stopgap until S3 moves barrels onto Core's storage screen).
- **Tile:** inventory sized per tier. While loading, the saved inventory size wins (the tile is created before its block is known), and `onLoad` only ever grows it to the tier's size.
- **Front display items:** synced tile data (`displayItems`: item, rotation, slot; up to 4) drawn by a TESR ported from the original's DisplayItemRenderer: 1 centred, 2 stacked, 3 as one over two, 4 in a square; 2D items smaller, blocks standing out. The settings screen that picks them comes with S3; until then they are only set via NBT.
- **JEI:** all tiers have subtypes, dye examples and per-variant tier-upgrade examples (the original's TierUpgradeRecipesMaker), so every listed barrel's recipe resolves.

Checked:
- [x] Every tier renders with its corners: normal, flat top, main/accent/both tints; no model or texture errors
- [x] Display items: 1, 2, 3 and 4 items, rotation 45, a block item on a flat top
- [x] 132-slot GUI in the 12-column layout (it is taller than the screen at large GUI scales, like Backpacks' biggest tier)
- [x] Crafting a red oak barrel with 8 iron gives "Oak Iron Barrel", still red; JEI finds the recipe for wood variants
- [x] 5 diamonds in slot 131 of a nether star barrel survive a server restart
- [ ] Not checked: the CurseForge instance boot, hopper/comparator on higher tiers (same code path as wood)

**S3: done 2026-10-16 (barrels on Core).** What it added:
- `BarrelWrapper` (the barrel's `IStorageWrapper`): a Core `StorageContents` kept in the tile's NBT under `contents` (inventory, upgrades, settings, sort order, open tab, a contents UUID for the jukebox). Barrels saved before S3 kept their plain `inventory` at the top level; it is read straight into the new contents (same item format), so nothing is lost.
- The barrel GUI is Core's storage screen (upgrade slots per tier: 1/2/3/4/5, upgrade tabs, sort buttons, settings) and the settings screen (main, no-sort, memory, item display). The tile ticks its upgrades and exposes items (behind filter upgrades), fluids and energy like a placed backpack. Breaking a barrel spills contents and upgrades (packing tape comes in S4).
- Storage's own upgrade items, as in the original: pickup, filter, magnet, feeding, compacting, void (basic and advanced), smelting, auto-smelting, crafting, stack tiers 1 / 1 Plus / 2 / 3 / 4, jukebox, pump (basic and advanced), XP pump, upgrade base. Recipes converted from the original (`tools/convert_recipes.py`); Stack Upgrade Tier 1 Plus uses `ingotCopper`/`blockCopper` (any copper mod). The original at this pin has no pump / XP pump recipes; ours reuse Backpacks Legacy's so they're craftable.
- Item display: Core's category now holds up to N slots with a rotation each (barrels 4, backpacks still 1); an empty selected slot shows its memorised item, as in the original. Old single-slot NBT is migrated.
- Settings wording per mod: `IStorageWrapper` gives the title/tooltip/back-tab/context keys, so barrels say "Settings" / "Back To Storage" like the original Storage, and backpacks keep their wording.
- Tier upgrade items (`<from>_to_<to>_tier_upgrade`, 10 of them, nether star art from `tools/make_nether_star_tier_upgrades.py`): right-click a placed barrel to raise its tier in place, keeping everything; not while it is open. Recipes from the original; the netherite step is obsidian + nether star.

Checked in the dev client: gold barrel GUI with 81 slots and 3 upgrade slots; stack and crafting upgrades go in and the crafting tab works; settings screen wording; 4 display items picked in the settings (one rotated) show on the front. Also checked: Basic to Iron tier upgrade on a filled oak barrel (becomes an iron barrel in place, contents, settings and display kept, one item used in survival); Backpacks' one-item display still works after the Core change. Not checked one by one: every upgrade (same Core code Backpacks uses).

## 7. Decisions still open

1. Names/modids: `Sophisticated Storage Legacy` / `sstoragelegacy` and `Sophisticated Core Legacy` / `scorelegacy`?
2. 5th tier material: whatever Backpacks Legacy picks (its §11), so both match.
3. When to do the Core extraction (Phase C): ideally right before Backpacks Phase 4, by the session working on Backpacks or once it's idle.


**S4 started 2026-10-16: packing tape done.** Right-click a barrel with packing tape (8 uses): it shows the tape overlay (the original's "packed" part, a multipart layer), hides its display items and can't be opened. Breaking it gives a barrel item marked "Packed" that carries the contents, upgrades and settings; placing it puts them back and unpacks it. Checked in game with a filled iron barrel. **Deviation:** the original keeps a packed item's contents in world data keyed by UUID; ours keeps them in the item's NBT (simpler; revisit if huge barrels hit packet limits). 

**S4: done 2026-10-16 (chests, shulker boxes, basic tier upgrade).**
- **Refactor:** the barrel code was split into shared `StorageBlockBase` / `StorageTileEntity` / `StorageBlockItem` / `StorageBlockWrapper` / `StorageBlockContainer` (and `BarrelTier` became `StorageTier`); barrels, chests and shulker boxes sit on top. Same tile NBT as S3, so existing barrels load unchanged.
- **No double chests at first:** the pinned 1.18 original has none. They were added later (see "Double chests" below), ported from the 1.20.x branch.
- **Chests** (`chest`, `iron_chest`... `nether_star_chest`): single chests in every wood, dyeable, tiered (same slot counts as barrels), one display item on the front (the lock hides when one is shown), packing tape. Drawn by `ChestRenderer`, a TESR with the original's model in its Y-up coordinates so its 1.18 textures map as-is; layers: wood (unless both colours dyed), tinted main, tinted accent, tier trim, lock, tape. Lid animates like a vanilla chest (block event + client ticking).
- **Shulker boxes** (`shulker_box`... `nether_star_shulker_box`): dyeable, tiered, always keep their contents when broken (creative too), need room to open, pistons break them, refuse shulker boxes (ours and vanilla). Drawn with vanilla's `ModelShulker` (same parts and texture layout as the original) in vanilla 1.12's shulker transform, with the original's layered textures.
- **Items:** chest and shulker items are `builtin/entity` models drawn by `StorageItemStackRenderer` (TEISR) with vanilla's chest display transforms. Blockstates are particle-only (`chest_block.json`, `shulker_box_block.json`).
- **Recipes:** chests from 8 planks + redstone torch per wood, tier chests and shulker boxes (`storage_tier_upgrade`), shulker box from chest (shells around a chest of any tier, keeping its NBT), the plain shulker box recipe, nether star tiers with obsidian + nether star, basic tier upgrade (sticks + redstone torch).
- **Basic tier upgrade** (and every basic-to-X one): turns a vanilla chest into an oak chest and a vanilla shulker box into ours in its colour (purple stays undyed), keeping items and name. (A vanilla double chest was refused at first; see "Double chests".)
- **Nether star art:** `tools/make_nether_star_barrel.py` also writes `entity/{chest,shulker_box}/nether_star_tier.png`.

Checked in the dev client on a dedicated server: every chest and shulker tier renders (woods, tints, trims, display item); chest GUI (54 slots, 2 upgrade slots) and lid animation; shulker GUI and lid animation; a shulker box broken in survival drops one item with its contents, which come back when placed; packing a chest, breaking it and placing it again keeps its items; JEI lists every variant with the right icons; vanilla chest -> oak chest (name and items kept), vanilla red/lime shulker -> tinted iron/basic shulker (items kept). Found and fixed: `BlockShulkerBox` colour getters are client-only in 1.12 (crashed the conversion on the server and lost a box's items in testing); the conversion now works everything out before touching the vanilla block. Not done: display item turning with an opening shulker lid (shown only while closed), contents tooltip on packed/shulker items, vanilla shulker boxes accepting our shulker box items (1.12 has no hook to stop that).

**S5: done 2026-10-16 (limited barrels, storage tool, locking).**
- **Limited barrels** (`limited_barrel_1`..`_4`, `limited_<tier>_barrel_<n>`, 20 blocks): 1 to 4 big slots with the original's base stack multipliers (I 32/64/96/128/160, II 16/32/48/64/80, III 10/20/30/40/50, IV 8/16/24/32/40 for wood..nether star) and the tier's upgrade slots. Front faces any way: horizontal, or up/down with a horizontal facing so the front reads upright (meta: 2 bits each). Models from `tools/gen_limited_barrel_models.py` (merges the original's limited_barrel_1 parent chain and wood overrides, resolves `simple_composite` parts; 112 parts, 760 item models).
- **Front interaction** like the original: right-click a slot deposits the held stack, a quick second click (10 ticks) pulls every matching item from the inventory, a dye recolours that slot's count (sneak + dye: all, via the right-click event since 1.12 skips the block when sneaking with an item); left-click takes one, sneak + left-click a stack (survival through `onBlockClicked`, creative through the left-click event; the client sets `blockHitDelay` so holding the button does not take an item every tick). The GUI opens from the sides or sneak + right-click.
- **Rendering:** `LimitedBarrelRenderer` (TESR) draws each slot's item where the slot is, its count (`CountAbbreviator`, bold, slot colour, 1/48 or 1/96 scale), fill bars (`fill_indicators.png`) and upgrades. Counts and fill levels are synced only when a count changes (checked on the tile tick; not saved). `BarrelDisplayItemRenderer` became the shared front-face helper (display items, upgrade icons, lock quad).
- **GUI:** `LimitedBarrelContainer` / `LimitedBarrelGui` with the original's framed 1-4 slot layout, fill bars with percentages and no sort buttons; the settings screen uses the same layout without the item display and no-sort tabs (all slots are always shown and never sorted, selected on placement). Needed Core layout hooks (see Core PLAN.md).
- **Locking** (all storages): a locked storage only takes items it already holds (hoppers too, via `LockedInputHandler`); a locked limited barrel memorises every slot instead, and its empty unmemorised slots take nothing. Barrels show the original's lock part (multipart), chests the lock quad.
- **Storage tool** (`storage_tool`, recipe ` EI / SR / S  `): sneak + scroll cycles Link / Lock / Count / Lock display / Tier display / Upgrades display / Fill level display (new Storage network channel, `ScrolledToolMessage`); mode shown above the hotbar while held; right-click a storage toggles; in the off hand while placing, Lock mode locks it. Link mode only remembers a position until controllers (S6). Hidden tiers: barrels drop the tier part, chests keep the latch and show a faded trim while holding the tool in tier mode or a tier upgrade. Shulker boxes ignore tier hiding, as in the original.
- **Upgrades on the front:** server checks every 10 ticks and syncs the upgrade list when it changes; shown when toggled on or while holding the tool or an upgrade (empty slots as upgrade bases).
- **Recipes:** 24 limited barrel recipes (the original's 4 patterns per wood), 16 tier recipes, the storage tool.

Checked in the dev client on a dedicated server: all four sizes place facing the player and render; deposits land in the slot aimed at (1, 2, 3 and 4 slots); counts show and a dye recolours one slot's count; left-click takes one, sneak + left-click a stack; double-click deposit pulled 168 iron from the inventory; GUI frame, slot layout and fill percentages (64 diamonds = 10% of a 3-slot wood barrel); settings screen layout and tabs. Found and fixed: Core never applied the base multiplier when the settings created the inventory before the upgrade handler (every limited barrel held only 64). Also checked (second pass): tool overlay and sneak + scroll through the modes; lock icon on barrels and chests; a locked empty limited barrel refuses dirt while a locked slot still takes its own item; count, lock, tier (barrel and chest, latch kept), fill level and upgrade toggles, all surviving a restart; fill bars while holding the tool; an upward-facing limited barrel (item on top, count upright from the placing side). Found and fixed: upgrade icons were drawn on the recessed lid, hidden behind the frame; they now sit on the outer surface like the original. **Not checked:** hoppers into locked storages, vertical barrels facing down. **Small gaps:** a hidden barrel lock or tier is not drawn faded while holding the tool (chests do fade the tier); empty upgrade slots are drawn opaque, not translucent. Known look: a block item can hide the count under it when viewed from above (same geometry as the original). Not ported: limited barrels default the void upgrade to "void overflow" (Core has no upgrade-defaults hook yet).

**S6: done 2026-10-16 (hopper and compression upgrades, controller, storage link).**
- **Hopper upgrade** (basic and advanced, the original's defaults: 2/2 filter slots, 8 ticks, 1 item; advanced 4/4, 2 ticks, 4 items): pulls from the storage's top and pushes to its bottom (relative to its front, so limited barrels facing up/down work); the advanced one's tab adds the original's six side buttons (push / pull / both / off). Neighbours are reached through their item handler capability (1.12 vanilla inventories have one), looked up per transfer instead of cached. Input and output filters are two contents filters on one upgrade, so Core's filter messages now carry the filter's tag key (as in the original).
- **Controller** (`controller`) and **storage link** (`storage_link`), with the original's recipes: placed next to storages the controller takes in every storage touching it or touching those, within 15 blocks; storages placed later join, broken or packed ones leave, and anything no longer reachable drops out. Pipes and hoppers see the whole network as one inventory; inserts go to storages already holding that stack, then ones with the item and room, then ones memorising it, then any with room (the original's order). Right-click deposits the held item if the network has it; a quick second click deposits every such item from the inventory. Storage tool in link mode: click the controller, then a storage or storage link to link it from a distance (a link brings in the storages around the block it sits on); the controller renderer outlines linked blocks while the tool is held in link mode. The tool's other modes and tier upgrades used on a controller apply to every connected storage, like the original.
- **Deviation:** the original keeps the controller's routing index up to date with listeners on every storage; here it is rebuilt lazily per tick (and after the controller itself changes a storage), with failed inserts remembered per tick. Same routing, no listeners threaded through Core.
- **Compression upgrade** (`compression_upgrade`, up to 5 slots by config): the first free slots hold an item and its compressed forms (block / ingot / nugget), each slot showing the whole amount in its own item; adding or taking any form converts. Reversible 2x2/3x3 recipes, plus the config's extra decompressions (glowstone, quartz by default). Refused when the slots hold items that don't chain ("can't be compressed from the previous slot") or there are no free slots.
- **Deviation:** the original keeps a second "calculated" stack per slot plus a partition layout in the inventory NBT. Here a chain is one total counted in its smallest item, re-stored largest-form-first after every change (what the original's compaction ends with), and worked out from the stored stacks each time; each compression upgrade takes the first slots no earlier one has. Saved data are ordinary stacks, so removing the upgrade leaves real items. Needs Core's new inventory parts (see Core PLAN.md); parts only run on the server thread.

Checked in the dev client on a dedicated server, mostly by commands and NBT checks: basic hopper moved a chest's 64 cobblestone + 16 dirt through a barrel into the chest below; a controller routed hopper-fed diamonds into the barrel holding diamonds and gold into the first free storage, and a hand deposit of iron into the chest holding iron; linking a lone barrel with the tool routed redstone into it; compression turned 20 ingots + 9 hopper-fed nuggets into 2 blocks + 3 ingots, and a hopper below took out exactly 2 blocks + 3 ingots. Found and fixed: the controller and storage link blockstates (1.18's "" variant and "block/" model paths); Core's merge-onto-slot click would have lost items in a compression slot. The first fix (always write back the stack handed out) duplicated items, because 1.12's Slot.onTake calls onSlotChanged after the take: taking 20 ingots left the barrel with its 20. Slots now write back only when the handed-out stack was changed in place; checked, no dupe.

Second pass (screenshots allowed): compression GUI shows 2 blocks / 20 ingots / 185 nuggets, and after taking the ingots shows the blocks gone and 5 nuggets (part slots are sent to the client directly, since a click's own slot updates are dropped); putting them back restores it. Storage link: the tool in link mode clicked on the controller and then on a storage link gave the link and both barrels around it the controller's position. Tier upgrade (basic to iron) used on the controller upgraded all four network barrels (linked ones included); they kept their controller link and a hopper into the controller still routed into them. Controller outlines: lines and boxes drawn, but black (the lightmap unit was still on); fixed by turning the lightmap off while drawing, **colours not seen since**. Found and fixed: the client built a storage's container from its own copy of the tile, which never had the upgrades unless that client watched them go in, so after a relog or chunk reload a storage with a tab that adds slots (hopper filters) opened with fewer slots than the server's (IndexOutOfBounds on the window items packet, upgrade tab dead). The update tag and packet now carry the upgrade inventory, as Backpacks does with its upgrade sync message. **Not checked in game:** the advanced hopper's tab and side buttons after that fix (the user stopped screenshots), the outline colours, compression with memory.

Dev runs: `gradlew runClient -PautoConnect` joins the dev server on 25566 from launch, so tests can run from server commands alone.

**Fixes from play-testing (2026-10-17, user report from the Celeritas instance):**
- The settings screen's main tab said "Backpack" with the backpack icon; it now says "Storage" with the storage's own item, as the original's StorageMainSettingsTab does (Core: `IStorageWrapper.getMainSettingsTabKey/getMainSettingsTabIcon`).
- **Deviation:** the item display now follows its items through a sort. The original keeps the slot, so sorting changed what a barrel shows; each shown slot whose item moved now points at a slot holding it, keeping its rotation and place (checked on the dev server: display slot 0 to 1, and two shown items swapping).
- Middle click on a storage's slots sorts it (Core; Inventory Tweaks stays off for these containers, its click-sorting can't handle big stacks). Not in the original.
- Compacting: config `upgrades.alwaysCompactedItems` (default `notreepunching:rock/*`) lists items compacted although nothing crafts them back, so the basic upgrade turns NoTreePunching rocks into cobblestone / its stone cobbles / sandstone. Checked on the dev server with NTP 2.0.21 + AlcatrazCore in `eclipse/mods`.
- Dev-only `/sstoragedev sort|insert x y z ...` (registered only in a deobfuscated environment) to sort or insert through the item capability from the server console, because a server with no players stops ticking hoppers.

**S7: code done 2026-10-17 (config parity, polish, JEI). Not released yet.**
- **Config, now the original's:** every storage's inventory and upgrade slots (`storages.*`, barrels, chests and shulker boxes per tier; limited barrels' base slot multiplier and upgrade slots), `tooManyItemEntityDrops` (200), `upgrades.maxUpgradesPerStorage` (2 stack / 1 furnace / 1 jukebox per barrel, chest and shulker box; 1 furnace / 1 jukebox per limited barrel), `upgrades.voiding.voidAnythingEnabled`, `shulkerBoxDisallowedItems` (list + container items), `client.showHigherTierTintedVariants`, and the original's default `nonStackableItems` (the 16 vanilla shulker boxes). Deployed configs that already had the old empty list were updated by hand (Forge never migrates defaults).
- **Slot counts are saved per storage** (`numberOfInventorySlots` / `numberOfUpgradeSlots` in its contents, like the original's StorageWrapper) the first time it is used, so config changes apply to new storages; tier upgrades (in world, through a controller, or crafting a shulker box / packed storage) add the difference between the tiers. The counts go to the client with the tile's sync data. Existing storages save the current defaults on first use, which equal the old hard-coded counts.
- **Upgrade limits** live in Core (`UpgradeLimits`, `UpgradeGroup`, `IStorageWrapper.getStorageType`); backpacks have no storage type, so no limits, as in Backpacks 1.16.
- **Break guard:** breaking a barrel/chest/limited barrel that would drop more than the configured number of item entities is refused with the original's packing tape hint, unless sneaking.
- **Contents tooltip:** Shift over a shulker box or a packed storage shows its upgrades and contents (Core's `ContentsTooltipPanel`, now shared with the backpack tooltip; read from the item's own NBT).
- **Sort key:** "Sort Storage" key binding (middle mouse by default, as the original's) over a storage's slots; registered by Core, so backpacks have it too.
- **JEI:** the controller recipe with every storage variant (the original's ControllerRecipesMaker) and the crafting upgrade as a crafting catalyst.
- Not ported: the original's debug tool and its internal inaccessible-slot item.

Checked on the dev server: saved counts (27/1, then 54/2 after an iron tier upgrade with contents kept; with the wood barrel config at 36, a new barrel got 36 and its iron upgrade 54, while an older one kept 54); limits (a third stack upgrade, a second furnace upgrade and a second jukebox refused; a second stack upgrade on a limited barrel allowed); the break guard (a gold barrel of 5184 cobblestone refused, the same broken while sneaking, a lightly filled barrel broken); shulker boxes refusing a configured ender chest and vanilla shulker boxes; two shulker boxes taking two slots in a stack-upgraded barrel while cobblestone stacks to 200. Client-only parts (tooltip, sort key, JEI, tinted-variant toggle, void-anything tab) not seen in game.
- **Deviation (2026-10-20, user request):** display items are pressed flat onto storage fronts (block items keep 1% of their depth and sit on the face), like the original's later versions. The 1.18 original stands block items out like an item frame, which the user found took too much room. Chest and shulker box tier layers also get a per-layer polygon offset: OptiFine shaders made the trim z-fight.

## Double chests (added 2026-10-20)

Ported from the original's `1.20.x` branch at `a41c8065` (2025-02-09, version 1.3.5, GPLv3; double chests came in at `5ded1f33`, 2024-02-28). Reference checkout: `Workspaces/SS120Reference`.

- **State:** `ChestBlock.TYPE` (single, left, right; vanilla 1.14's ChestType), meta = horizontal facing | type << 2, so old single chests (meta 0 to 3) load as single. The other half is clockwise of the front for a left half (`getConnectedDirection`).
- **Joining** (`getStateForPlacement` / `onBlockPlacedBy`): a chest placed next to a single chest of the same block, wood (none counts as acacia), colours and facing, unpacked, with at most one of the two holding upgrades, joins it. Sneaking on a chest's side joins that chest and takes its facing; sneaking anywhere else places a single chest.
- **Data:** only the main half keeps contents and upgrades. It is the half with upgrades, otherwise the right half. Its inventory grows by the other half's slot count and the other half's items and per-slot settings (memory, no-sort, item display) move to the slots after its own (`ChestTileEntity.joinWith`). The other half saves `doubleMainPos`; its capabilities, comparator, GUI and the break guard go to the main; the controller keeps it in the network for adjacency but skips its contents (`IControllableStorage.hasStorageData`, Core).
- **Splitting** (`ChestBlock.breakBlock`): breaking the other half takes back the main's last slots (its own share) and spills them; breaking the main hands its last slots to the survivor, each cut to a normal stack, and spills the rest with the main's upgrades. A half whose partner vanished without breaking (or a single chest that still has a main or a double's slots on load) becomes single and spills the extra slots. Deviation: shares are counted from the halves' saved slot counts, not the current config, so nothing is lost after a slot-count config change.
- **Tier upgrades:** in place on a double chest, two items upgrade both halves (main grows by both halves' new slots); through a controller the same. A vanilla double chest becomes one of ours with two basic tier upgrades. A packed double chest crafts to the next tier with the recipe of a single chest (deviation: the original has separate double-chest recipes with the same ingredients).
- **Packing:** taping either half packs both; breaking it drops one item flagged `doubleChest` with all the contents, and the other half goes with it. Placing that item needs the spot to the player's right free and puts down both halves.
- **Look:** `left_` / `right_` textures for every wood, tint, tape and tier (Nether Star made from diamond by `tools/import_double_chest_textures.py`); 15-pixel-wide half models with half latches (the original's createDoubleBody*Layer). The display item and lock icon are centred on the whole front and the upgrades run from its left edge, drawn by the main half only. The item is both halves at 0.8 scale.
- **Hopper upgrade:** on a double chest it reaches the blocks along both halves on a side (and beyond the other half towards it).
- **Checked on a dedicated dev server** (`/sstoragedev place|use|info|drops`): joining both ways and the main choice (upgraded left half stays main), sneak rules, wood and colour mismatches, inserting through the other half, both split cases with items in the upper half, packing and the double item (blocked spot refused), in-place iron upgrade (108/54 slots), vanilla double conversion, break guard per half, controller membership, settings moving on join. **Not checked (client):** the left/right models and textures, display/lock/upgrade positions on the front, lid animation of both halves, the double item render.

## Compat materials and 1.20 features (added 2026-10-20)

From the original's 1.20.x branch at `a41c8065` (Storage) and `91ffbe1` (Core, `Workspaces/SC120Reference`), both the last GPL commits; Backpacks' copper art from SB 1.20.x at `bafaeb5` (`Workspaces/SB120Reference`).

- **Compat materials** (Core `compat/MaterialCompat`): copper (`ingotCopper`), netherite (`ingotNetherite`, plus registry names of netherite mods without ore names) and crimson/warped (Nether Backport's and Farmer's Future Delight's planks, given `plankCrimson`/`slabCrimson` ore names). Checked in Core's postInit; each mod then loads `compat_recipes/<material>/` (`copper_and_netherite/` needs both). The blocks and items always exist; unavailable tiers and woods are left out of creative, JEI and recipes. Dev runs can fake materials with `eclipse/config/sophisticated-dev-materials.txt` (`oreName=item`), since Future MC and Deeper Depths can't load deobfuscated.
- **Tiers:** `StorageTier` is now wood, copper (45/1), iron, gold, diamond, netherite (132/5, beside the Nether Star), nether star. Netherite storages: shapeless diamond storage + netherite ingot. Assets: `tools/import_compat_assets.py`; models regenerated (`gen_barrel_models.py`, `gen_limited_barrel_models.py`, the latter's tier/prefix pairing fixed). `tools/gen_compat_recipes.py` writes the recipes.
- **Woods:** `WoodType.CRIMSON`/`WARPED`, textures from the 1.18 pin (single chest, barrels) and 1.20 (double chest halves).
- **Upgrades on the front:** `StorageBlockBase.tryAddSingleUpgrade` / `tryFillUpgrades` (sneak, via the right-click event); the controller applies to every connected storage before depositing.
- **Shulker boxes:** stash by right-click in inventories (client `GuiScreenEvent.MouseInputEvent` + `StashMessage`; contents edited through a tile outside the world, `StorageItemContents`); shapeless vanilla shulker box / chest + redstone torch recipes.
- **Stack upgrades:** tier 5 (x32), omega, downgrades (1/8, 1/16, 1/32); infinity upgrades (Core `InfinityUpgradeItem`, an inventory part over every slot). Spilling and double-chest splitting use stored stacks (`getRawStack`), never the endless view.
- **Storage I/O / Input / Output:** `StorageIOBlock` + `StorageIOTileEntity`, a storage without contents (`hasStorageData` false) that hands pipes the controller.
- **Barrel materials:** `BarrelMaterial`, `BarrelMaterials` (NBT `materials`, names with `@meta` for 1.12 metadata), the crafting-grid recipe, `DecorationHelper` costs, `PaintbrushItem` (+ HUD overlay of missing items), the decoration table (`DecorationTableBlock`/`TileEntity`/`Container`/`Gui`, `DecorationTableMessage`, a new `ColorPicker`). Rendering: `BarrelMaterialModel` wraps the barrel blockstate models and, when the extended state carries materials, bakes the original's whole/core/trim part models (`tools/gen_barrel_material_models.py`, `barrel_materials.json`) with each material block's sprite for the face that side stands for, plus our tint/tier/tape/lock parts; items get a per-stack model.
- **Checked on the dev server:** compat detection (stand-ins + real Nether Backport), all compat recipes, copper/netherite tier upgrades in place, crimson double chests, front upgrades, stash both ways, vanilla conversions, downgrades/omega/infinity (admin break guard, endless slots never spill), I/O blocks, material recipe and drops, Backpacks compat recipes. Client: the dev client loads with no model or texture errors. **Not checked (client):** how material barrels, the decoration table screen, colour picker, paintbrush overlay and ∞ slots look and behave in game.

