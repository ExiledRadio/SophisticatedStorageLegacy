"""Generates the Nether Star tier upgrade item textures and models.

Usage:
    python -I tools/make_nether_star_tier_upgrades.py <SophisticatedStorageLegacy dir>

The original's netherite tier is the Nether Star here (as in Backpacks Legacy). Each
"<from>_to_nether_star" texture is the matching "<from>_to_diamond" one (GPL, imported from the
original) with the five diamond tones swapped for Backpacks Legacy's Nether Star ramp.
diamond_to_nether_star starts from iron_to_diamond and also turns the iron "from" marker into
diamond's colour, the way gold_to_diamond marks gold.
"""
import json
import os
import sys

from PIL import Image

DIAMOND_TONES = [(28, 145, 154), (32, 197, 181), (74, 237, 217), (161, 251, 232), (213, 255, 246)]
NETHER_STAR_RAMP = [(112, 96, 146), (150, 136, 186), (196, 188, 222), (234, 231, 214), (255, 255, 244)]
IRON_MARKER = (216, 216, 216)
DIAMOND_MARKER = (74, 237, 217)

# result name -> (source texture, extra colour swaps applied before the diamond tones change)
VARIANTS = {
    "basic_to_nether_star_tier_upgrade": ("basic_to_diamond_tier_upgrade", {}),
    "iron_to_nether_star_tier_upgrade": ("iron_to_diamond_tier_upgrade", {}),
    "gold_to_nether_star_tier_upgrade": ("gold_to_diamond_tier_upgrade", {}),
    "diamond_to_nether_star_tier_upgrade": ("iron_to_diamond_tier_upgrade", {IRON_MARKER: "marker"}),
    # Copper storages (when another mod adds copper) go to the Nether Star too.
    "copper_to_nether_star_tier_upgrade": ("copper_to_diamond_tier_upgrade", {}),
}


def main():
    mod = sys.argv[1]
    assets = os.path.join(mod, "src", "main", "resources", "assets", "sstoragelegacy")
    tone_map = dict(zip(DIAMOND_TONES, NETHER_STAR_RAMP))
    for name, (source, extra) in VARIANTS.items():
        im = Image.open(os.path.join(assets, "textures", "item", source + ".png")).convert("RGBA")
        out = []
        for p in im.getdata():
            rgb = p[:3]
            if p[3] == 0:
                out.append(p)
            elif extra.get(rgb) == "marker":
                # The "from" marker becomes diamond, and must not then be recoloured as a target tone.
                out.append(DIAMOND_MARKER + (p[3],))
            else:
                out.append(tone_map.get(rgb, rgb) + (p[3],))
        result = Image.new("RGBA", im.size)
        result.putdata(out)
        result.save(os.path.join(assets, "textures", "item", name + ".png"))
        with open(os.path.join(assets, "models", "item", name + ".json"), "w", encoding="utf-8", newline="\n") as fh:
            json.dump({"parent": "item/generated", "textures": {"layer0": "sstoragelegacy:item/" + name}}, fh, indent=2)
            fh.write("\n")
        print("wrote", name)


if __name__ == "__main__":
    main()
