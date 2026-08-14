package com.playcity.simplelift.ride;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

final class EntityPhysics {

    private final Entity entity;
    private final boolean gravity;
    private final boolean invulnerable;
    private final boolean allowFlight;
    private final boolean flying;

    private EntityPhysics(Entity entity) {
        this.entity = entity;
        this.gravity = entity.hasGravity();
        this.invulnerable = entity.isInvulnerable();
        if (entity instanceof Player) {
            Player player = (Player) entity;
            this.allowFlight = player.getAllowFlight();
            this.flying = player.isFlying();
        } else {
            this.allowFlight = false;
            this.flying = false;
        }
    }

    static EntityPhysics captureAndPrepare(Entity entity) {
        EntityPhysics state = new EntityPhysics(entity);
        entity.setGravity(false);
        entity.setInvulnerable(true);
        entity.setFallDistance(0.0F);
        if (entity instanceof Player) {
            ((Player) entity).setAllowFlight(true);
        }
        return state;
    }

    void restore() {
        if (!entity.isValid()) {
            return;
        }
        entity.setVelocity(new Vector());
        entity.setFallDistance(0.0F);
        entity.setGravity(gravity);
        entity.setInvulnerable(invulnerable);
        if (entity instanceof Player) {
            Player player = (Player) entity;
            if (!allowFlight && player.isFlying()) {
                player.setFlying(false);
            }
            player.setAllowFlight(allowFlight);
            if (allowFlight && flying) {
                player.setFlying(true);
            }
        }
    }
}
