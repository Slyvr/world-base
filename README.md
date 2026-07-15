# WorldBase

A 2D top-down tile-based world renderer built with **LibGDX 1.13.1** using the `base_out_atlas` spritesheet (32×32 pixel tiles, 32×32 grid, 1024 total sprites).

## Design Architecture: Entity Component System (ECS)

WorldBase follows an **ECS (Entity Component System)** architecture. Entities are bare IDs, components are pure data bags, and systems hold the logic.

### Project Structure

```
engine/
├── ecs/                          ← ECS core
│   ├── Entity.java                   Bare ID wrapper
│   ├── Component.java                Marker interface
│   └── EntityManager.java            Component stores + multi-type queries
├── components/                   ← Pure data bags
│   ├── PositionComponent            gridX, gridY
│   ├── AppearanceComponent          spriteId, visible
│   ├── TileComponent                TileType enum
│   └── ZComponent                   render layer (0=water, 1=terrain, ...)
├── systems/                      ← Logic
│   ├── RenderSystem                 Queries Position+Appearance+Z, sorts by Z
│   ├── CameraSystem                 Input → pan/zoom state
│   ├── WorldGenerator               Abstract base for generation strategies
│   ├── IslandGenerator              Noise-shaped island (simplex + falloff)
│   ├── TerrainSmoother              Removes thin grass features (≤2 neighbors)
│   └── AutoTileSystem               Matches sprites via neighbor constraints
├── World.java                        Owns EntityManager
└── TileType.java                     Enum (grass, dirt, stone, water, lava...)
```

### World Generation Pipeline

```
IslandGenerator → TerrainSmoother → AutoTileSystem (match + re-tile validation)
```

1. **IslandGenerator** — places water at Z=0 (full grid), grass at Z=1 via simplex noise + radial falloff
2. **TerrainSmoother** — removes isolated singles, pairs, and narrow peninsulas
3. **AutoTileSystem** — assigns edge/corner sprites from `grass_normal` constraint data, then iteratively re-tiles any tiles whose neighbors change during cleanup

### Auto-Tiling

The spritesheet JSON contains 13 `grass_normal`-tagged sprites with 8-direction neighbor constraints. The auto-tiler uses subset matching to find the best sprite per tile, then validates via critical-position checking. Tiles that lose neighbors during cleanup are re-tiled rather than immediately deleted.

| Sprite | Constraints | Pattern |
|--------|-------------|---------|
| id=118 | all 8 | Full interior |
| id=22-55 | 7 | Concave corners |
| id=86,117,119,150 | 5 | Straight edges |
| id=85,87,149,151 | 3 | Convex corners |
| id=53 | 0 | Isolated tile |

### Controls

| Input | Action |
|---|---|
| Middle-click + drag | Pan |
| Scroll wheel | Zoom (centered on cursor) |
| `gradle desktop:run` | Launch |

### Running

```sh
gradle desktop:compileJava          # build only
gradle desktop:run                  # build + launch
```

Requires a display server with OpenGL (X11/Wayland). `GLFW_PLATFORM_UNAVAILABLE` on headless servers is expected.
