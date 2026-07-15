package com.mycelbot.worldbase.engine.ecs;

import java.util.*;

/**
 * Manages entity creation/destruction and component storage.
 * <p>
 * Each component type is stored in its own map keyed by entity ID,
 * giving O(1) lookup per component type and cache-friendly iteration
 * over all entities that share a component.
 */
public class EntityManager {

    private int nextId = 0;
    private final Set<Integer> activeEntities = new HashSet<>();
    private final Map<Class<? extends Component>, Map<Integer, Component>> stores = new HashMap<>();

    /** Create a new entity and return its ID. */
    public Entity createEntity() {
        int id = nextId++;
        activeEntities.add(id);
        return new Entity(id);
    }

    /** Remove an entity and all its components. */
    public void destroyEntity(Entity entity) {
        activeEntities.remove(entity.getId());
        for (Map<Integer, Component> store : stores.values()) {
            store.remove(entity.getId());
        }
    }

    /** Attach a component to an entity. */
    @SuppressWarnings("unchecked")
    public <T extends Component> void addComponent(Entity entity, T component) {
        Map<Integer, Component> store = stores.computeIfAbsent(component.getClass(), k -> new HashMap<>());
        store.put(entity.getId(), component);
    }

    /** Retrieve a component from an entity, or null if absent. */
    @SuppressWarnings("unchecked")
    public <T extends Component> T getComponent(Entity entity, Class<T> type) {
        Map<Integer, Component> store = stores.get(type);
        if (store == null) return null;
        return (T) store.get(entity.getId());
    }

    /** Check whether an entity has a given component type. */
    public boolean hasComponent(Entity entity, Class<? extends Component> type) {
        Map<Integer, Component> store = stores.get(type);
        return store != null && store.containsKey(entity.getId());
    }

    /** Check whether an entity has ALL the given component types. */
    public boolean hasAllComponents(Entity entity, Set<Class<? extends Component>> types) {
        for (Class<? extends Component> type : types) {
            if (!hasComponent(entity, type)) return false;
        }
        return true;
    }

    /**
     * Return all entities that have ALL of the specified component types.
     * Iterates the smallest component store first for efficiency.
     */
    @SafeVarargs
    public final Collection<Entity> getAllEntitiesWith(Class<? extends Component>... types) {
        if (types.length == 0) return Collections.emptyList();

        // Pick the smallest component store as the base set
        Class<? extends Component> smallest = types[0];
        for (Class<? extends Component> t : types) {
            Map<Integer, Component> s = stores.get(t);
            if (s != null && (stores.get(smallest) == null || s.size() < stores.get(smallest).size())) {
                smallest = t;
            }
        }

        Map<Integer, Component> base = stores.get(smallest);
        if (base == null) return Collections.emptyList();

        Set<Class<? extends Component>> required = new HashSet<>(Arrays.asList(types));
        List<Entity> result = new ArrayList<>();
        for (Integer id : base.keySet()) {
            if (!activeEntities.contains(id)) continue;
            Entity e = new Entity(id);
            if (hasAllComponents(e, required)) {
                result.add(e);
            }
        }
        return result;
    }

    /** Total active entities. */
    public int entityCount() {
        return activeEntities.size();
    }
}
