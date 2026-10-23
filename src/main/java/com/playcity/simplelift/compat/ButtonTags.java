package com.playcity.simplelift.compat;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;

public final class ButtonTags {
    private ButtonTags() { }

    public static boolean contains(String key, Material material) {
        Tag<Material> tag = Bukkit.getTag("blocks", NamespacedKey.minecraft(key.substring(10)), Material.class);
        if (tag != null) { return tag.isTagged(material); }
        // stone_buttons is absent on older servers. Only known vanilla members
        // are used there; unknown/custom materials retain the configured default.
        return key.equals("minecraft:stone_buttons")
                && (material.name().equals("STONE_BUTTON") || material.name().equals("POLISHED_BLACKSTONE_BUTTON"));
    }
}
