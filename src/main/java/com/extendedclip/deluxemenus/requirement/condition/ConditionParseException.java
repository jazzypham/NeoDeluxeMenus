package com.extendedclip.deluxemenus.requirement.condition;

/**
 * Thrown when a condition expression cannot be lexed or parsed. This is only expected at config load
 * time, where the requirement is skipped and the problem is logged.
 */
public class ConditionParseException extends RuntimeException {

  private final String expression;
  private final int position;

  public ConditionParseException(String message, String expression, int position) {
    super(message);
    this.expression = expression;
    this.position = position;
  }

  public String getExpression() {
    return expression;
  }

  public int getPosition() {
    return position;
  }

  /**
   * The message together with the expression and a caret pointing at the offending character.
   */
  public String getDetailedMessage() {
    StringBuilder builder = new StringBuilder(getMessage());
    builder.append(" (at position ").append(position).append(")");
    builder.append("\n  ").append(expression);
    builder.append("\n  ");
    for (int i = 0; i < position && i < expression.length(); i++) {
      builder.append(expression.charAt(i) == '\t' ? '\t' : ' ');
    }
    builder.append('^');
    return builder.toString();
  }
}
