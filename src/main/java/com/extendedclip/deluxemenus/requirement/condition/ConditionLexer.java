package com.extendedclip.deluxemenus.requirement.condition;

import java.util.ArrayList;
import java.util.List;

/**
 * Turns a condition expression into a flat list of {@link Token}s in a single pass.
 * <p>
 * Everything that is not an operator, a parenthesis or a quoted string becomes a single
 * {@link Token.Type#TERM}. That covers numbers, bare words, placeholders and arguments alike, which
 * is why {@code %some_placeholder%>=5} lexes correctly without any surrounding spaces.
 */
public final class ConditionLexer {

  private ConditionLexer() {
  }

  /**
   * Characters that end a {@link Token.Type#TERM}. Whitespace ends one too, but is checked
   * separately.
   */
  private static boolean isTermBreak(char c) {
    return c == '(' || c == ')' || c == '"' || c == '\''
        || c == '!' || c == '<' || c == '>' || c == '=' || c == '&' || c == '|';
  }

  public static List<Token> lex(final String expression) {
    final List<Token> tokens = new ArrayList<>();
    final int length = expression.length();
    int i = 0;

    while (i < length) {
      final char c = expression.charAt(i);

      if (Character.isWhitespace(c)) {
        i++;
        continue;
      }

      switch (c) {
        case '(':
          tokens.add(new Token(Token.Type.LPAREN, "(", i));
          i++;
          continue;
        case ')':
          tokens.add(new Token(Token.Type.RPAREN, ")", i));
          i++;
          continue;
        case '"':
        case '\'':
          i = readString(expression, i, tokens);
          continue;
        case '&':
          if (peek(expression, i + 1) != '&') {
            throw new ConditionParseException("Unexpected '&'; use '&&' for logical and", expression, i);
          }
          tokens.add(new Token(Token.Type.OP, "&&", i));
          i += 2;
          continue;
        case '|':
          if (peek(expression, i + 1) != '|') {
            throw new ConditionParseException("Unexpected '|'; use '||' for logical or", expression, i);
          }
          tokens.add(new Token(Token.Type.OP, "||", i));
          i += 2;
          continue;
        case '=':
          if (peek(expression, i + 1) != '=') {
            throw new ConditionParseException("Unexpected '='; use '==' for equality", expression, i);
          }
          tokens.add(new Token(Token.Type.OP, "==", i));
          i += 2;
          continue;
        case '!':
        case '<':
        case '>':
          if (peek(expression, i + 1) == '=') {
            tokens.add(new Token(Token.Type.OP, c + "=", i));
            i += 2;
          } else {
            tokens.add(new Token(Token.Type.OP, String.valueOf(c), i));
            i++;
          }
          continue;
        default:
          i = readTerm(expression, i, tokens);
      }
    }

    tokens.add(new Token(Token.Type.EOF, "", length));
    return tokens;
  }

  private static char peek(final String expression, final int index) {
    return index < expression.length() ? expression.charAt(index) : '\0';
  }

  /**
   * Reads a quoted string starting at {@code start}, supporting backslash escapes, and returns the
   * index just past the closing quote.
   */
  private static int readString(final String expression, final int start, final List<Token> tokens) {
    final char quote = expression.charAt(start);
    final StringBuilder value = new StringBuilder();
    int i = start + 1;

    while (i < expression.length()) {
      final char c = expression.charAt(i);

      if (c == '\\' && i + 1 < expression.length()) {
        value.append(expression.charAt(i + 1));
        i += 2;
        continue;
      }

      if (c == quote) {
        tokens.add(new Token(Token.Type.STRING, value.toString(), start));
        return i + 1;
      }

      value.append(c);
      i++;
    }

    throw new ConditionParseException("Unterminated string, missing a closing " + quote, expression, start);
  }

  /**
   * Reads a bare term starting at {@code start} and returns the index just past its last character.
   * <p>
   * A {@code %placeholder%} or an {@code {argument}} is consumed whole, so characters that would
   * normally end a term stay part of it. That keeps placeholders such as {@code %math_(1+2)%}
   * intact.
   */
  private static int readTerm(final String expression, final int start, final List<Token> tokens) {
    int i = start;

    while (i < expression.length()) {
      final char c = expression.charAt(i);

      if (c == '%' || c == '{') {
        final int closing = closingIndex(expression, i, c == '%' ? '%' : '}');
        if (closing >= 0) {
          i = closing + 1;
          continue;
        }
      }

      if (Character.isWhitespace(c) || isTermBreak(c)) {
        break;
      }
      i++;
    }

    tokens.add(new Token(Token.Type.TERM, expression.substring(start, i), start));
    return i;
  }

  /**
   * The index of the character closing a placeholder or an argument opened at {@code open}, or -1 if
   * there is none before the next whitespace. Neither placeholders nor arguments contain spaces, so
   * stopping there keeps a stray {@code %} from swallowing the rest of the expression.
   */
  private static int closingIndex(final String expression, final int open, final char closing) {
    for (int i = open + 1; i < expression.length(); i++) {
      final char c = expression.charAt(i);
      if (Character.isWhitespace(c)) {
        return -1;
      }
      if (c == closing) {
        return i;
      }
    }
    return -1;
  }
}
