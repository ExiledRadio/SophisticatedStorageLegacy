# Sophisticated Storage Legacy

An **unofficial** port of [Sophisticated Storage](https://github.com/P3pp3rF1y/SophisticatedStorage) to Minecraft 1.12.2 (Forge 14.23.5).

## Credits and license

Sophisticated Storage and Sophisticated Core are by **P3pp3rF1y**. This port is based on the original's `1.18.x` branch at commit
[`891b0d9`](https://github.com/P3pp3rF1y/SophisticatedStorage/tree/891b0d9e29350ec78c7b8567285036d2ecdb303c)
(version 0.9.8, October 2024), and Sophisticated Core's `1.18.x` branch at commit
[`42394d4`](https://github.com/P3pp3rF1y/SophisticatedCore/tree/42394d4f8f006d7bc6cb24ce180db2c3a75ae62e),
both released under the GNU General Public License v3. Double chests are ported from the original's `1.20.x` branch at commit
[`a41c806`](https://github.com/P3pp3rF1y/SophisticatedStorage/tree/a41c8065ebda0fbc09efc94ed5a4d39f9efc62f4)
(version 1.3.5, February 2025, also GPLv3), with their textures.

This port is a modified version of that work and is also licensed under the **GNU GPL v3**; see [LICENSE](LICENSE).
Textures, models and translations come from the original unless noted otherwise. Files that were ported carry
a header noting the modification.

This project isn't affiliated with or supported by P3pp3rF1y. Please don't report issues with this port to
the original mod.

## Differences from the original

- The top tier is a Nether Star tier, matching Sophisticated Backpacks Legacy. The original's copper and netherite tiers and crimson and warped woods exist too, but are listed and craftable only when another mod adds those materials (netherite then sits beside the Nether Star).
- The Smoking, Blasting and Stonecutter upgrades (and the auto versions) are listed and craftable only when another mod adds a smoker, blast furnace or stonecutter (Future MC, VillagerBackport and others). Smoking cooks whatever smelts into food and blasting ores and whatever smelts into an ingot or gem, plus Future MC's own lists; the stonecutter shows VillagerBackport's and Future MC's stonecutter recipes.
- Double chests come from the original's 1.20 version (the 1.18 one has none). When chests join, the per-slot settings of the half that keeps nothing move with its items, and a broken half takes back exactly its own share of slots.
- Packing tape keeps a storage's contents in the item instead of in world data.
- The controller keeps its routing index up to date once per tick instead of through listeners on every storage, and compression keeps one total per chain of forms; both behave the same in play.
- A storage's item display follows its items when you sort (the original keeps the slot, so sorting changed what a barrel shows).
- Compacting upgrades also compact the items in `upgrades.alwaysCompactedItems` (NoTreePunching's rocks by default), which no recipe crafts back.
- Shulker boxes' "container items" option counts items that carry an inventory (1.12 has no such item flag).

See [PLAN.md](PLAN.md) for the full porting plan.

## Building

```
gradlew build
```

The jar is written to `build/libs/`. Soft-dependency jars (JEI) go in `deps/` and are not committed.
