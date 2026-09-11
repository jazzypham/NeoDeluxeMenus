package com.extendedclip.deluxemenus.inventory.hider.impl;

import com.extendedclip.deluxemenus.inventory.hider.InventoryHider;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

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
    public boolean isHidden(@NotNull final Player viewer) {
        return false;
    }

    @Override
    public void hide(@NotNull final Player viewer, final @NotNull Inventory menu, final @NotNull Map<Integer, ItemStack> bottomContents) {
    }

    @Override
    public void updateOverlay(@NotNull final Player viewer, final @NotNull Map<Integer, ItemStack> bottomContents) {
    }

    @Override
    public void unhide(@NotNull final Player viewer) {
    }

    @Override
    public void unhide(@NotNull final Player viewer, final @NotNull Inventory closed) {
    }
}
