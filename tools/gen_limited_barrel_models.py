"""Builds the 1.12 limited barrel block and item models from the 1.18.x reference.

Usage (Windows paths):
    python -I tools/gen_limited_barrel_models.py <SS118Reference dir> <SophisticatedStorageLegacy dir>

Limited barrels (1 to 4 slots) are barrels with a divided front, so this reuses
gen_barrel_models.py's helpers. The reference's limited_barrel_2..4 models inherit from
limited_barrel_1 and override some parts; that chain is merged here the same way. Each
limited barrel block gets a multipart blockstate driven by:

    facing, vertical_facing   saved in metadata (horizontal front, or up/down with a horizontal
                              "top" so the count text stays upright, as in the original)
    flat_top, wood, tint,
    packed, locked            from the tile

Item models are barrel/limited_<n>_<tier prefix><look>. Safe to re-run.
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import gen_barrel_models as gen  # noqa: E402

SLOT_COUNTS = (1, 2, 3, 4)
# (facing, vertical_facing) -> (x, y), from the reference's limited_barrel_1 blockstate.
ROTATIONS = {}
for _facing, _y in (("east", 270), ("north", 180), ("south", 0), ("west", 90)):
    ROTATIONS[(_facing, "no")] = (270, _y)
    ROTATIONS[(_facing, "down")] = (180, _y)
    ROTATIONS[(_facing, "up")] = (0, _y)


def merged_loader(ref_models, n, flat):
    """The reference's limited_barrel_<n> model with its limited_barrel_1 parent merged in."""
    folder = "block/flat/" if flat else "block/"
    own = gen.load(ref_models, folder + "limited_barrel_%d" % n)
    if n == 1:
        return own
    parent = merged_loader(ref_models, 1, flat)
    parts = {k: dict(v) for k, v in parent["model_parts"].items()}
    for key, spec in own.get("model_parts", {}).items():
        if "model" in spec:
            parts[key] = spec
        else:
            merged = dict(parts.get(key, {}))
            textures = dict(merged.get("textures", {}))
            textures.update(spec.get("textures", {}))
            merged["textures"] = textures
            parts[key] = merged
    # A child's wood override only sets what differs (the front); the rest comes from the
    # parent's override for the same wood.
    woods = {}
    for wood, parent_parts in parent.get("wood_overrides", {}).items():
        woods[wood] = {part: {"textures": dict(spec.get("textures", {}))} for part, spec in parent_parts.items()}
    for wood, own_parts in own.get("wood_overrides", {}).items():
        for part, spec in own_parts.items():
            woods.setdefault(wood, {}).setdefault(part, {"textures": {}})["textures"].update(spec.get("textures", {}))
    return {"model_parts": parts, "wood_overrides": woods}


def load_resolved(ref_models, name):
    """A part model with its parent chain resolved: elements from the nearest model that has
    them (or, for the original's "simple_composite" loader, every listed part's elements
    together), textures from the whole chain (the child's win)."""
    model = gen.load(ref_models, name)
    textures = dict(model.get("textures", {}))
    if model.get("loader", "").endswith("simple_composite"):
        elements = []
        for part in model["parts"].values():
            resolved = load_resolved(ref_models, part["parent"].split(":")[-1].lstrip("/"))
            elements.extend(resolved["elements"])
            for key, value in resolved["textures"].items():
                textures.setdefault(key, value)
        return {"textures": textures, "elements": elements}
    while "elements" not in model and "parent" in model:
        model = gen.load(ref_models, model["parent"].split(":")[-1].lstrip("/"))
        for key, value in model.get("textures", {}).items():
            textures.setdefault(key, value)
    resolved = dict(model)
    resolved["textures"] = textures
    resolved.pop("parent", None)
    return resolved


class LimitedParts:
    def __init__(self, ref_models):
        self.models = {}
        for n in SLOT_COUNTS:
            for flat in (False, True):
                suffix = "_flat" if flat else ""
                loader = merged_loader(ref_models, n, flat)
                parts = loader["model_parts"]

                def part(key, extra=None):
                    spec = parts[key]
                    source = spec["model"].split(":")[1].lstrip("/")
                    textures = dict(spec.get("textures", {}))
                    if extra:
                        textures.update(extra)
                    if source == "block/cube_bottom_top":
                        return gen.with_textures(gen.cube_bottom_top({}), textures)
                    return gen.with_textures(gen.port_part(load_resolved(ref_models, source)), textures)

                prefix = "limited_%d_" % n
                for wood in gen.WOODS:
                    self.models["%swood_%s%s" % (prefix, wood, suffix)] = part("base", loader["wood_overrides"][wood]["base"]["textures"])
                self.models[prefix + "tintable_main" + suffix] = part("tintable_main")
                self.models[prefix + "tintable_accent" + suffix] = part("tintable_accent")
                self.models[prefix + "packed" + suffix] = part("packed")
                self.models[prefix + "locked" + suffix] = part("locked")
                for tier in gen.TIERS:
                    textures = {face: "%s:block/%s_barrel_%s" % (gen.OLD_NS, tier, face) for face in ("top", "side", "bottom")}
                    textures["top_trim"] = textures["top"]
                    textures["particle"] = textures["side"]
                    self.models["%stier_%s%s" % (prefix, tier, suffix)] = part("tier", textures)

    def stack(self, n, wood, tint, flat, tier=None):
        prefix = "limited_%d_" % n
        f = "_flat" if flat else ""
        layers = []
        if tint != "both":
            layers.append(self.models["%swood_%s%s" % (prefix, wood, f)])
        if tint in ("accent", "both"):
            layers.append(self.models[prefix + "tintable_accent" + f])
        if tint in ("main", "both"):
            layers.append(self.models[prefix + "tintable_main" + f])
        if tier:
            layers.append(self.models["%stier_%s%s" % (prefix, tier, f)])
        return layers


def limited_multipart(n, tier):
    prefix = "%s:barrel/limited_%d_" % (gen.NEW_NS, n)
    multipart = []
    for (facing, vertical), (x, y) in ROTATIONS.items():
        for flat in (False, True):
            f = "_flat" if flat else ""
            when = {"facing": facing, "vertical_facing": vertical, "flat_top": "true" if flat else "false"}

            def add(extra, model):
                condition = dict(when)
                condition.update(extra)
                multipart.append({"when": condition, "apply": {"model": prefix + model + f, "x": x, "y": y}})

            for wood in gen.WOODS:
                add({"wood": wood, "tint": "none|main|accent"}, "wood_" + wood)
            add({"tint": "accent|both"}, "tintable_accent")
            add({"tint": "main|both"}, "tintable_main")
            if tier:
                add({"show_tier": "true"}, "tier_" + tier)
            add({"packed": "true"}, "packed")
            add({"locked": "true"}, "locked")
    return multipart


def main():
    ref, mod = sys.argv[1], sys.argv[2]
    ref_models = os.path.join(ref, "src", "main", "resources", "assets", gen.OLD_NS, "models")
    assets = os.path.join(mod, "src", "main", "resources", "assets", gen.NEW_NS)
    parts = LimitedParts(ref_models)
    for name, model in parts.models.items():
        gen.write(os.path.join(assets, "models", "block", "barrel", name + ".json"), model)

    tiers = (None,) + gen.TIERS
    for n in SLOT_COUNTS:
        for prefix, tier in (("" if t is None else t + "_", t) for t in tiers):
            gen.write(os.path.join(assets, "blockstates", "limited_%sbarrel_%d.json" % (prefix, n)),
                      {"multipart": limited_multipart(n, tier)})

    count = 0
    for n in SLOT_COUNTS:
        for prefix, tier in (("" if t is None else t + "_", t) for t in tiers):
            for flat in (False, True):
                for tint in gen.TINTS:
                    for wood in gen.WOODS:
                        key = gen.item_key(wood, tint, flat)
                        path = os.path.join(assets, "models", "item", "barrel", "limited_%d_%s%s.json" % (n, prefix, key))
                        if os.path.exists(path) and tint == "both" and wood != gen.WOODS[0]:
                            continue
                        gen.write(path, gen.merge(parts.stack(n, wood, tint, flat, tier)))
                        count += 1
    print("part models:", len(parts.models))
    print("item models:", count)


if __name__ == "__main__":
    main()
