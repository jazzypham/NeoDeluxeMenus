package com.extendedclip.deluxemenus.utils;

import org.bukkit.Keyed;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Resolves registry values from the names menus are configured with.
 * <p>
 * Several Bukkit enums such as {@code Sound} and {@code PatternType} are now registry-backed
 * interfaces, and their {@code valueOf} methods are only kept as deprecated compatibility shims.
 * Looking values up in the registry works without them, and also accepts namespaced keys, which is
 * what the game itself uses.
 */
public final class RegistryUtils {

    private static final Map<Registry<?>, Map<String, ? extends Keyed>> INDEXES = new ConcurrentHashMap<>();

    private RegistryUtils() {
        throw new AssertionError("Util classes should not be initialized");
    }

    /**
     * Resolves a registry value from either a namespaced key ({@code entity.player.levelup},
     * {@code minecraft:stripe_bottom}) or a legacy enum constant name
     * ({@code ENTITY_PLAYER_LEVELUP}, {@code STRIPE_BOTTOM}).
     *
     * @return the value, or {@code null} if nothing matches
     */
    @SuppressWarnings("unchecked")
    public static <T extends Keyed> @Nullable T byNameOrKey(
            final @NotNull Registry<T> registry,
            final @NotNull String input
    ) {
        final NamespacedKey key = NamespacedKey.fromString(input.toLowerCase(Locale.ROOT));
        if (key != null) {
            final T direct = registry.get(key);
            if (direct != null) {
                return direct;
            }
        }

        return ((Map<String, T>) INDEXES.computeIfAbsent(registry, RegistryUtils::index))
                .get(toLegacyName(input));
    }

    /**
     * Builds the legacy-name index from the registry itself rather than by transforming the
     * name, because the reverse transformation is lossy: {@code BLOCK_NOTE_BLOCK_HARP} is
     * {@code block.note_block.harp}, not {@code block.note.block.harp}.
     */
    private static <T extends Keyed> Map<String, T> index(final Registry<T> registry) {
        final Map<String, T> index = new HashMap<>();

        for (final T value : registry) {
            final NamespacedKey key = registry.getKey(value);
            if (key == null) continue;

            index.put(toLegacyName(key.value()), value);
        }

        return index;
    }

    private static String toLegacyName(final @NotNull String value) {
        return value.toUpperCase(Locale.ROOT).replace('.', '_');
    }
}
