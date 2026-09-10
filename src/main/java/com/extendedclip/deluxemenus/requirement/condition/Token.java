package com.extendedclip.deluxemenus.requirement.condition;

/**
 * A single lexed piece of a condition expression.
 *
 * @param type     what kind of token this is
 * @param text     the exact text, with quotes and escapes already stripped for {@link Type#STRING}
 * @param position the index of the token's first character in the original expression
 */
public record Token(Token.Type type, String text, int position) {

  public enum Type {
    /** One of {@code && || ! == != < <= > >=}. */
    OP,
    /** A bare term: a number, a word, a placeholder or an argument. */
    TERM,
    /** A quoted string. Always evaluates to a string, never a number or boolean. */
    STRING,
    LPAREN,
    RPAREN,
    EOF
  }

  public boolean is(Type type, String text) {
    return this.type == type && this.text.equals(text);
  }
}
