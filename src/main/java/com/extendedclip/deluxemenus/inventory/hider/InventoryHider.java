package com.extendedclip.deluxemenus.inventory.hider;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Hides the player inventory shown underneath an open menu, optionally drawing menu items over it.
 * <p>
 * Implementations must never read, clear or write the server side player inventory. Both hiding and the overlay are
 * display only concerns, clicks inside a menu are already cancelled, so no implementation is allowed to put a player's
 * items at risk.
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
     * @param viewer   the player the menu is being opened for
     * @param menuSize the size of the menu inventory. Every raw slot at or above it belongs to the player inventory.
     */
    default void hide(@NotNull Player viewer, int menuSize) {
        hide(viewer, menuSize, Map.of());
    }

    /**
     * Start hiding the player inventory for a viewer, drawing the given items over it.
     *
     * @param viewer         the player the menu is being opened for
     * @param menuSize       the size of the menu inventory. Every raw slot at or above it belongs to the player
     *                       inventory.
     * @param bottomContents the items to draw over the hidden inventory, keyed by player inventory slot. These are
     *                       never given to the player, only rendered.
     */
    void hide(@NotNull Player viewer, int menuSize, @NotNull Map<Integer, ItemStack> bottomContents);

    /**
     * Replace the items drawn over the hidden inventory of a viewer. Does nothing for players that are not being
     * hidden from. Callers are responsible for making the client redraw, for example with
     * {@link Player#updateInventory()}.
     *
     * @param bottomContents the items to draw over the hidden inventory, keyed by player inventory slot
     */
    void updateOverlay(@NotNull Player viewer, @NotNull Map<Integer, ItemStack> bottomContents);

    /**
     * Stop hiding the player inventory for a viewer and restore their inventory view. Safe to call for players that
     * are not being hidden from.
     */
    void unhide(@NotNull Player viewer);
}
