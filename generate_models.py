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
    "pumpkin_pie": "pumpkin_pie",
    "sweet_berry_pie": "sweet_berry_pie",
    "glow_berry_pie": "glow_berry_pie",
    "chocolate_cake": "chocolate_cake",
}

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

    made = wheels(find_jar(sys.argv))

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
