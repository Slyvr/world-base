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
 * The JSON format is:
 * <pre>
 * {
 *   "spritesheet": "base_out_atlas.png",
 *   "spriteSize": 32,
 *   "columns": 32,
 *   "rows": 32,
 *   "sprites": [
 *     { "id": 0, "row": 0, "col": 0, "x": 0, "y": 0, "title": "", "description": "", "tags": [], "constraints": [] },
 *     ...
 *   ]
 * }
 * </pre>
 */
public class SpriteSheetLoader {

    private final Texture texture;
    private final Map<Integer, SpriteData> spriteDataMap;

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

        SpriteData(int id, int row, int col, int x, int y, String title, String description, String[] tags, String[] constraints) {
            this.id = id;
            this.row = row;
            this.col = col;
            this.x = x;
            this.y = y;
            this.title = title;
            this.description = description;
            this.tags = tags;
            this.constraints = constraints;
        }
    }

    /**
     * Load a spritesheet from the given asset path (relative to assets/).
     *
     * @param pngPath  path to the PNG file, e.g. "spritesheets/base_out_atlas.png"
     * @param jsonPath path to the JSON metadata file, e.g. "spritesheets/base_out_atlas.json"
     */
    public SpriteSheetLoader(String pngPath, String jsonPath) {
        this.texture = new Texture(pngPath);

        // Use Gdx.files to read the JSON, then parse manually with JsonReader
        FileHandle jsonFile = Gdx.files.internal(jsonPath);
        this.spriteDataMap = parseJson(jsonFile);
    }

    private Map<Integer, SpriteData> parseJson(FileHandle jsonFile) {
        Map<Integer, SpriteData> map = new HashMap<>();

        JsonReader reader = new JsonReader();
        JsonValue root = reader.parse(jsonFile);

        JsonValue sprites = root.get("sprites");
        for (JsonValue s = sprites.child; s != null; s = s.next) {
            int id = s.getInt("id");
            int row = s.getInt("row");
            int col = s.getInt("col");
            int x = s.getInt("x");
            int y = s.getInt("y");
            String title = s.getString("title", "");
            String description = s.getString("description", "");

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

            map.put(id, new SpriteData(id, row, col, x, y, title, description, tags, constraints));
        }

        return map;
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

    /**
     * Get the SpriteData metadata for a given sprite ID.
     */
    public SpriteData getSpriteData(int spriteId) {
        return spriteDataMap.get(spriteId);
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
