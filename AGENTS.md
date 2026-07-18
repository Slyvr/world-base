# WorldBase — Agent Guide

## Overview

A 2D top-down tile-based world renderer built with **LibGDX 1.13.1** (Java 17, Gradle 9.x). Uses an ECS (Entity Component System) architecture with a multi-stage world generation pipeline.

## Git Branch

Always work on `dev-build`. Master is production.

## Project Structure

```
engine/
├── ecs/                    ← ECS core (Entity, Component, EntityManager)
├── components/             ← Pure data bags (Position, Appearance, Tile, Z)
├── systems/                ← Logic (Render, Camera, WorldGenerator, etc.)
├── World.java              ← Owns EntityManager
└── TileType.java           ← Enum (grass, dirt, stone, water, lava...)
```

## World Generation Pipeline

```
SingleIslandGenerator → IslandWorldGenerator → TerrainSmoother → AutoTileSystem
```

1. **SingleIslandGenerator** — Simplex noise + radial falloff → grass tiles → smooth (perp-edge rule) → largest component BFS → trim to bounding box → `SingleIsland` record
2. **IslandWorldGenerator** — Creates water grid (Z=0), places islands at non-overlapping positions (ISLAND_GAP=4), then BFS cleanup removes fragments <5 tiles
3. **TerrainSmoother** — Two iterative phases:
   - Phase 1: Perpendicular-edge rule (grass needs neighbours in ≥2 perpendicular cardinal directions)
   - Phase 2: Opposite-corner water rule (removes grass with water in both opposite diagonals)
4. **AutoTileSystem** — Subset matching of sprite constraints → assigns edge/corner sprites → iterative re-validation

### Key Constants (from GameConfig)

| Key | Default | Description |
|-----|---------|-------------|
| `noiseFrequency` | 0.04 | Perlin noise frequency |
| `noiseOctaves` | 4 | FBM octaves |
| `islandRadiusFraction` | 0.45 | Island radius as fraction of grid half-diagonal |
| `islandThreshold` | 0.0 | Minimum noise+falloff value for grass |

## ECS Architecture

- **Entity** — Bare ID wrapper (integer)
- **Component** — Marker interface, pure data bags
- **EntityManager** — Component stores + multi-type queries
- **Systems** — Hold logic, query EntityManager for relevant component combinations

Components used: PositionComponent (gridX, gridY), AppearanceComponent (spriteId, visible), TileComponent (TileType enum), ZComponent (render layer: 0=water, 1=terrain).

## Controls

| Input | Action |
|-------|--------|
| Middle-click + drag | Pan |
| Scroll wheel | Zoom (centered on cursor) |
| `gradle desktop:run` | Build + launch |

## Running

```sh
gradle desktop:compileJava     # build only
gradle desktop:run             # build + launch
```

Requires a display server with OpenGL (X11/Wayland). `GLFW_PLATFORM_UNAVAILABLE` on headless servers is expected.

## Island Metadata

Exposed via `World.getIslands()` → `List<IslandInfo>` with id, tileCount, centroidX/Y.
Refreshed after TerrainSmoother via `World.refreshIslands(generator)`.

## Git Identity

Use `<hermes_profile_name> <sporeaibot@gmail.com>` (e.g. `MycelBot <sporeaibot@gmail.com>`). Load the SSH key with `ssh-add ~/.ssh/sporeAIBot_github` before pushing.
