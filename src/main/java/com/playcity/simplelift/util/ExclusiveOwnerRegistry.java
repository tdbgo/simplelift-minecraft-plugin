package com.playcity.simplelift.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class ExclusiveOwnerRegistry<K> {

    private final Map<K, UUID> owners = new HashMap<>();
    private final Map<UUID, K> resources = new HashMap<>();

    public boolean claim(UUID participant, K resource) {
        Objects.requireNonNull(participant, "participant");
        Objects.requireNonNull(resource, "resource");
        UUID owner = ownerOf(resource);
        if (owner != null && !owner.equals(participant)) {
            return false;
        }
        release(participant);
        owners.put(resource, participant);
        resources.put(participant, resource);
        return true;
    }

    public UUID ownerOf(K resource) {
        UUID owner = owners.get(resource);
        if (owner == null) {
            return null;
        }
        if (!resource.equals(resources.get(owner))) {
            owners.remove(resource);
            return null;
        }
        return owner;
    }

    public void release(UUID participant) {
        K resource = resources.remove(participant);
        if (resource != null && participant.equals(owners.get(resource))) {
            owners.remove(resource);
        }
    }

    public void clear() {
        owners.clear();
        resources.clear();
    }
}
