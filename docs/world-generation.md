# WorldBase — World Generation Pipeline

## Overview

The world is a tile grid (default 100×100) with two render layers:
- **Z=0** — water background (every cell)
- **Z=1** — grass tiles forming islands

Generation runs once at startup in `WorldScreen.show()`. The pipeline has four stages:

```
SingleIslandGenerator → IslandWorldGenerator → TerrainSmoother → AutoTileSystem
```

Each stage feeds into the next; all entity creation/deletion happens through a shared `EntityManager`.

---

## 1. SingleIslandGenerator

**File:** `engine/systems/SingleIslandGenerator.java`
**Purpose:** Produces one clean, bounded island from noise.

### Steps

1. **Noise + radial falloff** — Uses `SimplexNoise.fbm()` (multi-octave Perlin) combined with a circular falloff from the grid centre. Tiles above a noise threshold become grass.

2. **Smoothing** — Applies the perpendicular-edge survival rule (same algorithm as `TerrainSmoother` Phase 1, see below). Removes thin features and solitary tiles within the generation grid.

3. **Largest-component extraction** — Runs 4-direction BFS to find all connected components of grass. Discards everything except the largest component. This eliminates spurious fragments that the noise might produce.

4. **Bounding-box trim** — Computes the bounding box of the largest component's tiles and copies only those tiles into a compact `SingleIsland` object.

### Output

A `SingleIsland` record containing:
- `boolean[][] tiles` — grass grid in local bounding-box coords
- `int width`, `int height` — bounding box dimensions
- `int tileCount` — number of grass tiles

All tiles in the returned island are guaranteed to be part of a single connected component (4-direction).

---

## 2. IslandWorldGenerator

**File:** `engine/systems/IslandWorldGenerator.java`
**Extends:** `WorldGenerator`
**Purpose:** Creates the water grid and places one or more islands at non-overlapping positions.

### Steps

1. **Water grid** — Creates a water entity (TileType.WATER, sprite 470) at every cell.

2. **Island generation + placement** — For each island (default `ISLAND_COUNT = 2`):
   - Calls `SingleIslandGenerator.generate(seed++, genSize)` to build a clean island
   - **First island** is centred in the world
   - **Subsequent islands** use a candidate-position search (8 fixed positions like corners and quadrants) to find a spot that doesn't overlap any already-placed island's bounding box (with `ISLAND_GAP = 4` tiles of water between them)
   - If no valid position is found, the island is skipped

3. **Entity creation** — Iterates the island's `boolean[][]` grid and creates a grass entity (TileType.GRASS, sprite 118) at each `(ox + lx, oy + ly)` world position.

4. **Small-island cleanup** — Runs a 4-direction BFS over the full world, deletes any grass component with fewer than `MIN_ISLAND_TILES = 5` tiles, and records surviving island metadata as `IslandInfo` objects (id, tileCount, centroid).

### Island data

Exposed via `getIslands()` → `List<IslandInfo>`. Each entry has:
- `id` — sequential identifier
- `tileCount` — number of grass tiles
- `centroidX`, `centroidY` — geometric centre

Also stored in `World.getIslands()` and refreshed after the global `TerrainSmoother` pass via `World.refreshIslands(generator)`.

### Configuration (from GameConfig)

| Key | Default | Description |
|-----|---------|-------------|
| `noiseFrequency` | 0.04 | Perlin noise frequency |
| `noiseOctaves` | 4 | FBM octaves |
| `islandRadiusFraction` | 0.45 | Island radius as fraction of grid half-diagonal |
| `islandThreshold` | 0.0 | Minimum noise+falloff value for grass |

---

## 3. TerrainSmoother

**File:** `engine/systems/TerrainSmoother.java`
**Purpose:** Post-processes the world grid to remove unstable grass tiles. Two independent iterative phases.

### Phase 1 — Perpendicular-Edge Rule

A grass tile survives only if it has grass neighbours in at least **two perpendicular cardinal directions**:
- `(top && left) || (top && right) || (bottom && left) || (bottom && right)`

Opposite-only pairs (`top && bottom`, `left && right`) and single-neighbour tiles are removed.

Runs iteratively until no more tiles are removed in a pass. When a tile is destroyed, its neighbours are rechecked in the next iteration, enabling cascading removal.

### Phase 2 — Opposite-Corner Water Rule

A grass tile is removed if **both tiles in any opposite diagonal pair** are water (or out of bounds):
- `(!tl && !br) || (!tr && !bl)`

The auto-tile system's subset-matching can't correctly handle these patterns — it selects a corner/edge sprite whose constraints happen to be satisfied, but the texture would show water where grass actually exists.

Also runs iteratively until stable.

### Effect on various shapes

| Shape | Input tiles | After smoother |
|-------|-------------|----------------|
| Isolated tile | 1 | 0 |
| 2×2 block | 4 | 0 |
| 3×3 block | 9 | 4 (edge-mid diamond) |
| 4×4 block | 16 | 12 |
| 6×6 block | 36 | 32 |

---

## 4. AutoTileSystem

**File:** `engine/systems/AutoTileSystem.java`
**Purpose:** Assigns the correct edge/corner sprite to each grass tile based on its 8-neighbor pattern.

### How it works

1. **Build grass grid** — Scans all entities for grass tiles.

2. **Initial assignment** — For each grass tile, builds a set of active neighbour positions (e.g. `{tl, tc, tr, ml}`). Iterates the sprite list sorted by constraint count descending. Finds the sprite whose constraint set is a **subset** of the active set. The most specific matching sprite wins.

3. **Iterative validation** — Re-checks each tile's assigned sprite against its current neighbours. If the sprite no longer fits (tile was removed nearby), tries to find a new sprite. If no sprite matches, the tile is destroyed.

### Grass normal sprites

14 sprites with constraint patterns ranging from 0 (isolated) to 8 (full interior):

| Constraint count | Sprites | Purpose |
|------------------|---------|---------|
| 8 | id=118 | Interior (all neighbours grass) |
| 7 | ids 22, 23, 54, 55 | Concave corners (one diagonal missing) |
| 5 | ids 86, 117, 119, 150 | Edges (three in a row missing) |
| 3 | ids 85, 87, 149, 151 | Convex corners (five missing) |
| 0 | id=53 | Isolated tile |

---

## Data Flow Summary

```
GameConfig
    ↓
IslandWorldGenerator(config)
    ├── create water grid (full world)
    ├── SingleIslandGenerator.generate(seed, genSize)
    │     ├── noise → raw grass grid
    │     ├── smooth (perp-edge)
    │     ├── BFS → largest component
    │     └── trim to bounding box → SingleIsland
    ├── place island at (ox, oy) non-overlapping
    ├── repeat for each island
    └── BFS cleanup → remove <5 tile fragments
    ↓
World (stores island metadata in List<IslandInfo>)
    ↓
TerrainSmoother.smooth()
    ├── Phase 1: perp-edge cardinal rule (iterative)
    └── Phase 2: opposite-corner diagonal rule (iterative)
    ↓
IslandWorldGenerator.removeSmallIslands()   [re-run after smoother]
    ↓
World.refreshIslands(generator)
    ↓
AutoTileSystem.autoTileGrass()
    ├── initial sprite assignment (subset matching)
    └── iterative validation → fix or destroy mismatches
    ↓
RenderSystem renders the final world
```
