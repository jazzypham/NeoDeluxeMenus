package com.extendedclip.deluxemenus.inventory.hider;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Hides the player inventory shown underneath an open menu.
 * <p>
 * Implementations must never read, clear or write the server side player inventory. Hiding is a display only concern,
 * clicks inside a menu are already cancelled, so no implementation is allowed to put a player's items at risk.
 */
public interface InventoryHider {

    /**
     * Whether this hider can actually hide anything. When false, every other method is a no-op.
     */
    boolean isAvailable();

    /**
     * Start listening. Called once when the plugin enables.
     */
    void register();

    /**
     * Stop listening and restore the inventory view of every player currently being hidden from. Called once when the
     * plugin disables.
     */
    void unregister();

    /**
     * Start hiding the player inventory for a viewer.
     *
     * @param viewer  the player the menu is being opened for
     * @param menuSize the size of the menu inventory. Every raw slot at or above it belongs to the player inventory.
     */
    void hide(@NotNull Player viewer, int menuSize);

    /**
     * Stop hiding the player inventory for a viewer and restore their inventory view. Safe to call for players that
     * are not being hidden from.
     */
    void unhide(@NotNull Player viewer);
}
