package com.extendedclip.deluxemenus.requirement.condition;

/**
 * Thrown while walking a parsed condition when a value has the wrong type for the operator using it,
 * for example a non-boolean operand of {@code &&}. Caught by the requirement, which logs it and
 * evaluates to false.
 */
public class ConditionEvaluationException extends RuntimeException {

  public ConditionEvaluationException(String message) {
    super(message);
  }
}
