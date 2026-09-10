package com.extendedclip.deluxemenus.utils;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Color;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class StringUtils {

    /**
     * The '§x§a§a§b§b§c§c' format that Spigot and some PlaceholderAPI expansions use for hex colors.
     * <br>
     * Each digit needs its own capturing group, since a repeated group would only retain its last
     * match.
     */
    private final static Pattern UNUSUAL_HEX_PATTERN = Pattern
            .compile("§x§([a-f0-9])§([a-f0-9])§([a-f0-9])§([a-f0-9])§([a-f0-9])§([a-f0-9])",
                    Pattern.CASE_INSENSITIVE);

    /**
     * Legacy color codes, using either the ampersand or the section symbol. Also matches the
     * '&#aaFF00' hex format.
     */
    private final static Pattern LEGACY_PATTERN = Pattern
            .compile("[&§](#[a-f0-9]{6}|[0-9a-fk-or])", Pattern.CASE_INSENSITIVE);

    private final static Map<Character, String> LEGACY_TAGS = Map.ofEntries(
            Map.entry('0', "<black>"),
            Map.entry('1', "<dark_blue>"),
            Map.entry('2', "<dark_green>"),
            Map.entry('3', "<dark_aqua>"),
            Map.entry('4', "<dark_red>"),
            Map.entry('5', "<dark_purple>"),
            Map.entry('6', "<gold>"),
            Map.entry('7', "<gray>"),
            Map.entry('8', "<dark_gray>"),
            Map.entry('9', "<blue>"),
            Map.entry('a', "<green>"),
            Map.entry('b', "<aqua>"),
            Map.entry('c', "<red>"),
            Map.entry('d', "<light_purple>"),
            Map.entry('e', "<yellow>"),
            Map.entry('f', "<white>"),
            Map.entry('k', "<obfuscated>"),
            Map.entry('l', "<bold>"),
            Map.entry('m', "<strikethrough>"),
            Map.entry('n', "<underlined>"),
            Map.entry('o', "<italic>"),
            Map.entry('r', "<reset>")
    );

    /**
     * The legacy codes that set a color. Unlike the decoration codes, these also clear any
     * formatting that is already active.
     */
    private final static String LEGACY_COLOR_CODES = "0123456789abcdef";

    private final static MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private final static LegacyComponentSerializer SECTION = LegacyComponentSerializer.builder()
            .character('§')
            .hexColors()
            .useUnusualXRepeatedCharacterHexFormat()
            .build();

    /**
     * Parses a string into a component, supporting both the MiniMessage format and the legacy color
     * codes.
     * <br>
     * MiniMessage cannot see the legacy codes, so they are rewritten into their equivalent tags
     * before the string is deserialized. That way both syntaxes can be mixed within a single string.
     * Both '&' and '§' are recognised, since placeholders are replaced before this runs and
     * PlaceholderAPI expansions commonly return text that is already coloured with '§'.
     *
     * @param input The string to parse.
     * @return The parsed component.
     */
    @NotNull
    public static Component parse(@NotNull final String input) {
        return MINI_MESSAGE.deserialize(legacyToMiniMessage(input));
    }

    /**
     * Same as {@link #parse(String)}, but with italics explicitly turned off. Used for item display
     * names and lore, which the client renders in italics by default.
     *
     * @param input The string to parse.
     * @return The parsed component, with italics disabled.
     */
    @NotNull
    public static Component parseItem(@NotNull final String input) {
        return parse(input).decoration(TextDecoration.ITALIC, false);
    }

    /**
     * Translates the ampersand color codes like '&7' to their section symbol counterparts like '§7'.
     * <br>
     * It also translates hex colors like '&#aaFF00' to their section symbol counterparts like
     * '§x§a§a§F§F§0§0', and MiniMessage tags like '&lt;red&gt;' to their closest legacy equivalent.
     *
     * @param input The string in which to translate the color codes.
     * @return The string with the translated colors.
     */
    @NotNull
    public static String color(@NotNull String input) {
        return SECTION.serialize(parse(input));
    }

    /**
     * Rewrites the legacy color codes in the given string into their MiniMessage tag equivalents,
     * leaving any existing MiniMessage tags untouched.
     */
    @NotNull
    private static String legacyToMiniMessage(@NotNull String input) {
        // '§x§a§a§b§b§c§c' has to be handled first, otherwise LEGACY_PATTERN would eat it one code
        // at a time and turn it into six separate colors.
        Matcher hexMatcher = UNUSUAL_HEX_PATTERN.matcher(input);
        StringBuilder builder = new StringBuilder();
        while (hexMatcher.find()) {
            final StringBuilder hex = new StringBuilder("<#");
            for (int group = 1; group <= hexMatcher.groupCount(); group++) {
                hex.append(hexMatcher.group(group));
            }
            hexMatcher.appendReplacement(builder, Matcher.quoteReplacement(hex.append('>').toString()));
        }
        hexMatcher.appendTail(builder);
        input = builder.toString();

        Matcher legacyMatcher = LEGACY_PATTERN.matcher(input);
        builder = new StringBuilder();
        while (legacyMatcher.find()) {
            final String code = legacyMatcher.group(1);
            final String replacement;
            if (code.charAt(0) == '#') {
                // A hex color is still a color, so it clears the active formatting too.
                replacement = "<reset><" + code.toLowerCase() + ">";
            } else {
                final char character = Character.toLowerCase(code.charAt(0));
                final String tag = LEGACY_TAGS.get(character);
                // MiniMessage colors do not clear decorations, but legacy color codes do. Without
                // the reset, formatting from an earlier code would bleed past the color change and
                // menus written for the old behaviour would render differently.
                replacement = LEGACY_COLOR_CODES.indexOf(character) >= 0 ? "<reset>" + tag : tag;
            }
            legacyMatcher.appendReplacement(builder, Matcher.quoteReplacement(replacement));
        }
        legacyMatcher.appendTail(builder);

        return builder.toString();
    }

    @NotNull
    public static String replacePlaceholdersAndArguments(@NotNull String input, final @Nullable Map<String, String> arguments,
                                                         final @Nullable Player player,
                                                         final boolean parsePlaceholdersInsideArguments,
                                                         final boolean parsePlaceholdersAfterArguments) {
        if (player == null) {
            return replaceArguments(input, arguments, null, parsePlaceholdersInsideArguments);
        }

        if (parsePlaceholdersAfterArguments) {
            return replacePlaceholders(replaceArguments(input, arguments, player, parsePlaceholdersInsideArguments), player);
        }

        return replaceArguments(replacePlaceholders(input, player), arguments, player, parsePlaceholdersInsideArguments);
    }

    @NotNull
    public static String replacePlaceholders(final @NotNull String input, final @NotNull Player player) {
        return PlaceholderAPI.setPlaceholders(player, input);
    }

    @NotNull
    public static String replaceArguments(@NotNull String input, final @Nullable Map<String, String> arguments,
                                          final @Nullable Player player, boolean parsePlaceholdersInsideArguments) {
        if (arguments == null || arguments.isEmpty()) {
            return input;
        }

        for (final Map.Entry<String, String> entry : arguments.entrySet()) {
            final String value = player != null && parsePlaceholdersInsideArguments
                    ? replacePlaceholders(entry.getValue(), player)
                    : entry.getValue();
            input = input.replace("{" + entry.getKey() + "}", value);
        }

        return input;
    }

    @Nullable
    public static Color parseRGBColor(@NotNull final String input) {
        final String[] parts = input.split(",");
        try {
            return Color.fromRGB(
                    Integer.parseInt(parts[0].trim()),
                    Integer.parseInt(parts[1].trim()),
                    Integer.parseInt(parts[2].trim())
            );
        } catch (final Exception exception) {
            return null;
        }
    }
}
