package com.extendedclip.deluxemenus.requirement.condition;

import com.extendedclip.deluxemenus.menu.MenuHolder;

/**
 * A node of a parsed condition expression.
 * <p>
 * Values flowing through the tree are always a {@link Double}, a {@link String} or a
 * {@link Boolean}. Constant nodes are resolved once at parse time; only {@link Dynamic} and
 * {@link DynamicString} touch placeholders, and they only ever resolve their own text.
 */
public sealed interface ConditionNode {

  Object evaluate(MenuHolder holder);

  /**
   * Turns a resolved string into a {@link Boolean}, a {@link Double} or itself.
   */
  static Object coerce(final String value) {
    if (value.equalsIgnoreCase("true")) {
      return Boolean.TRUE;
    }
    if (value.equalsIgnoreCase("false")) {
      return Boolean.FALSE;
    }

    final Double number = parseNumber(value);
    return number != null ? number : value;
  }

  /**
   * Parses a plain decimal number, or returns null. Deliberately stricter than
   * {@link Double#parseDouble(String)}: forms a config author never means as a number, such as
   * {@code 5d}, {@code NaN} or {@code 0x1p3}, stay strings.
   */
  static Double parseNumber(final String value) {
    if (value.isEmpty()) {
      return null;
    }

    final char first = value.charAt(0);
    if (!Character.isDigit(first) && first != '-' && first != '+' && first != '.') {
      return null;
    }

    final char last = value.charAt(value.length() - 1);
    if (!Character.isDigit(last) && last != '.') {
      return null;
    }

    try {
      return Double.valueOf(value);
    } catch (final NumberFormatException exception) {
      return null;
    }
  }

  /**
   * The string form of a value. Whole numbers lose their trailing {@code .0} so that comparing a
   * numeric placeholder against a quoted string behaves the way a config author expects.
   */
  static String asString(final Object value) {
    if (value instanceof Double number) {
      final double d = number;
      if (d == Math.rint(d) && !Double.isInfinite(d) && Math.abs(d) < 1e15) {
        return Long.toString((long) d);
      }
      return Double.toString(d);
    }
    return String.valueOf(value);
  }

  /**
   * A value used where a boolean is required.
   *
   * @throws ConditionEvaluationException if the value is not a boolean
   */
  static boolean truth(final Object value) {
    if (value instanceof Boolean bool) {
      return bool;
    }
    throw new ConditionEvaluationException(
        "Expected true or false but got '" + asString(value) + "'");
  }

  enum Comparison {
    LESS("<"),
    LESS_EQUAL("<="),
    GREATER(">"),
    GREATER_EQUAL(">=");

    private final String symbol;

    Comparison(String symbol) {
      this.symbol = symbol;
    }

    public String symbol() {
      return symbol;
    }

    public boolean test(final int comparison) {
      return switch (this) {
        case LESS -> comparison < 0;
        case LESS_EQUAL -> comparison <= 0;
        case GREATER -> comparison > 0;
        case GREATER_EQUAL -> comparison >= 0;
      };
    }
  }

  /** A literal known at parse time. */
  record Constant(Object value) implements ConditionNode {
    @Override
    public Object evaluate(final MenuHolder holder) {
      return value;
    }
  }

  /** A bare term holding placeholders or arguments; its result is coerced. */
  record Dynamic(String raw) implements ConditionNode {
    @Override
    public Object evaluate(final MenuHolder holder) {
      return coerce(holder.setPlaceholdersAndArguments(raw));
    }
  }

  /** A quoted string holding placeholders or arguments; its result stays a string. */
  record DynamicString(String raw) implements ConditionNode {
    @Override
    public Object evaluate(final MenuHolder holder) {
      return holder.setPlaceholdersAndArguments(raw);
    }
  }

  record Not(ConditionNode operand) implements ConditionNode {
    @Override
    public Object evaluate(final MenuHolder holder) {
      return !truth(operand.evaluate(holder));
    }
  }

  record And(ConditionNode left, ConditionNode right) implements ConditionNode {
    @Override
    public Object evaluate(final MenuHolder holder) {
      return truth(left.evaluate(holder)) && truth(right.evaluate(holder));
    }
  }

  record Or(ConditionNode left, ConditionNode right) implements ConditionNode {
    @Override
    public Object evaluate(final MenuHolder holder) {
      return truth(left.evaluate(holder)) || truth(right.evaluate(holder));
    }
  }

  record Equality(ConditionNode left, ConditionNode right, boolean negated) implements ConditionNode {
    @Override
    public Object evaluate(final MenuHolder holder) {
      final Object l = left.evaluate(holder);
      final Object r = right.evaluate(holder);

      final boolean equal;
      if (l instanceof Double a && r instanceof Double b) {
        equal = a.doubleValue() == b.doubleValue();
      } else if (l instanceof Boolean a && r instanceof Boolean b) {
        equal = a.equals(b);
      } else {
        equal = asString(l).equals(asString(r));
      }

      return negated != equal;
    }
  }

  /**
   * Numeric compare when both sides are numbers, lexicographic compare otherwise.
   */
  record Relational(ConditionNode left, ConditionNode right, Comparison comparison) implements ConditionNode {
    @Override
    public Object evaluate(final MenuHolder holder) {
      final Object l = left.evaluate(holder);
      final Object r = right.evaluate(holder);

      final int result;
      if (l instanceof Double a && r instanceof Double b) {
        result = Double.compare(a, b);
      } else {
        result = asString(l).compareTo(asString(r));
      }

      return comparison.test(result);
    }
  }
}
