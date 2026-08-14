package com.playcity.simplelift.listener;

import com.playcity.simplelift.SimpleLiftPlugin;
import com.playcity.simplelift.compat.ActionBars;
import com.playcity.simplelift.compat.ServerCompatibility;
import com.playcity.simplelift.compat.SoundEffects;
import com.playcity.simplelift.config.ConfigLoader;
import com.playcity.simplelift.config.LiftSettings;
import com.playcity.simplelift.lift.BlockPosition;
import com.playcity.simplelift.lift.LiftFloor;
import com.playcity.simplelift.lift.LiftScanner;
import com.playcity.simplelift.lift.LiftSign;
import com.playcity.simplelift.lift.LiftStructure;
import com.playcity.simplelift.lift.LiftUserException;
import com.playcity.simplelift.ride.RideManager;
import com.playcity.simplelift.util.ExclusiveOwnerRegistry;
import com.playcity.simplelift.util.ScrollDirection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.scheduler.BukkitTask;

public final class LiftListener implements Listener {

    private static final double MAX_SELECTION_DISTANCE_SQUARED = 9.0D;

    private final SimpleLiftPlugin plugin;
    private final LiftScanner scanner;
    private final RideManager rides;
    private final Map<UUID, ScrollSelection> scrollSelections = new HashMap<>();
    private final ExclusiveOwnerRegistry<BlockPosition> scrollOwners = new ExclusiveOwnerRegistry<>();
    private final BukkitTask selectionCleanupTask;

    public LiftListener(SimpleLiftPlugin plugin, LiftScanner scanner, RideManager rides) {
        this.plugin = plugin;
        this.scanner = scanner;
        this.rides = rides;
        this.selectionCleanupTask = plugin.getServer().getScheduler()
                .runTaskTimer(plugin, this::expireSelections, 20L, 20L);
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Block clicked = event.getClickedBlock();
        if (clicked == null) {
            return;
        }

        Block buttonBelow = clicked.getRelative(BlockFace.DOWN);
        LiftSettings settings = plugin.settings();
        boolean activeButton = settings.isButton(clicked)
                && rides.isInsideActive(clicked.getLocation());
        boolean activeSign = settings.isSign(clicked)
                && settings.isButton(buttonBelow)
                && rides.isInsideActive(buttonBelow.getLocation());
        if (activeButton || activeSign) {
            event.setCancelled(true);
            playerBusy(event.getPlayer());
            return;
        }
        if (scanner.isValidButtonStructure(buttonBelow)) {
            event.setCancelled(true);
            handleSign(event.getPlayer(), clicked, buttonBelow);
            return;
        }
        if (scanner.isValidButtonStructure(clicked)) {
            handleButton(event.getPlayer(), clicked);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onHeldItemChange(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        ScrollSelection selection = scrollSelections.get(player.getUniqueId());
        if (selection == null) {
            return;
        }
        if (!isSelectionUsable(player, selection)) {
            endSelection(player, plugin.settings().messages().scrollSelectDisabled());
            return;
        }

        LiftSettings settings = plugin.settings();
        if (settings.preserveSelectionHotbar()) {
            event.setCancelled(true);
        }
        try {
            boolean forward = ScrollDirection.isForward(event.getNewSlot(), event.getPreviousSlot());
            int steps = player.isSneaking() ? settings.selectionFastStep() : 1;
            LiftFloor selected = selection.structure()
                    .cycleDestination(selection.sign(), forward, steps);
            selection.touch();
            showSelection(player, selection.structure(), selected);
            SoundEffects.button(player, settings.relativeVolume(1.0D), forward ? 1.8F : 0.7F);
        } catch (LiftUserException exception) {
            endSelection(player, null);
            player.sendMessage(exception.getMessage());
        } catch (RuntimeException exception) {
            endSelection(player, null);
            handleInternalError(player, exception);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onSwapHandItems(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        if (!plugin.settings().confirmSelectionWithSwapKey()
                || !scrollSelections.containsKey(player.getUniqueId())) {
            return;
        }
        event.setCancelled(true);
        confirmSelection(player);
    }

    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        ScrollSelection selection = scrollSelections.get(player.getUniqueId());
        if (selection != null && !isSelectionUsable(player, selection)) {
            endSelection(player, plugin.settings().messages().scrollSelectDisabled());
        }

        LiftSettings settings = plugin.settings();
        if (!settings.preventEntry() || !rides.hasActiveRides() || event.getTo() == null
                || rides.isParticipant(player)) {
            return;
        }
        if (!rides.isInsideActive(event.getFrom()) && rides.isInsideActive(event.getTo())) {
            event.setTo(event.getFrom());
            player.sendMessage(settings.messages().cantEnter());
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onVehicleExit(VehicleExitEvent event) {
        if (!rides.isParticipant(event.getVehicle())) {
            return;
        }
        event.setCancelled(true);
        if (event.getExited() instanceof Player) {
            ((Player) event.getExited()).sendMessage(plugin.settings().messages().unsafe());
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        removeSelection(event.getPlayer().getUniqueId());
        rides.handleQuit(event.getPlayer());
    }

    @EventHandler(ignoreCancelled = true)
    public void onKick(PlayerKickEvent event) {
        removeSelection(event.getPlayer().getUniqueId());
        rides.handleQuit(event.getPlayer());
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        rides.handleJoin(event.getPlayer());
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        endSelection(player, null);
        rides.detach(player);
    }

    @EventHandler
    public void onChangedWorld(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        endSelection(player, null);
        Entity vehicle = player.getVehicle();
        rides.detach(player);
        if (vehicle != null) {
            rides.detach(vehicle);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onWorldUnload(WorldUnloadEvent event) {
        clearSelectionsIn(event.getWorld());
        rides.stopWorld(event.getWorld());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onChunkUnload(ChunkUnloadEvent event) {
        // Early Bukkit releases have no plugin chunk tickets. Cancelling unload only for
        // the few seconds of an active ride keeps redstone/mob lifts reliable
        // even when no player is close enough to hold the chunk loaded.
        if (rides.isActiveChunk(event.getChunk())) {
            ServerCompatibility.cancelChunkUnload(event);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onRedstone(BlockRedstoneEvent event) {
        if (!plugin.settings().redstone() || event.getOldCurrent() > 0 || event.getNewCurrent() <= 0) {
            return;
        }

        Block source = event.getBlock();
        ConfigLoader.debug(plugin.getLogger(), plugin.settings(),
                "Redstone changed at " + source.getLocation() + " from "
                        + event.getOldCurrent() + " to " + event.getNewCurrent());
        for (int xOffset = -2; xOffset <= 2; xOffset++) {
            for (int yOffset = -2; yOffset <= 2; yOffset++) {
                for (int zOffset = -2; zOffset <= 2; zOffset++) {
                    if (Math.abs(xOffset) + Math.abs(yOffset) + Math.abs(zOffset) > 3) {
                        continue;
                    }
                    Block candidate = source.getRelative(xOffset, yOffset, zOffset);
                    if (!scanner.isValidButtonStructure(candidate) || !isPoweredFrom(candidate, source)) {
                        continue;
                    }
                    try {
                        Optional<LiftStructure> scanned = scanner.scan(candidate);
                        if (scanned.isPresent() && startRide(scanned.get())) {
                            clearSelectionsFor(scanned.get());
                        }
                    } catch (LiftUserException exception) {
                        ConfigLoader.debug(plugin.getLogger(), plugin.settings(),
                                "Redstone lift activation skipped: " + exception.getMessage());
                    } catch (RuntimeException exception) {
                        plugin.getLogger().log(Level.SEVERE,
                                "Could not activate redstone lift at " + candidate.getLocation(), exception);
                    }
                }
            }
        }
    }

    public void settingsReloaded() {
        for (UUID playerId : new ArrayList<UUID>(scrollSelections.keySet())) {
            Player player = plugin.getServer().getPlayer(playerId);
            if (player == null) {
                removeSelection(playerId);
            } else {
                endSelection(player, plugin.settings().messages().scrollSelectDisabled());
            }
        }
    }

    public void shutdown() {
        selectionCleanupTask.cancel();
        for (UUID playerId : new ArrayList<UUID>(scrollSelections.keySet())) {
            Player player = plugin.getServer().getPlayer(playerId);
            if (player != null) {
                ActionBars.clear(player);
            }
            removeSelection(playerId);
        }
    }

    private void handleSign(Player player, Block signBlock, Block button) {
        LiftSettings settings = plugin.settings();
        if (!player.hasPermission("lift.change")) {
            player.sendMessage(settings.messages().noPermission());
            return;
        }
        SoundEffects.button(player, settings.relativeVolume(1.0D), 1.0F);

        if (scrollSelections.containsKey(player.getUniqueId())) {
            confirmSelection(player);
            return;
        }

        boolean wasBlank = new LiftSign(signBlock).isBlank();
        try {
            Optional<LiftStructure> scanned = scanner.scan(button);
            if (!scanned.isPresent()) {
                return;
            }
            LiftStructure structure = scanned.get();
            if (wasBlank) {
                return;
            }

            LiftSign sign = structure.initialSign();
            UUID owner = currentOwner(sign.position());
            if (owner != null && !owner.equals(player.getUniqueId())) {
                player.sendMessage(settings.messages().scrollSelectBusy());
                return;
            }
            if (settings.mouseScroll()
                    && player.getInventory().getItemInMainHand().getType() == Material.AIR) {
                ScrollSelection selection = new ScrollSelection(structure, sign);
                scrollSelections.put(player.getUniqueId(), selection);
                if (!scrollOwners.claim(player.getUniqueId(), sign.position())) {
                    scrollSelections.remove(player.getUniqueId());
                    player.sendMessage(settings.messages().scrollSelectBusy());
                    return;
                }
                player.sendMessage(settings.messages().scrollSelectEnabled());
                showSelection(player, structure, structure.selectedDestination(sign));
                return;
            }
            structure.cycleDestination(sign, true);
        } catch (LiftUserException exception) {
            player.sendMessage(exception.getMessage());
        } catch (RuntimeException exception) {
            handleInternalError(player, exception);
        }
    }

    private void handleButton(Player player, Block button) {
        if (!player.hasPermission("lift")) {
            player.sendMessage(plugin.settings().messages().noPermission());
            return;
        }
        confirmSelection(player);
        try {
            Optional<LiftStructure> scanned = scanner.scan(button);
            if (scanned.isPresent() && startRide(scanned.get())) {
                clearSelectionsFor(scanned.get());
            }
        } catch (LiftUserException exception) {
            player.sendMessage(exception.getMessage());
        } catch (RuntimeException exception) {
            handleInternalError(player, exception);
        }
    }

    private boolean startRide(LiftStructure structure) {
        return rides.start(structure);
    }

    private void confirmSelection(Player player) {
        ScrollSelection selection = removeSelection(player.getUniqueId());
        if (selection == null) {
            return;
        }
        ActionBars.clear(player);
        try {
            LiftFloor floor = selection.structure().selectedDestination(selection.sign());
            player.sendMessage(formatFloor(
                    plugin.settings().messages().scrollSelectConfirmed(), selection.structure(), floor));
        } catch (RuntimeException exception) {
            player.sendMessage(plugin.settings().messages().scrollSelectDisabled());
        }
    }

    private void endSelection(Player player, String message) {
        if (removeSelection(player.getUniqueId()) == null) {
            return;
        }
        ActionBars.clear(player);
        if (message != null && !message.isEmpty()) {
            player.sendMessage(message);
        }
    }

    private ScrollSelection removeSelection(UUID playerId) {
        ScrollSelection selection = scrollSelections.remove(playerId);
        if (selection == null) {
            return null;
        }
        scrollOwners.release(playerId);
        return selection;
    }

    private UUID currentOwner(BlockPosition position) {
        UUID owner = scrollOwners.ownerOf(position);
        if (owner != null && !scrollSelections.containsKey(owner)) {
            scrollOwners.release(owner);
            return null;
        }
        return owner;
    }

    private void clearSelectionsFor(LiftStructure structure) {
        for (Map.Entry<UUID, ScrollSelection> entry
                : new ArrayList<Map.Entry<UUID, ScrollSelection>>(scrollSelections.entrySet())) {
            LiftStructure selectedStructure = entry.getValue().structure();
            if (!selectedStructure.world().equals(structure.world())
                    || !selectedStructure.shaftBounds().overlaps(structure.shaftBounds())) {
                continue;
            }
            Player player = plugin.getServer().getPlayer(entry.getKey());
            if (player == null) {
                removeSelection(entry.getKey());
            } else {
                endSelection(player, plugin.settings().messages().scrollSelectDisabled());
            }
        }
    }

    private void clearSelectionsIn(World world) {
        UUID worldId = world.getUID();
        for (Map.Entry<UUID, ScrollSelection> entry
                : new ArrayList<Map.Entry<UUID, ScrollSelection>>(scrollSelections.entrySet())) {
            if (!entry.getValue().sign().position().worldId().equals(worldId)) {
                continue;
            }
            Player player = plugin.getServer().getPlayer(entry.getKey());
            if (player == null) {
                removeSelection(entry.getKey());
            } else {
                endSelection(player, null);
            }
        }
    }

    private void expireSelections() {
        long now = System.currentTimeMillis();
        int timeoutSeconds = plugin.settings().selectionTimeoutSeconds();
        for (Map.Entry<UUID, ScrollSelection> entry
                : new ArrayList<Map.Entry<UUID, ScrollSelection>>(scrollSelections.entrySet())) {
            Player player = plugin.getServer().getPlayer(entry.getKey());
            if (player == null || !player.isOnline()) {
                removeSelection(entry.getKey());
                continue;
            }
            ScrollSelection selection = entry.getValue();
            if (!isSelectionUsable(player, selection)) {
                endSelection(player, plugin.settings().messages().scrollSelectDisabled());
            } else if (timeoutSeconds > 0
                    && now - selection.lastActivityMillis() >= timeoutSeconds * 1000L) {
                endSelection(player, plugin.settings().messages().scrollSelectTimedOut());
            }
        }
    }

    private boolean isSelectionUsable(Player player, ScrollSelection selection) {
        Block signBlock = selection.sign().block();
        Location signLocation = signBlock.getLocation();
        return player.hasPermission("lift.change")
                && plugin.settings().isSign(signBlock)
                && player.getWorld().equals(signLocation.getWorld())
                && player.getLocation().distanceSquared(signLocation) <= MAX_SELECTION_DISTANCE_SQUARED
                && player.getUniqueId().equals(currentOwner(selection.sign().position()));
    }

    private void showSelection(Player player, LiftStructure structure, LiftFloor floor) {
        if (!plugin.settings().showSelectionActionBar()) {
            return;
        }
        ActionBars.send(player,
                formatFloor(plugin.settings().messages().scrollSelection(), structure, floor));
    }

    private static String formatFloor(String template, LiftStructure structure, LiftFloor floor) {
        int index = structure.floors().indexOf(floor) + 1;
        String formatted = template
                .replace("{floor}", Integer.toString(floor.level()))
                .replace("{name}", floor.name().trim())
                .replace("{index}", Integer.toString(index))
                .replace("{total}", Integer.toString(structure.floors().size()));
        while (formatted.contains("  ")) {
            formatted = formatted.replace("  ", " ");
        }
        return formatted.trim();
    }

    private void handleInternalError(Player player, RuntimeException exception) {
        player.sendMessage(plugin.settings().messages().internalError());
        plugin.getLogger().log(Level.SEVERE,
                "Could not process a lift interaction for " + player.getName(), exception);
    }

    private void playerBusy(Player player) {
        player.sendMessage(plugin.settings().messages().busy());
    }

    private static Block supportBlock(Block button) {
        return ServerCompatibility.supportBlock(button);
    }

    private static boolean isPoweredFrom(Block button, Block source) {
        Block support = supportBlock(button);
        int supportDistance = Math.abs(support.getX() - source.getX())
                + Math.abs(support.getY() - source.getY())
                + Math.abs(support.getZ() - source.getZ());
        return button.equals(source)
                || support.equals(source)
                || supportDistance == 1
                || button.isBlockPowered()
                || button.isBlockIndirectlyPowered()
                || support.isBlockPowered()
                || support.isBlockIndirectlyPowered();
    }

    private static final class ScrollSelection {

        private final LiftStructure structure;
        private final LiftSign sign;
        private long lastActivityMillis;

        private ScrollSelection(LiftStructure structure, LiftSign sign) {
            this.structure = structure;
            this.sign = sign;
            touch();
        }

        private LiftStructure structure() {
            return structure;
        }

        private LiftSign sign() {
            return sign;
        }

        private long lastActivityMillis() {
            return lastActivityMillis;
        }

        private void touch() {
            lastActivityMillis = System.currentTimeMillis();
        }
    }
}
