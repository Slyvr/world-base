# WorldBase

A 2D top-down tile-based world renderer built with **LibGDX 1.13.1** using the `base_out_atlas` spritesheet (32×32 pixel tiles, 32×32 grid, 1024 total sprites).

## Design Architecture: Entity Component System (ECS)

WorldBase follows an **ECS (Entity Component System)** architecture. This is the guiding design principle — every feature, refactor, or addition should reinforce this structure.

### Core Principles

| Layer | Role | Rules |
|---|---|---|
| **Entity** | Identifier | Just an `int` or `long` ID. Zero behavior. |
| **Component** | Data | Pure data bags — fields only, no methods (besides getters/setters). Never extends a base class. |
| **System** | Logic | Operates on entities that have a specific set of components. One responsibility per system. Stateless between ticks where possible. |

### Project Structure (ECS mapping)

```
engine/
├── ecs/                  ← ECS core
│   ├── Entity.java          Simple ID wrapper or int[]
│   ├── Component.java       Marker interface for data bags
│   └── System.java          Abstract base for tick() logic
├── components/           ← Data-only component classes
│   ├── PositionComponent    gridX, gridY
│   ├── RenderComponent     spriteId, layer, visible
│   └── TileComponent       tileType (TileType enum)
├── systems/              ← One class per concern
│   ├── RenderSystem         Reads Position + Render, draws tiles
│   ├── CameraSystem         Input → offset/zoom state
│   └── WorldSystem          Generation, load/save
└── ...existing files...
```

### Current Status

The initial scaffold uses a simpler `World + Tile[][]` layout for quick iteration. As features grow, the plan is:

1. **Extract** the ecs/ core (Entity, Component marker, System base)
2. **Migrate** Tile data → PositionComponent + TileComponent
3. **Split** Renderer → RenderSystem, CameraController → CameraSystem
4. **Retain** the spritesheet loader and utility code as-is

### Controls

| Input | Action |
|---|---|
| Middle-click + drag | Pan |
| Scroll wheel | Zoom (centered on cursor) |
| `gradle desktop:run` | Launch |

### Spritesheet

`assets/spritesheets/base_out_atlas.png` (215 KB, 1024×1024 px)  
Companion JSON: `assets/spritesheets/base_out_atlas.json` — labels 9 named sprites (grass, dirt_light, dirt_dark, stone, sand, lava, water, etc.) with tags and placement constraints.

### Running

```sh
gradle desktop:compileJava          # build only
gradle desktop:run                  # build + launch
```

Requires a display server with OpenGL (X11/Wayland). `GLFW_PLATFORM_UNAVAILABLE` on headless servers is expected.
