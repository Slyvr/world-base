package com.mycelbot.worldbase.util;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads a spritesheet PNG and its associated JSON metadata.
 * Provides TextureRegions indexed by sprite ID.
 * <p>
 * Supports terrainCategory lookup via the {@code groups} array —
 * each group has a {@code terrainCategory} and a {@code cells} list
 * defining which sprites belong to that terrain type.
 */
public class SpriteSheetLoader {

    private final Texture texture;
    private final Map<Integer, SpriteData> spriteDataMap;
    private final Map<String, List<SpriteData>> categorySprites;

    /** Parsed metadata from the JSON file */
    public static class SpriteData {
        public final int id;
        public final int row;
        public final int col;
        public final int x;
        public final int y;
        public final String title;
        public final String description;
        public final String[] tags;
        public final String[] constraints;
        public final String terrainCategory;

        SpriteData(int id, int row, int col, int x, int y,
                   String title, String description,
                   String[] tags, String[] constraints,
                   String terrainCategory) {
            this.id = id;
            this.row = row;
            this.col = col;
            this.x = x;
            this.y = y;
            this.title = title;
            this.description = description;
            this.tags = tags;
            this.constraints = constraints;
            this.terrainCategory = terrainCategory;
        }
    }

    /** A group definition from the JSON — links cells to a terrainCategory. */
    public static class GroupData {
        public final String id;
        public final String terrainCategory;
        public final List<int[]> cells; // each is {row, col}

        GroupData(String id, String terrainCategory, List<int[]> cells) {
            this.id = id;
            this.terrainCategory = terrainCategory;
            this.cells = cells;
        }
    }

    /**
     * Load a spritesheet from the given asset path (relative to assets/).
     *
     * @param pngPath  path to the PNG file, e.g. "spritesheets/terrain.png"
     * @param jsonPath path to the JSON metadata file
     */
    public SpriteSheetLoader(String pngPath, String jsonPath) {
        this.texture = new Texture(pngPath);
        FileHandle jsonFile = Gdx.files.internal(jsonPath);
        this.spriteDataMap = new HashMap<>();
        this.categorySprites = new HashMap<>();
        parseJson(jsonFile);
    }

    private void parseJson(FileHandle jsonFile) {
        JsonReader reader = new JsonReader();
        JsonValue root = reader.parse(jsonFile);

        // ── Parse individual sprites ──
        JsonValue sprites = root.get("sprites");
        for (JsonValue s = sprites.child; s != null; s = s.next) {
            int id      = s.getInt("id");
            int row     = s.getInt("row");
            int col     = s.getInt("col");
            int x       = s.getInt("x");
            int y       = s.getInt("y");
            String title       = s.getString("title", "");
            String description = s.getString("description", "");
            String terrainCat  = s.getString("terrainCategory", "");

            // Parse tags array
            JsonValue tagsArray = s.get("tags");
            String[] tags;
            if (tagsArray != null && tagsArray.size > 0) {
                tags = new String[tagsArray.size];
                int i = 0;
                for (JsonValue t = tagsArray.child; t != null; t = t.next) {
                    tags[i++] = t.asString();
                }
            } else {
                tags = new String[0];
            }

            // Parse constraints array
            JsonValue consArray = s.get("constraints");
            String[] constraints;
            if (consArray != null && consArray.size > 0) {
                constraints = new String[consArray.size];
                int i = 0;
                for (JsonValue c = consArray.child; c != null; c = c.next) {
                    constraints[i++] = c.asString();
                }
            } else {
                constraints = new String[0];
            }

            SpriteData sd = new SpriteData(id, row, col, x, y,
                title, description, tags, constraints, terrainCat);
            spriteDataMap.put(id, sd);
        }

        // ── Parse groups and build category→sprites index ──
        JsonValue groups = root.get("groups");
        if (groups != null) {
            // Collect sprites per category from groups
            // sprites that are members of a group "inherit" that group's terrainCategory
            Map<String, List<int[]>> catCells = new HashMap<>();

            for (JsonValue g = groups.child; g != null; g = g.next) {
                String cat = g.getString("terrainCategory", "");
                if (cat.isEmpty()) continue;

                JsonValue cells = g.get("cells");
                if (cells == null) continue;

                List<int[]> cellList = catCells.computeIfAbsent(cat, k -> new ArrayList<>());
                for (JsonValue c = cells.child; c != null; c = c.next) {
                    cellList.add(new int[]{c.getInt("row"), c.getInt("col")});
                }
            }

            // Resolve cells to SpriteData
            for (Map.Entry<String, List<int[]>> entry : catCells.entrySet()) {
                List<SpriteData> spriteList = new ArrayList<>();
                for (int[] cell : entry.getValue()) {
                    int id = cell[0] * 32 + cell[1]; // row * columns + col
                    SpriteData sd = spriteDataMap.get(id);
                    if (sd != null) {
                        spriteList.add(sd);
                    }
                }
                if (!spriteList.isEmpty()) {
                    categorySprites.put(entry.getKey(), spriteList);
                }
            }
        }

        // Also index individual sprites that have a terrainCategory set on them
        for (SpriteData sd : spriteDataMap.values()) {
            if (!sd.terrainCategory.isEmpty()) {
                categorySprites.computeIfAbsent(sd.terrainCategory, k -> new ArrayList<>()).add(sd);
            }
        }
    }

    /**
     * Get a TextureRegion for the given sprite ID.
     * The spritesheet is 32 columns wide, each sprite is 32x32 pixels.
     */
    public TextureRegion getRegion(int spriteId) {
        SpriteData data = spriteDataMap.get(spriteId);
        if (data == null) return null;
        return new TextureRegion(texture, data.x, data.y, 32, 32);
    }

    /** Get the SpriteData metadata for a given sprite ID. */
    public SpriteData getSpriteData(int spriteId) {
        return spriteDataMap.get(spriteId);
    }

    /**
     * Return all sprites that belong to the given terrain category.
     * This resolves through the {@code groups} array in the JSON:
     * a group's {@code terrainCategory} and its {@code cells} define
     * which sprites belong to that category.
     * <p>
     * Also includes individual sprites whose own {@code terrainCategory}
     * field matches (e.g. the "water" sprites).
     */
    public List<SpriteData> getSpritesByCategory(String category) {
        return categorySprites.getOrDefault(category, List.of());
    }

    /**
     * Return all sprites that have the given tag.
     */
    public List<SpriteData> getSpritesByTag(String tag) {
        List<SpriteData> result = new ArrayList<>();
        for (SpriteData data : spriteDataMap.values()) {
            for (String t : data.tags) {
                if (tag.equals(t)) {
                    result.add(data);
                    break;
                }
            }
        }
        return result;
    }

    public Texture getTexture() {
        return texture;
    }

    public void dispose() {
        texture.dispose();
    }
}
