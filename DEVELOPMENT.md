# Let's Cook - Development Guide

For what the mod is and how it plays, see [README.md](README.md).

## Installation

Install server-side alongside its declared dependencies (see `fabric.mod.json`); connecting clients
need only Pandorical. Version targets live in `gradle.properties` (Minecraft, loader, Fabric API)
and `fabric.mod.json` (Java).

## Art

`generate_textures.py` draws the food out of vanilla's own pixels, `generate_models.py` gives every
textured item a model and points the smoked foods at the vanilla look they borrow, and
`generate_icon.py` cuts the mod's icon. Cheese wheels are cake's shape with square Swiss holes in
the skin; pies are built from boxes of their own, a rolled crust standing proud round the edge and
five vents cut through the lid to the filling underneath. All three are deterministic; re-run them after a Minecraft
version bump.
