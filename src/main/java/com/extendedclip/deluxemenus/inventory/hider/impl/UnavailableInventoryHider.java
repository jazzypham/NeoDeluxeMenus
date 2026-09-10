package com.extendedclip.deluxemenus.inventory.hider.impl;

import com.extendedclip.deluxemenus.inventory.hider.InventoryHider;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Used when PacketEvents is not installed. The {@code hide_player_inventory} menu option is simply ignored.
 */
public class UnavailableInventoryHider implements InventoryHider {

    @Override
    public boolean isAvailable() {
        return false;
    }

    @Override
    public void register() {
    }

    @Override
    public void unregister() {
    }

    @Override
    public void hide(@NotNull final Player viewer, final int menuSize) {
    }

    @Override
    public void unhide(@NotNull final Player viewer) {
    }
}
