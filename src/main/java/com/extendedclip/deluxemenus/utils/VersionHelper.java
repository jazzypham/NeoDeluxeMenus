package com.extendedclip.deluxemenus.utils;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.event.inventory.InventoryType;

/**
 * Inventory type helpers.
 * <p>
 * This class used to hold server version gates as well. They were removed because the fork requires
 * 1.21.11+, where every gated API exists. The version parsing behind them also broke on the new
 * year-based versions: {@code 26.1} parsed to {@code 2610}, which compared lower than the
 * {@code 1_21_4} constant, so features were silently switched off on 26.x servers.
 *
 * @author Matt from triumph-gui
 */
public final class VersionHelper {

    private static final List<InventoryType> CHEST_INVENTORY_TYPES = List.of(
            InventoryType.BARREL,
            InventoryType.CHEST,
            InventoryType.CRAFTING,
            InventoryType.CREATIVE,
            InventoryType.ENDER_CHEST,
            InventoryType.LECTERN,
            InventoryType.MERCHANT,
            InventoryType.SHULKER_BOX
    );

    private static List<InventoryType> VALID_INVENTORY_TYPES = null;

    public static List<InventoryType> getValidInventoryTypes() {
        if (VALID_INVENTORY_TYPES != null) return VALID_INVENTORY_TYPES;

        final List<InventoryType> validInventoryTypes = new ArrayList<>();

        for (final InventoryType inventoryType : InventoryType.values()) {
            if (inventoryType != InventoryType.CHEST && CHEST_INVENTORY_TYPES.contains(inventoryType)) continue;
            validInventoryTypes.add(inventoryType);
        }

        VALID_INVENTORY_TYPES = validInventoryTypes;
        return VALID_INVENTORY_TYPES;
    }

}
