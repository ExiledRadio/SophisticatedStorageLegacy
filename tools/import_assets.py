"""One-time import of GPL assets from the 1.18.x reference checkout into this mod.

Usage (run from anywhere, with Windows paths):
    python -I tools/import_assets.py <SS118Reference dir> <SophisticatedStorageLegacy dir>

Copies textures and the plain item/generated item models, and converts the 1.18 JSON lang
files into 1.12 .lang files. Barrel/chest/shulker block models are not copied: those use the
original's custom model loaders, and tools/gen_barrel_models.py builds plain 1.12 models
from them instead.

Safe to re-run: it overwrites what it wrote last time and touches nothing else.
"""
import json
import os
import shutil
import sys

OLD_NS = "sophisticatedstorage"
NEW_NS = "sstoragelegacy"

# Content that doesn't exist in 1.12: copper and netherite tiers, nether woods, and the
# stonecutter / smoker / blast furnace upgrades. Chipped compat is dropped as well.
DROPPED = (
    "copper",
    "netherite",
    "crimson",
    "warped",
    "stonecutter",
    "smoking",
    "blasting",
    "chipped",
)

# 1.12 lang files are matched by lower-case locale code once pack.mcmeta says pack_format 3.
LOCALES = ("en_us", "de_de", "es_mx", "ja_jp", "ko_kr", "pt_br", "ru_ru", "uk_ua", "zh_cn", "zh_tw")


def is_dropped(name):
    return any(word in name for word in DROPPED)


def copy_tree(src, dst, counter):
    for root, _dirs, files in os.walk(src):
        rel = os.path.relpath(root, src)
        if is_dropped(rel):
            continue
        for f in files:
            if is_dropped(f):
                continue
            out_dir = os.path.join(dst, rel)
            os.makedirs(out_dir, exist_ok=True)
            shutil.copyfile(os.path.join(root, f), os.path.join(out_dir, f))
            counter[0] += 1


def port_item_models(src, dst):
    """Copy plain item/generated models; anything with a custom loader or block parent is skipped."""
    os.makedirs(dst, exist_ok=True)
    copied, skipped = [], []
    for f in sorted(os.listdir(src)):
        path = os.path.join(src, f)
        if os.path.isdir(path) or is_dropped(f):
            continue
        with open(path, encoding="utf-8") as fh:
            model = json.load(fh)
        if model.get("parent") not in ("item/generated", "minecraft:item/generated") or "loader" in model:
            skipped.append(f)
            continue
        model["parent"] = "item/generated"
        text = json.dumps(model, indent=2).replace(OLD_NS + ":", NEW_NS + ":")
        with open(os.path.join(dst, f), "w", encoding="utf-8", newline="\n") as fh:
            fh.write(text + "\n")
        copied.append(f)
    return copied, skipped


def convert_key(key):
    parts = key.split(".")
    if parts[1:2] == [OLD_NS]:
        parts[1] = NEW_NS
    elif parts[0] == "itemGroup" and parts[1:] == [OLD_NS]:
        parts[1] = NEW_NS
    # 1.12 names items "item.<key>.name" and blocks "tile.<key>.name"; sub-keys like
    # tile.<ns>.<name>.tooltip.flat_top stay as they are.
    if parts[0] == "block":
        parts[0] = "tile"
    if parts[0] in ("item", "tile") and len(parts) == 3:
        parts.append("name")
    return ".".join(parts)


def convert_value(value):
    # .lang files are one entry per line. Newlines become a literal \n, which our tooltip
    # code splits on (vanilla 1.12 does not unescape lang values).
    return value.replace(OLD_NS + ":", NEW_NS + ":").replace("\r", "").replace("\n", "\\n")


def convert_lang(src_dir, dst_dir):
    os.makedirs(dst_dir, exist_ok=True)
    counts = {}
    for locale in LOCALES:
        src = os.path.join(src_dir, locale + ".json")
        if not os.path.exists(src):
            continue
        with open(src, encoding="utf-8") as fh:
            entries = json.load(fh)
        lines = []
        for key, value in entries.items():
            if is_dropped(key):
                continue
            lines.append("%s=%s" % (convert_key(key), convert_value(value)))
        out = os.path.join(dst_dir, locale + ".lang")
        with open(out, "w", encoding="utf-8", newline="\n") as fh:
            fh.write("# Converted from Sophisticated Storage (c) P3pp3rF1y, GPLv3.\n")
            fh.write("\n".join(lines) + "\n")
        counts[locale] = len(lines)
    return counts


def main():
    ref, mod = sys.argv[1], sys.argv[2]
    src_assets = os.path.join(ref, "src", "main", "resources", "assets", OLD_NS)
    dst_assets = os.path.join(mod, "src", "main", "resources", "assets", NEW_NS)

    textures = [0]
    # Folder names are kept (item/, block/, entity/, gui/): 1.12 resolves "<ns>:block/x" the
    # same way, so model texture paths only need their namespace rewritten.
    copy_tree(os.path.join(src_assets, "textures"), os.path.join(dst_assets, "textures"), textures)
    print("textures copied:", textures[0])

    copied, skipped = port_item_models(os.path.join(src_assets, "models", "item"),
                                       os.path.join(dst_assets, "models", "item"))
    print("item models copied:", len(copied))
    print("item models left for the model generators:", ", ".join(skipped))

    counts = convert_lang(os.path.join(src_assets, "lang"), os.path.join(dst_assets, "lang"))
    print("lang entries:", counts)


if __name__ == "__main__":
    main()
