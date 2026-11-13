"""Writes a copy of the 1.18.x reference's models with the 1.20.x branch's mangrove, cherry and
bamboo wood overrides added, for the barrel model generators.

Usage:
    python -I tools/merge_reference_models.py <SS118Reference dir> <SS120Reference dir> <output dir>

The generators were written against the 1.18.x pin (891b0d9) and reproduce our models exactly from
it; those woods only exist from 1.19 on. 1.20.x at a41c8065 (GPLv3) has their overrides in the
same format, and its crimson, warped and oak overrides equal 1.18's, so only the new woods are
taken from it. Its flat limited barrels have no mangrove override (they would show as acacia);
the non-flat one's, which uses the same textures, fills the gap. Then run the generators with
<output dir> in place of the SS118Reference dir.
"""
import json, os, shutil, sys
ref118, ref120, out = sys.argv[1:4]
NEW = ("mangrove", "cherry", "bamboo")
rel = os.path.join("src", "main", "resources", "assets", "sophisticatedstorage", "models")
src = os.path.join(ref118, rel)
dst = os.path.join(out, rel)
if os.path.isdir(out):
    shutil.rmtree(out)
shutil.copytree(src, dst)
n = same = diff = 0
for root, _, files in os.walk(dst):
    for f in files:
        if not f.endswith(".json"):
            continue
        p = os.path.join(root, f)
        r = os.path.relpath(p, dst)
        p120 = os.path.join(ref120, rel, r)
        d = json.load(open(p, encoding="utf-8"))
        if "wood_overrides" not in d or not os.path.exists(p120):
            continue
        d120 = json.load(open(p120, encoding="utf-8"))
        wo = d120.get("wood_overrides", {})
        for w in ("crimson", "warped", "oak"):
            if w in d["wood_overrides"]:
                if d["wood_overrides"][w] == wo.get(w):
                    same += 1
                else:
                    diff += 1; print("differs", r, w)
        for w in NEW:
            if w in wo:
                d["wood_overrides"][w] = wo[w]; n += 1
            else:
                # The original's flat limited barrels lack mangrove; use the non-flat one's (same textures).
                alt = os.path.join(ref120, rel, r.replace("flat" + os.sep, ""))
                d["wood_overrides"][w] = json.load(open(alt, encoding="utf-8"))["wood_overrides"][w]
                print("filled", r, w, "from", alt); n += 1
        json.dump(d, open(p, "w", encoding="utf-8"), indent=4)
print("injected", n, "same", same, "diff", diff)
