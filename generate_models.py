#!/usr/bin/env python3
"""
Give every item a model, so the client has something to draw.

An item registered through Pandorical takes its appearance from the synced assets and nothing
else: `ItemRegistration.model()` crosses the wire but is advisory, and the client synthesises no
fallback. An item with no model file is not a plain-looking item, it is the missing-model cube -
and because the server neither knows nor cares, it builds green and deploys clean. The seven
smoked foods shipped that way.

So the list is not kept by hand here. Anything with a texture gets a model built around it, and
the smoked foods - which deliberately have no texture of their own, being the same meat kept
differently - point at the vanilla item whose look they borrow.

Usage: python3 generate_models.py
"""

import json
import pathlib
import sys
import zipfile

HERE = pathlib.Path(__file__).parent
ASSETS = HERE / "src/main/resources/assets/lets-cook-justfatlard"
NAMESPACE = "lets-cook-justfatlard"

# The smoked foods wear their cooked counterpart's look; this is the same pairing Foods.java
# registers, and the two have to agree.
SMOKED = {
    "smoked_beef": "cooked_beef",
    "smoked_porkchop": "cooked_porkchop",
    "smoked_mutton": "cooked_mutton",
    "smoked_chicken": "cooked_chicken",
    "smoked_rabbit": "cooked_rabbit",
    "smoked_cod": "cooked_cod",
    "smoked_salmon": "cooked_salmon",
}


DEFAULT_JAR_GLOB = (
    ".gradle/caches/fabric-loom/minecraftMaven/net/minecraft/"
    "minecraft-merged-deobf/*/minecraft-merged-deobf-*.jar"
)

# A wheel of cheese is cake's shape: a block-wide round eaten in slices from one side. Its models
# are cake's models with cheese on them, read out of the jar rather than copied out by hand, so
# the seven bite stages cannot drift from the seven the block actually has.
WHEELS = {
    "cheese_block": "cheese",
    "smoked_cheese_block": "smoked_cheese",
    "chocolate_cake": "chocolate_cake",
}

# A pie is not a cake. It sits in the same footprint and comes apart in the same seven bites, but
# it has a rolled crust standing up round the edge and a lid with vents cut through it, and
# under the vents is filling. Built here from boxes rather than borrowed from cake, so the lip
# and the holes are geometry the light falls into and not a picture of one.
PIES = ("pumpkin_pie", "sweet_berry_pie", "glow_berry_pie")
PIE_BODY_TOP = 6      # the filling's surface
PIE_LID_TOP = 7       # one pixel of crust over the filling
PIE_LIP_TOP = 8       # the rolled edge, a pixel proud of the lid
# The same squares generate_textures.py paints into the top sprite: one in the middle and one
# towards each corner, two pixels on a side.
PIE_VENTS = {(x + dx, y + dy) for x, y in ((7, 7), (3, 3), (11, 3), (3, 11), (11, 11))
             for dx in (0, 1) for dy in (0, 1)}

# Cheese remembers whether it aged, so it carries a second axis and needs a model per tier. The
# count has to agree with Vintage on the Java side; a mismatch shows up as a missing model rather
# than as anything subtle.
VINTAGED = {"cheese_block", "smoked_cheese_block"}
VINTAGES = 2
CAKE_FACES = {"cake_top": "top", "cake_side": "side",
              "cake_bottom": "bottom", "cake_inner": "inner"}


def find_jar(argv):
    if len(argv) > 1:
        return pathlib.Path(argv[1])
    matches = [m for m in sorted(pathlib.Path.home().glob(DEFAULT_JAR_GLOB))
               if "sources" not in m.name]
    if not matches:
        sys.exit("No game jar found. Pass one explicitly.")
    return matches[-1]


def wheels(jar):
    """Cake's seven models and its blockstate, retextured for each kind of cheese."""
    zf = zipfile.ZipFile(jar)
    stages = ["cake"] + [f"cake_slice{n}" for n in range(1, 7)]
    written = 0

    for block, prefix in WHEELS.items():
        tiers = range(VINTAGES) if block in VINTAGED else [None]
        variants = {}

        for tier in tiers:
            suffix = "" if tier is None else f"_v{tier}"
            skin = prefix if tier is None else f"{prefix}_v{tier}"

            for bites, stage in enumerate(stages):
                model = json.loads(zf.read(f"assets/minecraft/models/block/{stage}.json"))
                model["textures"] = {
                    key: f"{NAMESPACE}:block/{skin}_{CAKE_FACES[value.rsplit('/', 1)[-1]]}"
                    for key, value in model["textures"].items()
                }
                name = f"{block}{suffix}" if bites == 0 else f"{block}{suffix}_slice{bites}"
                write(ASSETS / "models/block" / f"{name}.json", model)
                written += 1

                key = f"bites={bites}" if tier is None else f"bites={bites},vintage={tier}"
                variants[key] = {"model": f"{NAMESPACE}:block/{name}"}

        write(ASSETS / "blockstates" / f"{block}.json", {"variants": variants})

    return written


def box(x0, y0, z0, x1, y1, z1, faces):
    """One model element. faces maps direction to texture key, or to a dict of face settings."""
    return {"from": [x0, y0, z0], "to": [x1, y1, z1],
            "faces": {d: (f if isinstance(f, dict) else {"texture": f}) for d, f in faces.items()}}


def pie_model(bites):
    """A pie with this many bites out of it, cut from the west like cake."""
    x0 = 1 + bites * 2
    cut = "#inner" if bites else "#side"
    elements = [
        # The filling and the crust it sits in. Its top face is what the vents look down on.
        box(x0, 0, 1, 15, PIE_BODY_TOP, 15, {
            "down": {"texture": "#bottom", "cullface": "down"}, "up": "#fill",
            "north": "#side", "south": "#side", "east": "#side", "west": cut}),
    ]

    # The lid: one pixel of crust over the filling, inside the lip, with the vents left out. Cut
    # as strips of whole lid between the holes, banded by rows that share a hole pattern, so
    # every vent gets four walls of crust of its own.
    lid_x0 = max(x0, 2)
    rows = {}
    for z in range(2, 14):
        rows.setdefault(tuple(x for x in range(lid_x0, 14) if (x, z) in PIE_VENTS), []).append(z)
    for holes, zs in rows.items():
        # rows with one pattern are not always contiguous (the corner vents share one), so band
        # each contiguous run separately
        run = [zs[0]]
        for z in zs[1:] + [None]:
            if z is not None and z == run[-1] + 1:
                run.append(z)
                continue
            x = lid_x0
            for hole in list(holes) + [14]:
                if hole > x:
                    elements.append(box(x, PIE_BODY_TOP, run[0], hole, PIE_LID_TOP, run[-1] + 1, {
                        "up": "#top", "north": "#side", "south": "#side",
                        "east": "#side", "west": cut if x == x0 else "#side"}))
                x = hole + 1
            run = [z]

    # The lip: the rolled edge, a pixel wide and a pixel proud of the lid. No west bar once a
    # slice is gone; that edge is the cut, and the cut face carries the crust band instead.
    lip = {"up": "#top", "north": "#side", "south": "#side", "east": "#side", "west": "#side"}
    elements.append(box(x0, PIE_BODY_TOP, 1, 15, PIE_LIP_TOP, 2, dict(lip, west=cut)))
    elements.append(box(x0, PIE_BODY_TOP, 14, 15, PIE_LIP_TOP, 15, dict(lip, west=cut)))
    elements.append(box(14, PIE_BODY_TOP, 1, 15, PIE_LIP_TOP, 15, lip))
    if bites == 0:
        elements.append(box(1, PIE_BODY_TOP, 1, 2, PIE_LIP_TOP, 15, lip))
    return elements


def pies():
    """Seven bite stages and a blockstate for each pie."""
    written = 0
    for pie in PIES:
        variants = {}
        for bites in range(7):
            name = pie if bites == 0 else f"{pie}_slice{bites}"
            write(ASSETS / "models/block" / f"{name}.json", {
                "textures": {face: f"{NAMESPACE}:block/{pie}_{face}"
                             for face in ("top", "side", "bottom", "inner", "fill")}
                            | {"particle": f"{NAMESPACE}:block/{pie}_side"},
                "elements": pie_model(bites),
            })
            written += 1
            variants[f"bites={bites}"] = {"model": f"{NAMESPACE}:block/{name}"}
        write(ASSETS / "blockstates" / f"{pie}.json", {"variants": variants})
    return written


WRITTEN = set()


def write(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2) + "\n")
    WRITTEN.add(path.resolve())


def prune(*directories):
    """Delete anything in a generated directory that this run did not write.

    Generated output that outlives the thing that generated it is the worst kind of stale: it is
    still valid JSON, still loads, and still points at a texture - so nothing complains, and a
    blockstate quietly keeps referring to a model nobody makes any more. Adding the vintage axis
    orphaned eight cheese textures and seven models on the first run.
    """
    gone = 0
    for directory in directories:
        if not directory.exists():
            continue
        for stale in sorted(directory.rglob("*.json")):
            if stale.resolve() not in WRITTEN:
                stale.unlink()
                gone += 1
    return gone


def definition(name, model):
    """The item's entry point: which model to draw it with."""
    write(ASSETS / "items" / f"{name}.json",
          {"model": {"type": "minecraft:model", "model": model}})


def main():
    own = sorted(p.stem for p in (ASSETS / "textures/item").glob("*.png"))

    for name in own:
        # A flat sprite of our own texture, the way every vanilla food is drawn.
        write(ASSETS / "models/item" / f"{name}.json", {
            "parent": "minecraft:item/generated",
            "textures": {"layer0": f"{NAMESPACE}:item/{name}"},
        })
        definition(name, f"{NAMESPACE}:item/{name}")

    # Borrowed from Minedew Fishing: where that mod is installed, a smoked fillet wears its cooked
    # fillet, there being no texture of ours behind it. Written here rather than kept by hand
    # because the prune at the end deletes whatever this run did not write - which quietly ate both
    # of these, and the fillet integration with them, every single time this script ran.
    for cut in ("cod", "salmon"):
        write(ASSETS / "models/item" / f"smoked_{cut}_fillet.json", {
            "parent": "minecraft:item/generated",
            "textures": {"layer0": f"minedew-fishing:item/cooked_{cut}_fillet"},
        })

    made = wheels(find_jar(sys.argv)) + pies()

    # Chocolate cake is held as the block it is, the way vanilla's cake is: no flat sprite for it,
    # because a cake in the hand is a cake.
    definition("chocolate_cake", f"{NAMESPACE}:block/chocolate_cake")

    for name, vanilla in SMOKED.items():
        # Straight at vanilla's model: there is no texture of ours to wrap, and the client
        # already has that one.
        definition(name, f"minecraft:item/{vanilla}")

    gone = prune(ASSETS / "models/block", ASSETS / "models/item",
                 ASSETS / "items", ASSETS / "blockstates")
    print(f"  {len(own)} textured models, {len(SMOKED)} borrowed, "
          f"{made} wheel models -> assets/{NAMESPACE}"
          + (f" ({gone} stale removed)" if gone else ""))


if __name__ == "__main__":
    main()
