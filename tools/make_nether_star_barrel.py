"""Generates the 5th barrel tier's corner textures.

Usage:
    python -I tools/make_nether_star_barrel.py <SS118Reference dir> <SophisticatedStorageLegacy dir>

Netherite doesn't exist in 1.12, so the 5th tier is the Nether Star, as in Backpacks Legacy.
Takes the original's diamond barrel corners (GPL) and swaps their three-tone ramp for three
tones of the Nether Star palette Backpacks Legacy uses for its clips, so the two mods match.

Writes textures/block/nether_star_barrel_{top,side,bottom}.png, and the chest and shulker box
tier overlays textures/entity/{chest,shulker_box}/nether_star_tier.png from their diamond ones
(only the three diamond tones change; the shared grey lock pixels stay).
"""
import os
import sys

from PIL import Image

# Backpacks Legacy's Nether Star ramp (tools/make_nether_star_clips.py), darkest to lightest.
NETHER_STAR_RAMP = [
    (112, 96, 146),
    (150, 136, 186),
    (196, 188, 222),
    (234, 231, 214),
    (255, 255, 244),
]
# Diamond's three tones are a shadow, a mid tone and a highlight: keep that spread.
PICKED_TONES = [NETHER_STAR_RAMP[1], NETHER_STAR_RAMP[2], NETHER_STAR_RAMP[4]]


def luminance(rgb):
    r, g, b = rgb[:3]
    return 0.299 * r + 0.587 * g + 0.114 * b


def main():
    ref, mod = sys.argv[1], sys.argv[2]
    src = os.path.join(ref, "src", "main", "resources", "assets", "sophisticatedstorage", "textures", "block")
    dst = os.path.join(mod, "src", "main", "resources", "assets", "sstoragelegacy", "textures", "block")
    for face in ("top", "side", "bottom"):
        im = Image.open(os.path.join(src, "diamond_barrel_%s.png" % face)).convert("RGBA")
        tones = sorted({p[:3] for p in im.getdata() if p[3] > 0}, key=luminance)
        if len(tones) != len(PICKED_TONES):
            raise SystemExit("expected %d tones in diamond_barrel_%s, found %d" % (len(PICKED_TONES), face, len(tones)))
        mapping = dict(zip(tones, PICKED_TONES))
        out = Image.new("RGBA", im.size)
        out.putdata([mapping[p[:3]] + (p[3],) if p[3] > 0 else p for p in im.getdata()])
        path = os.path.join(dst, "nether_star_barrel_%s.png" % face)
        out.save(path)
        print("wrote", path)

    diamond_tones = [(74, 237, 217), (161, 251, 232), (213, 255, 246)]
    mapping = dict(zip(diamond_tones, PICKED_TONES))
    entity = os.path.join(mod, "src", "main", "resources", "assets", "sstoragelegacy", "textures", "entity")
    for kind in ("chest", "shulker_box"):
        im = Image.open(os.path.join(entity, kind, "diamond_tier.png")).convert("RGBA")
        out = Image.new("RGBA", im.size)
        out.putdata([mapping[p[:3]] + (p[3],) if p[3] > 0 and p[:3] in mapping else p for p in im.getdata()])
        path = os.path.join(entity, kind, "nether_star_tier.png")
        out.save(path)
        print("wrote", path)


if __name__ == "__main__":
    main()
