#!/usr/bin/env python3
"""
Draw the mod's food, out of the game's own pixels.

Almost nothing here is drawn from scratch, and that is the point: a dish built by recolouring
vanilla's bowl keeps vanilla's silhouette, vanilla's rim, vanilla's dithering and vanilla's
one-pixel highlight, so it sits in a hotbar beside mushroom stew without looking like a guest.

Two techniques do most of the work:

  contents  The difference between mushroom_stew and an empty bowl is exactly the soup. Lift
            that mask, read its brightness, and paint it back through a new colour ramp - the
            shading survives because it was never redrawn, only re-tinted.

  tint      Vanilla renders a potion as a greyscale liquid tinted by colour and a glass bottle
            laid over it. Doing the same gets a drink that is a real potion bottle rather than
            something shaped like one.

Usage: python3 generate_textures.py [path/to/minecraft-merged-deobf-<version>.jar]
"""

import pathlib
import random
import subprocess
import sys
import zipfile
from io import BytesIO

from PIL import Image, ImageDraw

HERE = pathlib.Path(__file__).parent
OUT = HERE / "src/main/resources/assets/lets-cook-justfatlard/textures/item"

DEFAULT_JAR_GLOB = (
    ".gradle/caches/fabric-loom/minecraftMaven/net/minecraft/"
    "minecraft-merged-deobf/*/minecraft-merged-deobf-*.jar"
)


def find_jar(argv):
    if len(argv) > 1:
        return pathlib.Path(argv[1])
    matches = [m for m in sorted(pathlib.Path.home().glob(DEFAULT_JAR_GLOB))
               if "sources" not in m.name]
    if not matches:
        sys.exit("No game jar found. Pass one explicitly.")
    return matches[-1]


class Textures:
    """Vanilla item textures, read straight out of the jar."""

    def __init__(self, jar):
        self.zip = zipfile.ZipFile(jar)

    def get(self, name):
        return self.read(f"item/{name}")

    def block(self, name):
        return self.read(f"block/{name}")

    def read(self, path):
        with self.zip.open(f"assets/minecraft/textures/{path}.png") as handle:
            return Image.open(BytesIO(handle.read())).convert("RGBA")


def contents_mask(stew, bowl):
    """The soup, without the bowl: every pixel where the full bowl differs from the empty one."""
    mask = Image.new("L", stew.size, 0)
    for x in range(stew.width):
        for y in range(stew.height):
            s, b = stew.getpixel((x, y)), bowl.getpixel((x, y))
            if s[3] > 0 and s[:3] != b[:3]:
                mask.putpixel((x, y), 255)
    return mask


def ramp(dark, light):
    """A 256-step colour ramp between two ends, for repainting shading that already exists."""
    return [tuple(round(dark[i] + (light[i] - dark[i]) * n / 255) for i in range(3))
            for n in range(256)]


def bowl_of(bowl, stew, mask, dark, light):
    """Vanilla's bowl, filled with something else.

    The contents are shaded by where they sit rather than by borrowing the stew's own shading.
    That sounds like the harder way round, and it is the only one that works: mushroom stew's
    thirty-five contents pixels carry just six brightnesses, in two clumps with a gap in the
    middle. Re-tinting that gives a brown dish a passable dark rim and a light body, and gives a
    pale one a dark rim and a flat white smear - there is no middle to shade with.

    So the form is drawn instead: lit from the top-left the way every vanilla item is, and
    darkened at the rim so the food reads as sitting down inside the bowl rather than painted on.
    """
    points = [(x, y) for x in range(stew.width) for y in range(stew.height)
              if mask.getpixel((x, y))]
    left = min(x for x, _ in points)
    right = max(x for x, _ in points)
    top = min(y for _, y in points)
    bottom = max(y for _, y in points)
    span_x = max(right - left, 1)
    span_y = max(bottom - top, 1)

    out = bowl.copy()
    colours = ramp(dark, light)
    for x, y in points:
        across = (x - left) / span_x
        down = (y - top) / span_y

        # Minecraft lights its items from the upper left, so that corner is the bright one.
        shade = 1.0 - (across * 0.30 + down * 0.45)

        # A pixel with a hole beside it is the edge of the food, and the edge is in shadow.
        if any(not mask.getpixel((x + dx, y + dy))
               for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))
               if 0 <= x + dx < stew.width and 0 <= y + dy < stew.height):
            shade -= 0.28

        alpha = stew.getpixel((x, y))[3]
        out.putpixel((x, y), colours[max(0, min(255, round(shade * 255)))] + (alpha,))
    return out


def luminance(pixel):
    r, g, b = pixel[:3]
    return round(0.299 * r + 0.587 * g + 0.114 * b)


def outline(image, shade=0.55):
    """Darken the rim of a drawn shape.

    Vanilla items are edged - every sprite has a darker border a shade or two down from its fill,
    never a hard black, and it is most of why a hand-pixelled item reads as one. A flat shape with
    a clean geometric edge reads as a diagram instead.
    """
    px = image.load()
    solid = {(x, y) for x in range(16) for y in range(16) if px[x, y][3]}
    out = image.copy()
    for x, y in solid:
        if all((x + dx, y + dy) in solid for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
            continue
        r, g, b, a = px[x, y]
        out.putpixel((x, y), (round(r * shade), round(g * shade), round(b * shade), a))
    return out


def tinted_bottle(overlay, glass, colour, bubbles=0, seed=0):
    """A drink, the way vanilla makes one: tinted liquid with the glass laid over it."""
    liquid = Image.new("RGBA", overlay.size, (0, 0, 0, 0))
    for x in range(overlay.width):
        for y in range(overlay.height):
            r, g, b, a = overlay.getpixel((x, y))
            if a == 0:
                continue
            shade = r / 255
            liquid.putpixel((x, y), tuple(round(c * shade) for c in colour) + (a,))

    # Beer is the one drink you can tell by its bubbles, so it gets them.
    if bubbles:
        rng = random.Random(seed)
        wet = [(x, y) for x in range(16) for y in range(16) if liquid.getpixel((x, y))[3]]
        for x, y in rng.sample(wet, min(bubbles, len(wet))):
            r, g, b, a = liquid.getpixel((x, y))
            liquid.putpixel((x, y), (min(255, r + 70), min(255, g + 70), min(255, b + 55), a))

    return Image.alpha_composite(liquid, glass)


def recolour(image, dark, light, keep=lambda p: True):
    """Repaint a texture through a ramp, keeping its shading."""
    out = image.copy()
    colours = ramp(dark, light)
    for x in range(image.width):
        for y in range(image.height):
            r, g, b, a = image.getpixel((x, y))
            if a == 0 or not keep((r, g, b)):
                continue
            brightness = round(0.299 * r + 0.587 * g + 0.114 * b)
            out.putpixel((x, y), colours[brightness] + (a,))
    return out


# Each dish, as the two ends of its colour: the shadowed side and the lit side.
#
# The shadow end has to be genuinely dark even for a pale dish. Ice cream given a light-grey
# shadow has no shading left at all and reads as a white smear rather than a surface with a near
# side and a far side - the contents are only thirty-five pixels, and all the form is in the
# contrast across them.
BOWLS = {
    "bread_pudding":   ((0x54, 0x33, 0x16), (0xD9, 0xA8, 0x66)),
    "fruit_salad":     ((0x8C, 0x22, 0x3A), (0xF2, 0x82, 0x8C)),
    "vegetable_soup":  ((0x6B, 0x3A, 0x12), (0xE0, 0x9A, 0x3C)),
    "meat_stew":       ((0x4A, 0x27, 0x18), (0xA8, 0x5E, 0x3C)),
    "potato_chowder":  ((0x7A, 0x6A, 0x42), (0xF2, 0xE8, 0xC0)),
    "fish_chowder":    ((0x8A, 0x7E, 0x66), (0xEE, 0xE4, 0xCC)),
    "nether_chili":    ((0x5A, 0x10, 0x18), (0xC8, 0x44, 0x2C)),
}


def scoop_of(bowl, mask, dark, light, speckles=()):
    """A ball of ice cream sitting up out of the bowl, not a pool lying in it.

    Ice cream is the one dish here that is not a liquid, and drawing it as one was the tell: it
    read as melted. A scoop rises above the rim, is round rather than flat, and takes its light
    from the upper left like everything else, so the near-bottom of the ball is the dark part.

    Speckles are whatever was mixed in - berries, crumbled cookie - scattered on a fixed pattern
    so the texture regenerates identically every time.
    """
    points = [(x, y) for x in range(16) for y in range(16) if mask.getpixel((x, y))]
    centre_x = (min(x for x, _ in points) + max(x for x, _ in points)) / 2
    surface = min(y for _, y in points)

    out = bowl.copy()
    colours = ramp(dark, light)
    radius = 4.2
    cy = surface - 0.6

    for x in range(16):
        for y in range(16):
            dx, dy = x + 0.5 - centre_x, y + 0.5 - cy
            distance = (dx * dx + dy * dy) ** 0.5
            if distance > radius:
                continue

            # Round, and lit from the upper left: brightest where the light lands, darkest at the
            # underside where the ball meets the bowl.
            lift = 1.0 - distance / radius
            facing = 0.5 - (dx / radius) * 0.28 - (dy / radius) * 0.42
            shade = max(0.0, min(1.0, facing * 0.75 + lift * 0.4))
            out.putpixel((x, y), colours[round(shade * 255)] + (255,))

    for i, (sx, sy) in enumerate(SPECKLE_SPOTS):
        if not speckles:
            break
        px, py = round(centre_x + sx), round(cy + sy)
        if 0 <= px < 16 and 0 <= py < 16:
            out.putpixel((px, py), speckles[i % len(speckles)] + (255,))
    return out


NETHER_PIECES = (
    ((0x5A, 0x16, 0x1E), (0x9E, 0x36, 0x2C)),   # crimson stem
    ((0x22, 0x58, 0x54), (0x4E, 0x96, 0x8C)),   # warped stem
    ((0x8A, 0x3A, 0x12), (0xD8, 0x7E, 0x2E)),   # nether wart
)


def crumb(bread):
    """The inside of the loaf: everything the rim and the outline are not.

    A loaf has no bowl to sit in, so its own edge is the only thing holding its shape. Scattering
    over the whole silhouette put pieces on the outline, and a piece on the outline is a bite out
    of it - the flecks stopped being things baked into bread and became confetti dropped on top.
    Vanilla's bread draws its rim a clear step darker than its body, so the body is what is left
    above that step.
    """
    px = bread.load()
    mask = Image.new("L", (16, 16))
    for x in range(16):
        for y in range(16):
            r, g, b, a = px[x, y]
            if a and 0.3 * r + 0.6 * g + 0.1 * b >= 100:
                mask.putpixel((x, y), 255)
    return mask


def chunky(image, mask, pairs, seed, density=4):
    """Lumps in the bowl, for a dish that is pieces rather than a liquid.

    Placed at random from a fixed seed, not by a formula over the coordinates. Every arithmetic
    scatter I tried - every third pixel, then {@code (x * 7 + y * 5) % 4} - laid down a regular
    diagonal, because that is what modular arithmetic on a grid does. It read as stripes or as
    dithering; it never read as food.

    Each lump is two pixels across with a shadow under it, because a single lit pixel is a
    speckle and what these dishes want is something you could get on a spoon.
    """
    out = image.copy()
    points = sorted((x, y) for x in range(16) for y in range(16) if mask.getpixel((x, y)))
    rng = random.Random(seed)

    # A pair per ingredient, cycled: a chili with crimson in it and nothing warped is a chili
    # missing half its recipe, and the bowl is the only place that shows what went in.
    for i, (x, y) in enumerate(rng.sample(points, max(3, len(points) // density))):
        chunk_dark, chunk_light = pairs[i % len(pairs)]
        out.putpixel((x, y), chunk_light + (255,))
        if (x + 1, y) in points:
            out.putpixel((x + 1, y), chunk_light + (255,))
        if (x, y + 1) in points:
            out.putpixel((x, y + 1), chunk_dark + (255,))
    return out


def speckle(image, colours, seed, density=7):
    """Scatter a few flecks through whatever is already opaque, for a thing made of several."""
    out = image.copy()
    points = sorted((x, y) for x in range(16) for y in range(16)
                    if image.getpixel((x, y))[3] > 0)
    rng = random.Random(seed)

    for i, (x, y) in enumerate(rng.sample(points, max(2, len(points) // density))):
        out.putpixel((x, y), colours[i % len(colours)] + (255,))
    return out


def filled_bucket(bucket, milk_bucket, dark, light, craggy=False, seed=0,
                  holes=True, vent=None, vent_width=1):
    """A bucket of something, made the way the milk one is: the same pail, a different fill.

    The fill is wherever the milk bucket differs from an empty one, so the pail keeps every pixel
    of vanilla's and only what is inside it changes.

    Cheese heaps. Milk sits flat in the top of the pail - two pixels of it - and holes punched in
    a two-pixel band do not read as holes, they read as a band broken into three pieces, which is
    what the first attempt looked like: a yellow bow tied to a bucket. So cheese mounds up out of
    the opening instead, and the holes go into the mound where there is room for them to be holes.
    """
    out = bucket.copy()
    colours = ramp(dark, light)
    fill = [(x, y) for x in range(16) for y in range(16)
            if milk_bucket.getpixel((x, y))[3] and
            milk_bucket.getpixel((x, y))[:3] != bucket.getpixel((x, y))[:3]]
    if not fill:
        return out

    left = min(x for x, _ in fill)
    right = max(x for x, _ in fill)
    surface = min(y for _, y in fill)

    if not craggy:
        bottom = max(y for _, y in fill)
        span = max(bottom - surface, 1)
        for x, y in fill:
            shade = 1.0 - (y - surface) / span * 0.55
            out.putpixel((x, y), colours[round(max(0.0, min(1.0, shade)) * 255)] + (255,))
        return out

    # A low mound over the pail's mouth: wide and flat, unlike an ice cream's ball.
    centre = (left + right) / 2 + 0.5
    half = (right - left) / 2 + 1.0
    heaped = []
    for x in range(16):
        for y in range(16):
            dx = (x + 0.5 - centre) / half
            dy = (y + 0.5 - (surface + 0.5)) / 3.2
            if dx * dx + dy * dy > 1.0 or y > surface + 1:
                continue
            shade = max(0.0, min(1.0, 0.62 - dx * 0.26 - dy * 0.34))
            out.putpixel((x, y), colours[round(shade * 255)] + (255,))
            heaped.append((x, y))

    for x, y in fill:
        out.putpixel((x, y), colours[round(0.55 * 255)] + (255,))
        heaped.append((x, y))

    # A pie crust mounds the same way but is not full of holes, so the mound and the holes are
    # separate questions.
    if not holes:
        # Cut a vent across the crown so the fruit shows. Without it three baked pies are three
        # identical domes of pastry, which is exactly what the first attempt produced - the shape
        # said "pie" and nothing at all said "which".
        if vent:
            juice = ramp(*vent)
            crown = min(y for _, y in heaped)
            band = sorted(x for x, y in heaped if y <= crown + 1)
            if band:
                middle = band[len(band) // 2]
                for dx in range(-vent_width, vent_width + 1):
                    for dy in (0, 1):
                        if (middle + dx, crown + dy) in heaped:
                            out.putpixel((middle + dx, crown + dy),
                                         juice[210 if dy == 0 else 150] + (255,))
        return out

    # Holes, each with a lit rim on its far side so it reads as a pocket rather than a smudge.
    rng = random.Random(seed)
    for x, y in rng.sample(heaped, max(2, len(heaped) // 6)):
        out.putpixel((x, y), colours[round(0.06 * 255)] + (255,))
        if (x + 1, y) in heaped:
            out.putpixel((x + 1, y), colours[248] + (255,))


    return out


# A fixed scatter, so a regenerated texture is the same texture.
SPECKLE_SPOTS = ((-2, -1), (1, -2), (2, 1), (-1, 2), (0, 0), (-3, 1))


def cheese_wedge(body, lit, shade):
    """A wedge of cheese: a rind-lit top, a shaded cut face, and a few holes."""
    image = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    draw = ImageDraw.Draw(image)

    # A triangle read from the side, sitting on the bottom of the frame.
    draw.polygon([(2, 12), (13, 12), (13, 5)], fill=body)
    draw.polygon([(2, 12), (13, 5), (13, 4), (3, 11)], fill=lit)
    draw.line([(2, 12), (13, 12)], fill=shade)
    draw.line([(13, 4), (13, 12)], fill=shade)

    for hole in ((6, 10), (9, 8), (11, 10)):
        draw.point(hole, fill=shade)
    return image


def cheese_face(dark, light, seed, holes, cut):
    """One face of a cheese wheel.

    Cheese is a smooth solid with holes in it, so the body barely varies and the holes do all the
    work. The first attempt shaded every pixel independently across the whole ramp, which is what
    sand looks like: the holes vanished into the noise and the rind and the cut face became the
    same picture.

    Two kinds of face. A rind is a skin - darker, close to even, barely holed. A cut face is the
    inside, so it is paler and the holes are the point.
    """
    rng = random.Random(seed)
    shades = ramp(dark, light)
    body = 210 if cut else 150

    image = Image.new("RGBA", (16, 16))
    for x in range(16):
        for y in range(16):
            image.putpixel((x, y), shades[body] + (255,))

    # Gentle blotching in patches rather than per pixel, so it reads as an uneven surface and not
    # as grain.
    for _ in range(10):
        bx, by = rng.randrange(15), rng.randrange(15)
        shade = shades[max(0, min(255, body + rng.choice((-26, -14, 14, 26))))]
        for dx in range(rng.randint(2, 4)):
            for dy in range(rng.randint(2, 3)):
                if bx + dx < 16 and by + dy < 16:
                    image.putpixel((bx + dx, by + dy), shade + (255,))

    # Holes are square, the way a Minecraft Swiss would be: a dark mouth with its top edge deeper
    # in shadow and a lit lip under it, so a flat square reads as a pit and not as a stain. The
    # round-ish blobs the first pass drew smeared into the blotching; a square with a straight
    # shadow line does not.
    deep = shades[max(0, body - 150)]
    mouth, lip = shades[max(0, body - 110)], shades[min(255, body + 45)]
    taken = []
    for _ in range(holes):
        for _try in range(40):
            size = rng.choice((2, 2, 3)) if cut else 2
            cx, cy = rng.randrange(1, 15 - size), rng.randrange(1, 14 - size)
            if all(cx + size < ox or ox + os_ < cx or cy + size + 1 < oy or oy + os_ + 1 < cy
                   for ox, oy, os_ in taken):
                taken.append((cx, cy, size))
                break
        else:
            continue
        for dx in range(size):
            for dy in range(size):
                image.putpixel((cx + dx, cy + dy), (deep if dy == 0 else mouth) + (255,))
            image.putpixel((cx + dx, cy + size), lip + (255,))
    return image


def stencil(rows, palette):
    """Draw a sprite from a picture of it.

    Written out pixel by pixel rather than assembled from ellipses and rectangles. The drawn
    shapes came out looking like a diagram of a sandwich - perfectly straight, perfectly
    symmetrical, every edge the same - and vanilla is none of those things. A grid you can read is
    also a grid you can nudge one pixel at a time, which is how this sort of art actually gets
    made.
    """
    image = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(rows):
        for x, key in enumerate(row):
            if key in palette:
                image.putpixel((x, y), palette[key] + (255,))
    return image


# Sliced bread, not a bun. The top face is skewed rather than tapered: a shape that narrows as it
# rises is a dome, and a dome on top of a filling is a burger no matter what is in it. Square
# corners and a flat top are what say "slice", and sliding each row sideways is what makes the
# slice lie down in space instead of standing up flat.
#
# Every row is filled edge to edge. Leaving gaps in the salad to suggest separate leaves punched
# actual holes through the middle of the sandwich, which is a different thing entirely - the
# leaves are told apart by a darker green between them instead.
#
# The right side runs the full height of the stack and each layer shades its own bit of it. A side
# face that stopped a row short left the bottom corner open, which reads as a bite taken out of
# the sandwich rather than as a corner.
SANDWICH = (
    "................",
    "................",
    ".....TTTTTTTTT..",
    "....TTTTTTTTTT..",
    "...TTTTTTTTTTT..",
    "...cccccccccdd..",
    "...mmmmmmmmmee..",
    "...MMMMMMMMMee..",
    "...gGgggGgggff..",
    "...BBBBBBBBBdd..",
    "...CCCCCCCCCdd..",
    "...bbbbbbbbbdd..",
    "................",
    "................",
    "................",
    "................",
)

# A roll stood on its end and seen from slightly above: the cut face is an ellipse rather than a
# circle, and a short shaded wall below it makes it a cylinder. No rice - just the fish and the
# seaweed holding it.
#
# Both halves have to actually curve. A wall in one flat green read as a fish sitting on a box,
# and widening every row of the top to the same span made the fish a rectangle - an ellipse drawn
# with equal rows is just a square with the corners still on. Rows that narrow toward the top, and
# a wall lit down its near side, are the whole difference between a cylinder and a slab.
SUSHI = (
    "................",
    "................",
    ".....nnnnnn.....",
    "...nnFFFFFFnn...",
    "..nFFFFFFFFFFn..",
    "..nFFFFFFFFFFn..",
    "..nnffffffffnn..",
    "..nmMHHHMMmmnn..",
    "..nmMHHHMMmmnn..",
    "..nmMHHHMMmmnn..",
    "...nmMMMMmmnn...",
    "................",
    "................",
    "................",
    "................",
    "................",
)


# Where the model punches vents through a pie lid, in top-texture pixels: one in the middle and one
# towards each corner. generate_models.py cuts the same squares out of the lid geometry.
PIE_VENTS = [(x + dx, y + dy) for x, y in ((7, 7), (3, 3), (11, 3), (3, 11), (11, 11))
             for dx in (0, 1) for dy in (0, 1)]


def pie_face(crust, fill, seed, face):
    """One face of a baked pie.

    Crust everywhere but the cut, which is where the filling is - the same rind-and-inside split
    the cheese wheel uses, because it is the same question: what does a slice expose?
    """
    rng = random.Random(seed)
    cut = face == "inner"
    dark, light = (fill if cut else crust)
    shades = ramp(dark, light)
    body = 190 if cut else (150 if face == "top" else 120)
    if face == "fill":
        dark, light = fill
        shades = ramp(dark, light)
        body = 170

    image = Image.new("RGBA", (16, 16))
    for x in range(16):
        for y in range(16):
            image.putpixel((x, y), shades[body] + (255,))

    for _ in range(9):
        bx, by = rng.randrange(15), rng.randrange(15)
        shade = shades[max(0, min(255, body + rng.choice((-30, -16, 16, 30))))]
        for dx in range(rng.randint(2, 4)):
            for dy in range(rng.randint(2, 3)):
                if bx + dx < 16 and by + dy < 16:
                    image.putpixel((bx + dx, by + dy), shade + (255,))

    if face == "fill":
        # The filling under the lid, seen through the vents. Paler and busier than the cut face
        # so a berry or two shows where the crust was pierced.
        berries = ramp(*fill)
        for _ in range(6):
            bx, by = rng.randrange(2, 13), rng.randrange(2, 13)
            for dx, dy in ((0, 0), (1, 0), (0, 1)):
                image.putpixel((bx + dx, by + dy), berries[120] + (255,))

    if face == "top":
        # The lid is the middle twelve pixels; the ring around it is the rolled edge of the crust,
        # which the model raises above the lid. Lit on the outside and shadowed inside, so the
        # rim reads as a lip even in the flat sprite.
        for i in range(1, 15):
            for x, y in ((i, 1), (1, i)):
                image.putpixel((x, y), shades[min(255, body + 60)] + (255,))
            for x, y in ((i, 14), (14, i)):
                image.putpixel((x, y), shades[max(0, body - 50)] + (255,))
        # The vents the model cuts through the lid. The pixels underneath are never drawn, but a
        # sprite that shows them as filling stays honest when the texture is looked at on its own.
        berries = ramp(*fill)
        for x, y in PIE_VENTS:
            image.putpixel((x, y), berries[160] + (255,))

    # The cut face: crust where the lid and lip were, filling below. Rows eight and nine are the
    # two pixels of lid the model leaves at the top of a slice, so they are the ones that show.
    if cut:
        # Well down the crust ramp: a mid-crust brown sits at almost exactly pumpkin's value and
        # the band disappeared into the filling entirely.
        edge = ramp(*crust)
        for x in range(16):
            image.putpixel((x, 8), edge[40] + (255,))
            image.putpixel((x, 9), edge[70] + (255,))
    return image


def caramel_apple(apple, dark, light, stick_dark, stick_light):
    """Vanilla's apple under a coat of caramel, hanging off a stick.

    Turned over, because that is the way one is held: the stick goes in where the stem was and you
    grip it underneath, so the fruit sits above your hand with its stem end pointing down. Drawn
    the right way up with a stick poking out of the top, it is an apple with a twig in it.

    The apple keeps its own shading through the recolour, so the caramel reads as something poured
    over a familiar shape rather than a new fruit.
    """
    coated = recolour(apple, dark, light).transpose(Image.FLIP_TOP_BOTTOM)

    # Lifted to leave room for a stick long enough to be a handle rather than a stub. Vanilla's
    # apple is fourteen rows tall, so two is the most that can come off before the fruit starts
    # losing shape rather than just its bottom curve - three clipped it flat against the frame.
    image = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    image.paste(coated, (0, -2), coated)

    px = image.load()
    bottom = max(y for x in range(16) for y in range(16) if px[x, y][3])

    draw = ImageDraw.Draw(image)
    draw.rectangle([7, bottom - 1, 8, 15], fill=stick_dark)
    draw.line([(7, bottom - 1), (7, 15)], fill=stick_light)
    return image


def boiled_egg(egg):
    """An egg shelled and boiled: the same egg, gone white all through.

    No yolk. A boiled egg still in one piece does not show one, and drawing a cut face on an
    item you hold whole was inventing a detail to solve a legibility problem that the colour
    already solves.
    """
    return recolour(egg, (0xC4, 0xC2, 0xBA), (0xFF, 0xFF, 0xFF))


# Dishes that are pieces rather than liquid: the lumps and their shadow.
# Dishes that are pieces rather than liquid: the lump's shadow, its lit face, and the seed that
# scatters it. The seed is fixed so the texture is the same every time it is generated.
CHUNKY = {
    "potato_chowder": ((((0x8A, 0x76, 0x46), (0xF8, 0xF0, 0xD4)),), 11),
    "fish_chowder":   ((((0xA8, 0x5A, 0x4E), (0xF2, 0xA8, 0x8C)),), 23),
    "fruit_salad":    ((((0x6A, 0x14, 0x28), (0xFF, 0xC8, 0x58)),
                        ((0x1E, 0x5A, 0x28), (0x8C, 0xD8, 0x6A))), 7),
    "bread_pudding":  ((((0x6A, 0x42, 0x1E), (0xF0, 0xC8, 0x84)),), 31),
    # Wart, crimson fungus and warped fungus all went in the pot, so all three come back out.
    "nether_chili":   ((((0x4E, 0x0E, 0x14), (0xE8, 0x6A, 0x3C)),
                        ((0x14, 0x4E, 0x50), (0x3C, 0xC8, 0xC0)),
                        ((0x6A, 0x0E, 0x22), (0xC8, 0x2E, 0x44))), 5),
}

# Ice creams: the ball's shadow and highlight, and whatever was stirred through it.
SCOOPS = {
    "ice_cream":        ((0x9A, 0xA2, 0xB8), (0xFF, 0xFF, 0xFF), ()),
    "berry_ice_cream":  ((0x8A, 0x16, 0x2A), (0xF2, 0x6E, 0x84),
                         ((0x54, 0x06, 0x14), (0x9E, 0x10, 0x24))),
    "glow_ice_cream":   ((0xB0, 0xA0, 0x70), (0xFF, 0xF6, 0xD8),
                         ((0xFF, 0xA8, 0x14), (0xFF, 0xD0, 0x3A))),
    "melon_ice_cream":  ((0xA8, 0x44, 0x58), (0xFF, 0xB4, 0xBE), ()),
    "cookie_ice_cream": ((0xA8, 0x9C, 0x8A), (0xFF, 0xF8, 0xEE),
                         # Crumbled cookie is biscuit as well as chocolate. Dark specks on their
                         # own read as chocolate chip, which is a different pudding.
                         ((0x3A, 0x22, 0x12), (0xC8, 0x9A, 0x5A),
                          (0x4E, 0x2E, 0x18), (0xE0, 0xB8, 0x7A))),
}


def prune(directory, keep):
    """Delete textures this run no longer produces.

    A texture nobody references is harmless in the jar and invisible everywhere else, right up
    until a model still points at one that has quietly stopped being regenerated.
    """
    gone = 0
    for stale in sorted(directory.glob("*.png")):
        if stale.name not in keep:
            stale.unlink()
            gone += 1
    return gone


def main():
    jar = find_jar(sys.argv)
    art = Textures(jar)
    OUT.mkdir(parents=True, exist_ok=True)

    bowl = art.get("bowl")
    stew = art.get("mushroom_stew")
    mask = contents_mask(stew, bowl)

    for name, (dark, light) in BOWLS.items():
        dish = bowl_of(bowl, stew, mask, dark, light)

        if name in CHUNKY:
            dish = chunky(dish, mask, *CHUNKY[name])
        dish.save(OUT / f"{name}.png")

    # Ice cream is a scoop, not a puddle.
    for name, (dark, light, speckles) in SCOOPS.items():
        scoop_of(bowl, mask, dark, light, speckles).save(OUT / f"{name}.png")

    # A drink, made the way vanilla makes drinks.
    overlay, glass = art.get("potion_overlay"), art.get("potion")
    tinted_bottle(overlay, glass, (0x6C, 0xC2, 0x4A)).save(OUT / "cactus_juice.png")

    # Cheese is only ever carried in a bucket - it is made in one and dumped out of one - so the
    # item is a pail of cheese rather than a wedge. The wedge belongs to the block it becomes.
    pail, milk = art.get("bucket"), art.get("milk_bucket")
    filled_bucket(pail, milk, (0xB8, 0x86, 0x14), (0xFF, 0xDE, 0x62),
                  craggy=True, seed=3).save(OUT / "cheese_bucket.png")
    filled_bucket(pail, milk, (0x7E, 0x52, 0x0E), (0xC8, 0x9A, 0x38),
                  craggy=True, seed=3).save(OUT / "smoked_cheese_bucket.png")

    outline(stencil(SANDWICH, {
        "T": (0xF2, 0xCE, 0x8E), "c": (0xC2, 0x8C, 0x44),
        "B": (0xE0, 0xB4, 0x70), "C": (0xC2, 0x8C, 0x44), "b": (0x9A, 0x66, 0x2C),
        "m": (0xC8, 0x5A, 0x40), "M": (0x9A, 0x3C, 0x2C),
        "g": (0x6E, 0xB4, 0x40), "G": (0x3E, 0x74, 0x28),
        # The receding side, one shade down from whatever layer it belongs to.
        "d": (0xA8, 0x76, 0x36), "e": (0x8E, 0x38, 0x2A), "f": (0x46, 0x7E, 0x2C),
    })).save(OUT / "meat_sandwich.png")

    # No outline pass: nori is already the darkest thing here, and darkening its rim again just
    # turns the whole roll into a silhouette.
    stencil(SUSHI, {
        "n": (0x12, 0x22, 0x18), "m": (0x22, 0x3E, 0x28),
        "M": (0x38, 0x64, 0x3E), "H": (0x52, 0x8A, 0x54),
        "f": (0xC4, 0x5C, 0x48), "F": (0xEE, 0x8A, 0x6E),
    }).save(OUT / "sushi.png")

    caramel_apple(art.get("apple"), (0x6E, 0x38, 0x0C), (0xE8, 0xA2, 0x3E),
                  (0x5A, 0x40, 0x22), (0x8A, 0x66, 0x3A)).save(OUT / "caramel_apple.png")

    # The chili cooked down: the bowl burns away and what is left is a loaf, so it is bread's
    # shape in the nether's colours rather than anything of its own.
    bread = art.get("bread")
    loaf = recolour(bread, (0x4A, 0x12, 0x1A), (0xC8, 0x5A, 0x3C))
    chunky(loaf, crumb(bread), NETHER_PIECES, seed=17, density=7)\
        .save(OUT / "nether_loaf.png")

    # Shelled and boiled: vanilla's egg, white all through.
    boiled_egg(art.get("egg")).save(OUT / "hardboiled_egg.png")

    # Beer and wine, twice each. A starter is the same bottle gone cloudy: murky and grey-shifted,
    # so a shelf of them reads at a glance as the ones that are not ready.
    for drink, done, raw in (
            ("beer", (0xC8, 0x8A, 0x18), (0x9A, 0x86, 0x5A)),
            ("apple_wine", (0xE0, 0xC8, 0x4A), (0xA8, 0xA0, 0x74)),
            ("berry_wine", (0x9A, 0x18, 0x3A), (0x7A, 0x50, 0x58)),
            ("glow_berry_wine", (0xF0, 0xB0, 0x2E), (0xB0, 0x96, 0x60)),
            ("melon_wine", (0xD8, 0x5A, 0x6E), (0xA0, 0x78, 0x7A)),
            ("chorus_wine", (0x8A, 0x4A, 0xC8), (0x76, 0x66, 0x8A))):
        tinted_bottle(overlay, glass, done, bubbles=7 if drink == "beer" else 0, seed=4) \
            .save(OUT / f"{drink}.png")
        tinted_bottle(overlay, glass, raw).save(OUT / f"{drink}_starter.png")

    blocks = HERE / "src/main/resources/assets/lets-cook-justfatlard/textures/block"
    blocks.mkdir(parents=True, exist_ok=True)

    # Pies: raw in the tin, baked in the tin, and the four faces of the block it becomes.
    tin = ((0x5E, 0x5E, 0x64), (0x9A, 0x9A, 0xA2))
    raw_crust = ((0xB0, 0xA2, 0x7A), (0xE8, 0xDC, 0xB4))
    crust = ((0xA8, 0x72, 0x2E), (0xE8, 0xBE, 0x76))
    fillings = {
        "pumpkin": ((0xB0, 0x5A, 0x0E), (0xF0, 0xA0, 0x3C)),
        "sweet_berry": ((0x7A, 0x12, 0x26), (0xD8, 0x3A, 0x4E)),
        "glow_berry": ((0xC0, 0x7A, 0x0E), (0xFF, 0xDA, 0x5E)),
    }
    for fruit, fill in fillings.items():
        # Both are a crust in a tin. Raw is pale dough and still shows a lot of what is under it;
        # baking browns the pastry and closes it up, so the finished pie shows less, not more.
        filled_bucket(pail, milk, *raw_crust, craggy=True, holes=False, vent=fill, vent_width=2) \
            .save(OUT / f"raw_{fruit}_pie.png")
        filled_bucket(pail, milk, *crust, craggy=True, holes=False, vent=fill, vent_width=1) \
            .save(OUT / f"whole_{fruit}_pie.png")
        for i, face in enumerate(("top", "side", "bottom", "inner", "fill")):
            pie_face(crust, fill, 40 + i, face).save(blocks / f"{fruit}_pie_{face}.png")

    # Chocolate cake is vanilla's cake through a chocolate ramp: same frosting, same crumb, same
    # everything a player already recognises as cake.
    for face in ("top", "side", "bottom", "inner"):
        recolour(art.block(f"cake_{face}"), (0x2A, 0x16, 0x0E), (0xC8, 0x8E, 0x5C)) \
            .save(blocks / f"chocolate_cake_{face}.png")

    # The wheel the bucket pours out. Cake's four faces, in cheese - and once per vintage, because
    # a wheel that sat for three days should not look like one that sat for one. Aged cheese
    # darkens and dries, so the older skin goes deeper and loses some of its glare.
    for prefix, tiers in (
            ("cheese", (((0xB8, 0x86, 0x14), (0xFF, 0xDE, 0x62)),
                        ((0x96, 0x60, 0x0E), (0xE4, 0xB2, 0x38)))),
            ("smoked_cheese", (((0x7E, 0x52, 0x0E), (0xC8, 0x9A, 0x38)),
                               ((0x5C, 0x38, 0x0A), (0x9E, 0x72, 0x26))))):
        for tier, (dark, light) in enumerate(tiers):
            for face, seed, holes, cut in (("top", 5, 3, False), ("side", 6, 2, False),
                                           ("bottom", 7, 1, False), ("inner", 8, 7, True)):
                cheese_face(dark, light, seed, holes, cut).save(
                    blocks / f"{prefix}_v{tier}_{face}.png")

    faces = {f"{prefix}_v{tier}_{face}.png"
             for prefix in ("cheese", "smoked_cheese") for tier in range(2)
             for face in ("top", "side", "bottom", "inner")}
    faces |= {f"{fruit}_pie_{face}.png" for fruit in fillings
              for face in ("top", "side", "bottom", "inner", "fill")}
    faces |= {f"chocolate_cake_{face}.png" for face in ("top", "side", "bottom", "inner")}
    gone = prune(blocks, faces)

    print(f"  {len(BOWLS)} bowls, 1 bottle, 2 cheeses, 1 egg -> {OUT.relative_to(HERE)}")
    print(f"  {len(list(blocks.glob('*.png')))} block faces -> {blocks.relative_to(HERE)}"
          + (f" ({gone} stale removed)" if gone else ""))


if __name__ == "__main__":
    main()
