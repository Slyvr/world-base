package com.mycelbot.worldbase.util;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

        // ── Build category→sprites index from groups AND individual terrainCategory ──
        // Collect unique sprite IDs per category (dedup sprites in both a group and individual)
        Map<String, Set<Integer>> catIds = new HashMap<>();

        // From groups
        JsonValue groups = root.get("groups");
        if (groups != null) {
            for (JsonValue g = groups.child; g != null; g = g.next) {
                String cat = g.getString("terrainCategory", "");
                if (cat.isEmpty()) continue;
                JsonValue cells = g.get("cells");
                if (cells == null) continue;
                Set<Integer> ids = catIds.computeIfAbsent(cat, k -> new HashSet<>());
                for (JsonValue c = cells.child; c != null; c = c.next) {
                    ids.add(c.getInt("row") * 32 + c.getInt("col"));
                }
            }
        }

        // From individual sprites with terrainCategory set
        for (SpriteData sd : spriteDataMap.values()) {
            if (!sd.terrainCategory.isEmpty()) {
                catIds.computeIfAbsent(sd.terrainCategory, k -> new HashSet<>()).add(sd.id);
            }
        }

        // Resolve IDs to SpriteData, sorted by ID for deterministic order
        for (Map.Entry<String, Set<Integer>> entry : catIds.entrySet()) {
            List<SpriteData> catList = new ArrayList<>();
            List<Integer> sorted = new ArrayList<>(entry.getValue());
            sorted.sort(null); // natural order (integer)
            for (int id : sorted) {
                SpriteData sd = spriteDataMap.get(id);
                if (sd != null) catList.add(sd);
            }
            categorySprites.put(entry.getKey(), catList);
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

    /**
     * Return the first sprite whose title matches the given name, or null.
     * Used for looking up baseline tiles (e.g. a sprite titled "grass").
     */
    public SpriteData getSpriteByTitle(String title) {
        for (SpriteData data : spriteDataMap.values()) {
            if (title.equals(data.title)) {
                return data;
            }
        }
        return null;
    }

    public Texture getTexture() {
        return texture;
    }

    public void dispose() {
        texture.dispose();
    }
}
