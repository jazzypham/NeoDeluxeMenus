package com.extendedclip.deluxemenus.menu.options;

/**
 * Where a menu item is drawn: inside the menu itself, or inside the player inventory shown underneath it.
 */
public enum MenuItemLocation {
    /**
     * The menu inventory. This is the default and the only location upstream DeluxeMenus supports.
     */
    TOP,
    /**
     * The player inventory shown underneath the menu. Only available on menus that enable
     * {@code hide_player_inventory}, and drawn purely client side.
     */
    BOTTOM
}
