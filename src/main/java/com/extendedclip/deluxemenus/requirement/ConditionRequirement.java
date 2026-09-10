package com.extendedclip.deluxemenus.requirement;

import com.extendedclip.deluxemenus.DeluxeMenus;
import com.extendedclip.deluxemenus.menu.MenuHolder;
import com.extendedclip.deluxemenus.requirement.condition.ConditionEvaluationException;
import com.extendedclip.deluxemenus.requirement.condition.ConditionNode;
import com.extendedclip.deluxemenus.requirement.condition.ConditionParseException;
import com.extendedclip.deluxemenus.requirement.condition.ConditionParser;
import com.extendedclip.deluxemenus.utils.DebugLevel;
import java.util.logging.Level;
import org.jetbrains.annotations.NotNull;

/**
 * Evaluates a native boolean expression, without a scripting engine.
 * <p>
 * The expression is parsed once, here in the constructor, so evaluating it later is only a walk of
 * the resulting tree. Placeholders are resolved per leaf rather than over the whole expression,
 * which keeps a placeholder that expands to text containing spaces, quotes or operators from
 * changing how the expression parses.
 */
public class ConditionRequirement extends Requirement {

  private final DeluxeMenus plugin;
  private final String expression;
  private final ConditionNode root;

  /**
   * @throws ConditionParseException if the expression is malformed
   */
  public ConditionRequirement(final @NotNull DeluxeMenus plugin, final @NotNull String expression) {
    this.plugin = plugin;
    this.expression = expression;
    this.root = ConditionParser.parse(expression);
  }

  @Override
  public boolean evaluate(MenuHolder holder) {
    try {
      final Object result = root.evaluate(holder);

      if (!(result instanceof Boolean)) {
        plugin.debug(
            DebugLevel.HIGHEST,
            Level.WARNING,
            "Requirement condition <" + this.expression + "> is invalid and does not return a boolean!"
        );
        return false;
      }

      return (Boolean) result;

    } catch (final ConditionEvaluationException exception) {
      plugin.debug(
          DebugLevel.HIGHEST,
          Level.WARNING,
          "Error in requirement condition <" + this.expression + "> - " + exception.getMessage()
      );
      return false;
    }
  }
}
