# Changelog

## 1.1.0 (2026-10-21)

**Requires Sophisticated Core Legacy 1.2.0 or later.**

- **Smoking, blasting and stonecutter upgrades** (from the original), with auto-smoking and auto-blasting: listed and craftable when a mod adds a smoker, blast furnace or stonecutter (Future MC, VillagerBackport and others). Crafted from the original's recipes with that block, or the smoking and blasting ones from a smelting upgrade (the blasting one uses stone where the original has smooth stone).
- Smoking and blasting share the cooking upgrade limit with smelting: one per storage by default.

## 1.0.0 (2026-10-17)

First release of Sophisticated Storage Legacy, an unofficial 1.12.2 port of P3pp3rF1y's Sophisticated Storage (GPLv3, `1.18.x` branch at `891b0d9`, version 0.9.8; double chests, barrel materials, the decoration table, Storage I/O blocks and the newer upgrades from the `1.20.x` branch at `a41c806`, version 1.3.5, also GPLv3).

**Requires Sophisticated Core Legacy 1.1.0 or later** and Forge 14.23.5.2847 or later. Optional: JEI/HEI.

Contents:
- **Barrels, chests and shulker boxes** in the six 1.12 woods, dyeable on two colours, flat-top barrels. Five tiers: wood, iron, gold, diamond and Nether Star. Upgrade in place with tier upgrade items, through a controller, or by crafting.
- **Compat tiers and woods** (from the original): a **copper** tier when a mod adds copper (any `ingotCopper`), a **netherite** tier beside the Nether Star when a mod adds netherite (Future MC, Nether Backport and others), and **crimson and warped** storages when a Nether backport adds those woods. Without such a mod they stay out of creative, JEI and recipes.
- **Barrel materials** (from the original): give barrels and limited barrels the look of other blocks, part by part (core and trim of the top, sides and bottom, and the rings of a limited barrel's front). Put blocks around a barrel in a crafting grid, or use the new **decoration table** (blocks, dyes and a colour picker, with a turning preview) and the **paintbrush**, which applies a stored decoration to placed storages or a whole controller network.
- **Storage I/O, Input and Output** blocks: pipe into and out of a controller's whole network, both ways, insert only or extract only.
- **Double chests** can show their display items on either end instead of the front (item display settings), as in the original 1.20. Limited barrels count an endless slot as "∞".
- **More upgrades:** stack upgrade tier 5 and omega, stack downgrades I to III, and the infinity upgrades (admin and survival, creative only).
- **Upgrades on the front:** right-click a storage's front (or a controller) with an upgrade to install it; sneak to install as many as fit.
- **Shulker boxes:** right-click in an inventory to stash items into a carried shulker box, or carried items into one in a slot. A vanilla shulker box or chest with a redstone torch crafts into ours (keeping a shulker box's colour and items).
- **Double chests:** two matching chests side by side join into one with twice the slots (sneak to place a chest next to another without joining, or sneak on a chest's side to join that one). Upgrade both halves at once with two tier upgrades, pack the whole double chest into one item, and turn a vanilla double chest into one with the basic tier upgrade.
- **Limited barrels** (1 to 4 big slots) with front display, click-to-deposit and take, counts and fill levels.
- **Upgrades:** everything from Core (pickup, filter, magnet, feeding, compacting, void, smelting, auto-smelting, crafting, jukebox, pump, XP pump, stack upgrades), plus the **hopper** and advanced hopper, and the **compression** upgrade.
- **Controller and storage link:** one network inventory for pipes, hoppers and hand deposit; link distant storages with the storage tool.
- **Storage tool** (link, lock, display toggles), **packing tape** (move a full storage), locking and memory slots, the item display on barrel fronts.
- **Config with the original's options:** slots per storage and tier, limited barrel multipliers, upgrade limits per storage (two stack upgrades, one furnace and one jukebox by default), break guard for storages that would spill too many items, shulker box disallowed items, non-stackable items, "void anything", hiding tinted variants in JEI.
- **JEI:** every variant listed, dye, flat-top and tier upgrade examples, the controller recipe with every storage, and the crafting upgrade as a catalyst.
- Shift tooltip showing the contents of shulker boxes and packed storages.

Differences from the original:
- The top tier is the Nether Star; netherite is a second top tier only when a mod adds netherite, and copper, crimson and warped need mods too. Netherite storages are crafted from a diamond storage and an ingot (1.12 has no smithing table).
- Stack upgrade tier 5 can also be made with obsidian and a Nether Star (netherite in the original).
- The infinity upgrade's empty slots take items from anyone, pipes included (the original lets only players with the permission fill them).
- The decoration table's colour picker is written new for 1.12.
- A limited barrel's count sits in front of a block shown in its slot (the original leaves it behind, where the block's edge covers the top of the digits).
- No stonecutter, smoking or blasting upgrades (those blocks don't exist in 1.12.2).
- Packing tape keeps the contents in the item instead of in world data.
- Sorting keeps a barrel's displayed items (the display follows its items).
- The basic compacting upgrade also compacts NoTreePunching rocks into their cobblestones and sandstone (`upgrades.alwaysCompactedItems`).
- Middle click (the "Sort Storage" key) over a storage's slots sorts it, as in the original; it's a rebindable key here.

Notes for pack makers:
- A storage keeps the slot counts it was first used with, so slot-count config changes only apply to storages used for the first time afterwards (the original works the same way).
- Forge never updates existing config files when a default changes, so check `config/sstoragelegacy.cfg` if you used a pre-release build.
