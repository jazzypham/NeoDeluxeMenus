package com.extendedclip.deluxemenus.inventory;

import com.extendedclip.deluxemenus.DeluxeMenus;
import com.extendedclip.deluxemenus.inventory.hider.InventoryHider;
import com.extendedclip.deluxemenus.inventory.hider.impl.PacketEventsInventoryHider;
import com.extendedclip.deluxemenus.inventory.hider.impl.UnavailableInventoryHider;
import com.github.retrooper.packetevents.PacketEvents;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Hides the player inventory shown underneath menus that use the {@code hide_player_inventory} option.
 * <p>
 * This is purely cosmetic. Menu clicks are already cancelled, and nothing here touches the server side player
 * inventory, so a player's items are never at risk. Requires PacketEvents, and silently does nothing without it.
 */
public class PlayerInventoryHider implements InventoryHider {

    private final static boolean SUPPORTS_PACKET_EVENTS = checkPacketEvents();

    private final InventoryHider hider;

    public PlayerInventoryHider(@NotNull final DeluxeMenus plugin) {
        hider = SUPPORTS_PACKET_EVENTS ? new PacketEventsInventoryHider(plugin) : new UnavailableInventoryHider();
    }

    @Override
    public boolean isAvailable() {
        return hider.isAvailable();
    }

    @Override
    public void register() {
        hider.register();
    }

    @Override
    public void unregister() {
        hider.unregister();
    }

    @Override
    public void hide(@NotNull final Player viewer, final int menuSize) {
        hider.hide(viewer, menuSize);
    }

    @Override
    public void unhide(@NotNull final Player viewer) {
        hider.unhide(viewer);
    }

    private static boolean checkPacketEvents() {
        try {
            Class.forName("com.github.retrooper.packetevents.PacketEvents");
            return isPacketEventsLoaded();
        } catch (final ClassNotFoundException | NoClassDefFoundError ignored) {
            return false;
        }
    }

    /**
     * Kept separate from {@link #checkPacketEvents()} so that resolving the PacketEvents class reference cannot happen
     * before the {@link Class#forName(String)} check above has passed.
     */
    private static boolean isPacketEventsLoaded() {
        return PacketEvents.getAPI() != null && PacketEvents.getAPI().isLoaded();
    }
}
