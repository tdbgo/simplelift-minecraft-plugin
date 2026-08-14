package com.playcity.simplelift.config;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;

public final class LiftSettings {

    private final boolean debug;
    private final int maxLiftArea;
    private final int maxHeight;
    private final int minHeight;
    private final boolean autoPlace;
    private final boolean preventEntry;
    private final boolean preventLeave;
    private final boolean liftMobs;
    private final boolean mouseScroll;
    private final boolean preserveSelectionHotbar;
    private final boolean showSelectionActionBar;
    private final int selectionTimeoutSeconds;
    private final int selectionFastStep;
    private final boolean confirmSelectionWithSwapKey;
    private final int secondsUntilTimeout;
    private final boolean redstone;
    private final boolean soundEnabled;
    private final int soundVolume;
    private final Map<Material, Double> baseSpeeds;
    private final Set<Material> floorMaterials;
    private final Set<Material> buttonMaterials;
    private final Set<Material> signMaterials;
    private final Set<Material> shaftMaterials;
    private final Set<Material> musicMaterials;
    private final LiftMessages messages;

    public LiftSettings(boolean debug, int maxLiftArea, int maxHeight, int minHeight,
            boolean autoPlace, boolean preventEntry, boolean preventLeave, boolean liftMobs,
            boolean mouseScroll, boolean preserveSelectionHotbar, boolean showSelectionActionBar,
            int selectionTimeoutSeconds, int selectionFastStep, boolean confirmSelectionWithSwapKey,
            int secondsUntilTimeout, boolean redstone, boolean soundEnabled, int soundVolume,
            Map<Material, Double> baseSpeeds, Set<Material> floorMaterials,
            Set<Material> buttonMaterials, Set<Material> signMaterials, Set<Material> shaftMaterials,
            Set<Material> musicMaterials, LiftMessages messages) {
        this.debug = debug;
        this.maxLiftArea = maxLiftArea;
        this.maxHeight = maxHeight;
        this.minHeight = minHeight;
        this.autoPlace = autoPlace;
        this.preventEntry = preventEntry;
        this.preventLeave = preventLeave;
        this.liftMobs = liftMobs;
        this.mouseScroll = mouseScroll;
        this.preserveSelectionHotbar = preserveSelectionHotbar;
        this.showSelectionActionBar = showSelectionActionBar;
        this.selectionTimeoutSeconds = selectionTimeoutSeconds;
        this.selectionFastStep = selectionFastStep;
        this.confirmSelectionWithSwapKey = confirmSelectionWithSwapKey;
        this.secondsUntilTimeout = secondsUntilTimeout;
        this.redstone = redstone;
        this.soundEnabled = soundEnabled;
        this.soundVolume = soundVolume;
        this.baseSpeeds = Collections.unmodifiableMap(new LinkedHashMap<Material, Double>(baseSpeeds));
        this.floorMaterials = immutableSet(floorMaterials);
        this.buttonMaterials = immutableSet(buttonMaterials);
        this.signMaterials = immutableSet(signMaterials);
        this.shaftMaterials = immutableSet(shaftMaterials);
        this.musicMaterials = immutableSet(musicMaterials);
        this.messages = messages;
    }

    public boolean debug() { return debug; }
    public int maxLiftArea() { return maxLiftArea; }
    public int maxHeight() { return maxHeight; }
    public int minHeight() { return minHeight; }
    public boolean autoPlace() { return autoPlace; }
    public boolean preventEntry() { return preventEntry; }
    public boolean preventLeave() { return preventLeave; }
    public boolean liftMobs() { return liftMobs; }
    public boolean mouseScroll() { return mouseScroll; }
    public boolean preserveSelectionHotbar() { return preserveSelectionHotbar; }
    public boolean showSelectionActionBar() { return showSelectionActionBar; }
    public int selectionTimeoutSeconds() { return selectionTimeoutSeconds; }
    public int selectionFastStep() { return selectionFastStep; }
    public boolean confirmSelectionWithSwapKey() { return confirmSelectionWithSwapKey; }
    public int secondsUntilTimeout() { return secondsUntilTimeout; }
    public boolean redstone() { return redstone; }
    public boolean soundEnabled() { return soundEnabled; }
    public int soundVolume() { return soundVolume; }
    public Map<Material, Double> baseSpeeds() { return baseSpeeds; }
    public Set<Material> floorMaterials() { return floorMaterials; }
    public Set<Material> buttonMaterials() { return buttonMaterials; }
    public Set<Material> signMaterials() { return signMaterials; }
    public Set<Material> shaftMaterials() { return shaftMaterials; }
    public Set<Material> musicMaterials() { return musicMaterials; }
    public LiftMessages messages() { return messages; }

    public boolean isBase(Block block) {
        return baseSpeeds.containsKey(block.getType());
    }

    public boolean isFloor(Block block) {
        return floorMaterials.contains(block.getType());
    }

    public boolean isButton(Block block) {
        return buttonMaterials.contains(block.getType());
    }

    public boolean isSign(Block block) {
        return signMaterials.contains(block.getType()) && block.getState() instanceof Sign;
    }

    public boolean isAllowedShaftBlock(Block block) {
        Material material = block.getType();
        return !material.isSolid()
                || floorMaterials.contains(material)
                || buttonMaterials.contains(material)
                || signMaterials.contains(material)
                || shaftMaterials.contains(material);
    }

    public double speedOf(Block base) {
        Double speed = baseSpeeds.get(base.getType());
        if (speed == null) {
            throw new IllegalArgumentException("No speed configured for " + base.getType());
        }
        return speed.doubleValue();
    }

    public float relativeVolume(double volume) {
        return (float) (volume * soundVolume / 100.0D);
    }

    private static <T> Set<T> immutableSet(Set<T> source) {
        return Collections.unmodifiableSet(new LinkedHashSet<T>(source));
    }
}
