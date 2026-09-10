package com.extendedclip.deluxemenus.inventory.hider.impl;

import com.extendedclip.deluxemenus.DeluxeMenus;
import com.extendedclip.deluxemenus.inventory.hider.InventoryHider;
import com.extendedclip.deluxemenus.utils.DebugLevel;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerCommon;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetPlayerInventory;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetSlot;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerWindowItems;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Hides the player inventory by blanking it out of the container packets sent to the client.
 * <p>
 * The server side player inventory is never read or modified, so this class cannot lose a player's items. The worst
 * possible failure is a client side desync where the inventory looks empty, which any inventory update or relog
 * repairs. Because outgoing packets are intercepted, every server driven resync (menu refreshes, another plugin giving
 * the player an item, {@link Player#updateInventory()}) stays hidden without any re-sending on our side.
 *
 * @see <a href="https://github.com/retrooper/packetevents">PacketEvents</a>
 */
public class PacketEventsInventoryHider extends PacketListenerAbstract implements InventoryHider {

    /**
     * The window id the client uses for its own inventory screen. Packets for it must never be touched, otherwise the
     * player would also see an empty inventory when opening it with no menu on screen.
     */
    private static final int PLAYER_INVENTORY_WINDOW_ID = 0;

    /**
     * Player inventory slots from this index on (armor and offhand) are never drawn underneath a menu.
     */
    private static final int FIRST_UNSEEN_PLAYER_INVENTORY_SLOT = 36;

    private final DeluxeMenus plugin;

    /**
     * Viewer uuid to the size of the menu they have open. Every raw slot at or above that size belongs to the player
     * inventory. Written from the main thread and read from netty threads, hence the concurrent map.
     */
    private final Map<UUID, Integer> hidden = new ConcurrentHashMap<>();

    private PacketListenerCommon registered = null;

    public PacketEventsInventoryHider(@NotNull final DeluxeMenus plugin) {
        super(PacketListenerPriority.LOW);
        this.plugin = plugin;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public void register() {
        if (registered != null) {
            return;
        }

        registered = PacketEvents.getAPI().getEventManager().registerListener(this);
    }

    @Override
    public void unregister() {
        if (registered != null) {
            PacketEvents.getAPI().getEventManager().unregisterListener(registered);
            registered = null;
        }

        // Copy first: once the ids are out of the map the listener stops blanking their packets, which is what makes
        // the inventory updates below actually restore something.
        final var viewers = new HashSet<>(hidden.keySet());
        hidden.clear();

        for (final UUID uuid : viewers) {
            final Player viewer = Bukkit.getPlayer(uuid);
            if (viewer == null) {
                continue;
            }

            viewer.updateInventory();
        }
    }

    @Override
    public void hide(@NotNull final Player viewer, final int menuSize) {
        hidden.put(viewer.getUniqueId(), menuSize);
    }

    @Override
    public void unhide(@NotNull final Player viewer) {
        if (hidden.remove(viewer.getUniqueId()) == null) {
            return;
        }

        if (!viewer.isOnline()) {
            return;
        }

        // The client is still showing blanked slots, so push the real contents back.
        if (!plugin.isEnabled()) {
            viewer.updateInventory();
            return;
        }

        Bukkit.getScheduler().runTask(plugin, viewer::updateInventory);
    }

    @Override
    public void onPacketSend(final PacketSendEvent event) {
        if (hidden.isEmpty()) {
            return;
        }

        final PacketTypeCommon packetType = event.getPacketType();
        if (packetType != PacketType.Play.Server.WINDOW_ITEMS
                && packetType != PacketType.Play.Server.SET_SLOT
                && packetType != PacketType.Play.Server.SET_PLAYER_INVENTORY) {
            return;
        }

        final User user = event.getUser();
        if (user == null || user.getUUID() == null) {
            return;
        }

        final Integer menuSize = hidden.get(user.getUUID());
        if (menuSize == null) {
            return;
        }

        try {
            if (packetType == PacketType.Play.Server.WINDOW_ITEMS) {
                if (!blankWindowItems(event, menuSize)) {
                    return;
                }
            } else if (packetType == PacketType.Play.Server.SET_SLOT) {
                if (!blankSetSlot(event, menuSize)) {
                    return;
                }
            } else if (!blankPlayerInventorySlot(event)) {
                return;
            }

            event.markForReEncode(true);
        } catch (final Exception exception) {
            plugin.printStacktrace("Failed to hide the player inventory of " + user.getName(), exception);
            plugin.debug(
                    DebugLevel.HIGHEST,
                    Level.WARNING,
                    "Could not hide the player inventory of " + user.getName() + "!"
            );
        }
    }

    /**
     * @return true if the packet was modified and needs to be re-encoded
     */
    private boolean blankWindowItems(final @NotNull PacketSendEvent event, final int menuSize) {
        final WrapperPlayServerWindowItems packet = new WrapperPlayServerWindowItems(event);

        if (packet.getWindowId() == PLAYER_INVENTORY_WINDOW_ID) {
            return false;
        }

        final List<ItemStack> items = new ArrayList<>(packet.getItems());
        if (items.size() <= menuSize) {
            return false;
        }

        for (int slot = menuSize; slot < items.size(); slot++) {
            items.set(slot, ItemStack.EMPTY);
        }

        packet.setItems(items);
        return true;
    }

    /**
     * @return true if the packet was modified and needs to be re-encoded
     */
    private boolean blankSetSlot(final @NotNull PacketSendEvent event, final int menuSize) {
        final WrapperPlayServerSetSlot packet = new WrapperPlayServerSetSlot(event);

        // 0 is the player's own inventory screen, -1 is the item on the cursor.
        if (packet.getWindowId() <= PLAYER_INVENTORY_WINDOW_ID) {
            return false;
        }

        if (packet.getSlot() < menuSize) {
            return false;
        }

        packet.setItem(ItemStack.EMPTY);
        return true;
    }

    /**
     * Blanks a 1.21.2 and newer player inventory update. Unlike the other two packets this one carries no window id
     * and always targets the player inventory, so it is what stops an item handed to the player by another plugin from
     * popping up underneath an open menu.
     *
     * @return true if the packet was modified and needs to be re-encoded
     */
    private boolean blankPlayerInventorySlot(final @NotNull PacketSendEvent event) {
        final WrapperPlayServerSetPlayerInventory packet = new WrapperPlayServerSetPlayerInventory(event);

        // Armor and offhand are not part of the inventory shown under a menu, and blanking them would also blank the
        // viewer's own armor rendering until the menu is closed.
        if (packet.getSlot() >= FIRST_UNSEEN_PLAYER_INVENTORY_SLOT) {
            return false;
        }

        packet.setStack(ItemStack.EMPTY);
        return true;
    }
}
