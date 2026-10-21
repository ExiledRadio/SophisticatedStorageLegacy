"""Imports the copper and netherite tier and crimson and warped wood assets (GPL) for the
compat tiers and woods, which exist when another mod adds those materials.

Usage:
    python -I tools/import_compat_assets.py <SS118Reference dir> <SS120Reference dir> <SophisticatedStorageLegacy dir>

The 1.18.x pin (891b0d9) has every texture except the double chest halves, which come from the
1.20.x branch at a41c8065 (also GPLv3). tools/import_assets.py skipped all of these when the
port had no such tiers or woods. Run tools/make_nether_star_tier_upgrades.py afterwards for
copper_to_nether_star, then the barrel model generators.
"""
import json
import os
import shutil
import sys

OLD_NS = "sophisticatedstorage"
NEW_NS = "sstoragelegacy"


def textures(root):
    return os.path.join(root, "src", "main", "resources", "assets", OLD_NS, "textures")


def main():
    ref118, ref120, mod = sys.argv[1], sys.argv[2], sys.argv[3]
    dst = os.path.join(mod, "src", "main", "resources", "assets", NEW_NS)
    copies = []
    for tier in ("copper", "netherite"):
        for face in ("top", "side", "bottom"):
            copies.append((ref118, "block/%s_barrel_%s.png" % (tier, face)))
        copies.append((ref118, "entity/chest/%s_tier.png" % tier))
        copies.append((ref118, "entity/shulker_box/%s_tier.png" % tier))
        for side in ("left", "right"):
            copies.append((ref120, "entity/chest/%s_%s_tier.png" % (side, tier)))
    for wood in ("crimson", "warped"):
        for face in ("top", "top_open", "side", "bottom"):
            copies.append((ref118, "block/%s_barrel_%s.png" % (wood, face)))
        for n in range(1, 5):
            copies.append((ref118, "block/limited_%s_barrel_%d_top.png" % (wood, n)))
        copies.append((ref118, "entity/chest/%s.png" % wood))
        for side in ("left", "right"):
            copies.append((ref120, "entity/chest/%s_%s.png" % (side, wood)))
    upgrades = ["basic_to_copper", "copper_to_iron", "copper_to_gold", "copper_to_diamond", "copper_to_netherite",
                "basic_to_netherite", "iron_to_netherite", "gold_to_netherite", "diamond_to_netherite"]
    for name in upgrades:
        copies.append((ref118, "item/%s_tier_upgrade.png" % name))

    for root, rel in copies:
        src = os.path.join(textures(root), rel)
        out = os.path.join(dst, "textures", rel)
        os.makedirs(os.path.dirname(out), exist_ok=True)
        shutil.copy(src, out)
    for name in upgrades:
        path = os.path.join(dst, "models", "item", name + "_tier_upgrade.json")
        with open(path, "w", encoding="utf-8", newline="\n") as fh:
            json.dump({"parent": "item/generated", "textures": {"layer0": "%s:item/%s_tier_upgrade" % (NEW_NS, name)}}, fh, indent=2)
            fh.write("\n")
    print("copied", len(copies), "textures and", len(upgrades), "item models")


if __name__ == "__main__":
    main()
