package com.extendedclip.deluxemenus.inventory.hider.impl;

import com.extendedclip.deluxemenus.DeluxeMenus;
import com.extendedclip.deluxemenus.inventory.BottomInventorySlots;
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
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Hides the player inventory by blanking it out of the container packets sent to the client, and draws the menu's
 * {@code location: bottom} items over it by substituting them into those same packets.
 * <p>
 * The server side player inventory is never read or modified, so this class cannot lose a player's items, and the
 * items it draws underneath a menu are never actually given to the player. The worst possible failure is a client side
 * desync where the inventory looks wrong, which any inventory update or relog repairs. Because outgoing packets are
 * intercepted, every server driven resync (menu refreshes, another plugin giving the player an item,
 * {@link Player#updateInventory()}) keeps the inventory hidden and the overlay drawn without any packet being sent
 * from here.
 *
 * @see <a href="https://github.com/retrooper/packetevents">PacketEvents</a>
 */
public class PacketEventsInventoryHider extends PacketListenerAbstract implements InventoryHider {

    /**
     * The window id the client uses for its own inventory screen. Packets for it must never be touched, otherwise the
     * player would also see an empty inventory when opening it with no menu on screen.
     */
    private static final int PLAYER_INVENTORY_WINDOW_ID = 0;

    private final DeluxeMenus plugin;

    /**
     * Viewer uuid to the view being drawn for them. Written from the main thread and read from netty threads, hence
     * the concurrent map.
     */
    private final Map<UUID, HiddenView> hidden = new ConcurrentHashMap<>();

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

        // Copy first: once the ids are out of the map the listener stops rewriting their packets, which is what makes
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
    public boolean isHidden(@NotNull final Player viewer) {
        return hidden.containsKey(viewer.getUniqueId());
    }

    @Override
    public void hide(@NotNull final Player viewer, final @NotNull Inventory menu, final @NotNull Map<Integer, org.bukkit.inventory.ItemStack> bottomContents) {
        hidden.put(viewer.getUniqueId(), new HiddenView(menu, menu.getSize(), convert(bottomContents)));
    }

    @Override
    public void updateOverlay(@NotNull final Player viewer, final @NotNull Map<Integer, org.bukkit.inventory.ItemStack> bottomContents) {
        final Map<Integer, ItemStack> overlay = convert(bottomContents);
        hidden.computeIfPresent(viewer.getUniqueId(), (uuid, view) -> new HiddenView(view.menu(), view.menuSize(), overlay));
    }

    @Override
    public void unhide(@NotNull final Player viewer, final @NotNull Inventory closed) {
        final HiddenView view = hidden.get(viewer.getUniqueId());
        if (view == null || !closed.equals(view.menu())) {
            return;
        }

        unhide(viewer);
    }

    @Override
    public void unhide(@NotNull final Player viewer) {
        if (hidden.remove(viewer.getUniqueId()) == null) {
            return;
        }

        if (!viewer.isOnline()) {
            return;
        }

        // The client is still showing rewritten slots, so push the real contents back.
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

        final HiddenView view = hidden.get(user.getUUID());
        if (view == null) {
            return;
        }

        try {
            if (packetType == PacketType.Play.Server.WINDOW_ITEMS) {
                if (!rewriteWindowItems(event, view)) {
                    return;
                }
            } else if (packetType == PacketType.Play.Server.SET_SLOT) {
                if (!rewriteSetSlot(event, view)) {
                    return;
                }
            } else if (!rewritePlayerInventorySlot(event, view)) {
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
    private boolean rewriteWindowItems(final @NotNull PacketSendEvent event, final @NotNull HiddenView view) {
        final WrapperPlayServerWindowItems packet = new WrapperPlayServerWindowItems(event);

        if (packet.getWindowId() == PLAYER_INVENTORY_WINDOW_ID) {
            return false;
        }

        final List<ItemStack> items = new ArrayList<>(packet.getItems());
        if (items.size() <= view.menuSize()) {
            return false;
        }

        for (int rawSlot = view.menuSize(); rawSlot < items.size(); rawSlot++) {
            items.set(rawSlot, view.itemForRawSlot(rawSlot));
        }

        packet.setItems(items);
        return true;
    }

    /**
     * @return true if the packet was modified and needs to be re-encoded
     */
    private boolean rewriteSetSlot(final @NotNull PacketSendEvent event, final @NotNull HiddenView view) {
        final WrapperPlayServerSetSlot packet = new WrapperPlayServerSetSlot(event);

        // 0 is the player's own inventory screen, -1 is the item on the cursor.
        if (packet.getWindowId() <= PLAYER_INVENTORY_WINDOW_ID) {
            return false;
        }

        if (packet.getSlot() < view.menuSize()) {
            return false;
        }

        packet.setItem(view.itemForRawSlot(packet.getSlot()));
        return true;
    }

    /**
     * Rewrites a 1.21.2 and newer player inventory update. Unlike the other two packets this one carries no window id
     * and always targets the player inventory, so it is what stops an item handed to the player by another plugin from
     * popping up underneath an open menu. Its slot is already a player inventory slot, so it indexes the overlay
     * directly.
     *
     * @return true if the packet was modified and needs to be re-encoded
     */
    private boolean rewritePlayerInventorySlot(final @NotNull PacketSendEvent event, final @NotNull HiddenView view) {
        final WrapperPlayServerSetPlayerInventory packet = new WrapperPlayServerSetPlayerInventory(event);

        // Armor and offhand are not part of the inventory shown under a menu, and blanking them would also blank the
        // viewer's own armor rendering until the menu is closed.
        if (!BottomInventorySlots.isValid(packet.getSlot())) {
            return false;
        }

        packet.setStack(view.overlay().getOrDefault(packet.getSlot(), ItemStack.EMPTY));
        return true;
    }

    /**
     * Converts the rendered menu items into their packet representation once, on the calling thread, so that netty
     * threads only ever read from an immutable map.
     */
    private @NotNull Map<Integer, ItemStack> convert(final @NotNull Map<Integer, org.bukkit.inventory.ItemStack> bottomContents) {
        if (bottomContents.isEmpty()) {
            return Map.of();
        }

        final Map<Integer, ItemStack> converted = new HashMap<>(bottomContents.size());

        for (final Map.Entry<Integer, org.bukkit.inventory.ItemStack> entry : bottomContents.entrySet()) {
            final org.bukkit.inventory.ItemStack itemStack = entry.getValue();
            if (itemStack == null || itemStack.getType() == Material.AIR) {
                continue;
            }

            if (!BottomInventorySlots.isValid(entry.getKey())) {
                continue;
            }

            converted.put(entry.getKey(), SpigotConversionUtil.fromBukkitItemStack(itemStack));
        }

        return Map.copyOf(converted);
    }

    /**
     * A viewer's hidden inventory and the items drawn over it.
     *
     * @param menu     the menu this view belongs to. Only ever read from the main thread, to tell a close of this
     *                 window apart from a close of one it replaced.
     * @param menuSize the size of the menu inventory. Every raw slot at or above it belongs to the player inventory.
     *                 Cached here so the packet path never calls into the Bukkit API from a netty thread.
     * @param overlay  items to draw, keyed by player inventory slot. Empty for menus without bottom items.
     */
    private record HiddenView(@NotNull Inventory menu, int menuSize, @NotNull Map<Integer, ItemStack> overlay) {

        /**
         * The item to draw at a raw slot known to be part of the player inventory, blank when nothing is drawn there.
         */
        private @NotNull ItemStack itemForRawSlot(final int rawSlot) {
            if (overlay.isEmpty()) {
                return ItemStack.EMPTY;
            }

            final int bottomSlot = BottomInventorySlots.fromRawSlot(rawSlot, menuSize);
            if (bottomSlot < 0) {
                return ItemStack.EMPTY;
            }

            return overlay.getOrDefault(bottomSlot, ItemStack.EMPTY);
        }
    }
}
