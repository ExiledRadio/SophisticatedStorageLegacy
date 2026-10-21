"""Imports the original's double chest textures (left and right halves) for the 1.12 woods.

Usage:
    python -I tools/import_double_chest_textures.py <SS120Reference dir> <SophisticatedStorageLegacy dir>

Source: Sophisticated Storage 1.20.x at a41c8065 (GPLv3), where double chests were added. Copies
left_/right_ textures for the six 1.12 woods, the tintable layers, packing tape and the
wood/iron/gold/diamond tier trims. The 5th tier's trim (Nether Star, replacing netherite) is the
diamond one with diamond's three tones swapped for the Nether Star tones, as
tools/make_nether_star_barrel.py does for the single chest.
"""
import os
import shutil
import sys

from PIL import Image

WOODS = ["oak", "spruce", "birch", "jungle", "acacia", "dark_oak"]
OTHER = ["tintable_main", "tintable_accent", "packed", "wood_tier", "iron_tier", "gold_tier", "diamond_tier"]
DIAMOND_TONES = [(74, 237, 217), (161, 251, 232), (213, 255, 246)]
NETHER_STAR_TONES = [(150, 136, 186), (196, 188, 222), (255, 255, 244)]


def main():
    ref, mod = sys.argv[1], sys.argv[2]
    src = os.path.join(ref, "src", "main", "resources", "assets", "sophisticatedstorage", "textures", "entity", "chest")
    dst = os.path.join(mod, "src", "main", "resources", "assets", "sstoragelegacy", "textures", "entity", "chest")
    for side in ("left", "right"):
        for name in WOODS + OTHER:
            shutil.copy(os.path.join(src, f"{side}_{name}.png"), os.path.join(dst, f"{side}_{name}.png"))
        im = Image.open(os.path.join(src, f"{side}_diamond_tier.png")).convert("RGBA")
        found = {p[:3] for p in im.getdata() if p[3] > 0}
        missing = [t for t in DIAMOND_TONES if t not in found]
        if missing:
            raise SystemExit(f"{side}_diamond_tier.png lacks the expected diamond tones {missing}")
        mapping = dict(zip(DIAMOND_TONES, NETHER_STAR_TONES))
        out = Image.new("RGBA", im.size)
        out.putdata([mapping[p[:3]] + (p[3],) if p[3] > 0 and p[:3] in mapping else p for p in im.getdata()])
        out.save(os.path.join(dst, f"{side}_nether_star_tier.png"))
    print("imported", 2 * (len(WOODS) + len(OTHER) + 1), "textures into", dst)


if __name__ == "__main__":
    main()
