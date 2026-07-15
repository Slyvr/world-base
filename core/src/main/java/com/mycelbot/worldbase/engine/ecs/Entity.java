package com.mycelbot.worldbase.engine.ecs;

/**
 * A lightweight entity identifier.
 * Entities are just IDs — all state lives in Components.
 */
public class Entity {
    private final int id;

    public Entity(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Entity)) return false;
        return id == ((Entity) o).id;
    }

    @Override
    public int hashCode() {
        return id;
    }
}
