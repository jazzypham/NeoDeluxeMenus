package com.extendedclip.deluxemenus.inventory;

/**
 * Slot math for the player inventory shown underneath an open menu.
 * <p>
 * Bottom slots are numbered the way {@link org.bukkit.inventory.PlayerInventory} numbers them: {@code 0-8} is the
 * hotbar and {@code 9-35} is the main storage. That is also the numbering the {@code SET_PLAYER_INVENTORY} packet
 * uses, so no conversion is needed there. Armor and offhand ({@code 36-40}) are never drawn underneath a menu and are
 * out of range here.
 * <p>
 * Raw slots inside an open window are numbered differently: the container's own slots come first, then the 27 storage
 * slots, then the 9 hotbar slots. Vanilla appends the player inventory in that order for every container type, so the
 * conversion below holds regardless of the menu's {@link org.bukkit.event.inventory.InventoryType}.
 */
public final class BottomInventorySlots {

    /**
     * The number of player inventory slots drawn underneath a menu.
     */
    public static final int SIZE = 36;

    /**
     * The number of hotbar slots, which come first in player inventory numbering and last in raw slot numbering.
     */
    public static final int HOTBAR_SIZE = 9;

    private static final int STORAGE_SIZE = SIZE - HOTBAR_SIZE;

    private BottomInventorySlots() {
    }

    /**
     * Whether the given player inventory slot is drawn underneath a menu.
     */
    public static boolean isValid(final int bottomSlot) {
        return bottomSlot >= 0 && bottomSlot < SIZE;
    }

    /**
     * Converts a raw slot inside a window of the given size back into a player inventory slot.
     *
     * @return the player inventory slot, or {@code -1} if the raw slot is not part of the inventory drawn underneath
     * the menu
     */
    public static int fromRawSlot(final int rawSlot, final int menuSize) {
        final int offset = rawSlot - menuSize;
        if (offset < 0 || offset >= SIZE) {
            return -1;
        }

        return offset < STORAGE_SIZE ? offset + HOTBAR_SIZE : offset - STORAGE_SIZE;
    }
}
