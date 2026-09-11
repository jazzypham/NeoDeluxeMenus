package com.extendedclip.deluxemenus.requirement;

import com.extendedclip.deluxemenus.menu.MenuHolder;
import org.jetbrains.annotations.NotNull;

/**
 * Stands in for a requirement that could not be built from the config, so that a broken requirement
 * denies instead of disappearing.
 * <p>
 * Dropping the requirement would be a fail open: a requirement list that ends up empty is stored as
 * {@code null}, and both {@code Menu#handleOpenRequirements} and the click handling treat a null
 * list as "nothing to check". A typo in the only requirement of a block would then let everyone
 * through.
 * <p>
 * The reason is only reported once, when the config is loaded, because evaluating happens on every
 * click and on every view requirement pass.
 */
public class InvalidRequirement extends Requirement {

  private final String path;
  private final String reason;

  /**
   * @param path   the config path of the requirement that failed to load
   * @param reason why it failed, for debugging
   */
  public InvalidRequirement(final @NotNull String path, final @NotNull String reason) {
    this.path = path;
    this.reason = reason;
  }

  @Override
  public boolean evaluate(MenuHolder holder) {
    return false;
  }

  /**
   * Always false. A requirement that could not be loaded must not be skippable, otherwise marking
   * the broken entry {@code optional: true} would bring the fail open back.
   */
  @Override
  public boolean isOptional() {
    return false;
  }

  public @NotNull String getPath() {
    return path;
  }

  public @NotNull String getReason() {
    return reason;
  }

  @Override
  public String toString() {
    return "InvalidRequirement{path=" + path + ", reason=" + reason + "}";
  }
}
