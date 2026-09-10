package com.extendedclip.deluxemenus.requirement.condition;

import java.util.List;

/**
 * A recursive descent parser for condition expressions.
 * <p>
 * Precedence, lowest to highest:
 * <pre>
 * or         := and        ( '||' and )*
 * and        := equality   ( '&amp;&amp;' equality )*
 * equality   := relational ( ( '==' | '!=' ) relational )*
 * relational := unary      ( ( '&lt;' | '&lt;=' | '&gt;' | '&gt;=' ) unary )*
 * unary      := '!' unary | primary
 * primary    := STRING | TERM | '(' or ')'
 * </pre>
 * Parsing happens once, when the menu config is loaded. Evaluating a menu click is then only a walk
 * of the resulting {@link ConditionNode} tree.
 */
public final class ConditionParser {

  private final String expression;
  private final List<Token> tokens;
  private int index;

  private ConditionParser(final String expression, final List<Token> tokens) {
    this.expression = expression;
    this.tokens = tokens;
  }

  /**
   * @throws ConditionParseException if the expression is malformed
   */
  public static ConditionNode parse(final String expression) {
    final ConditionParser parser = new ConditionParser(expression, ConditionLexer.lex(expression));
    final ConditionNode node = parser.or();

    final Token remaining = parser.peek();
    if (remaining.type() != Token.Type.EOF) {
      throw parser.error("Unexpected '" + remaining.text() + "'", remaining);
    }

    return node;
  }

  private ConditionNode or() {
    ConditionNode node = and();
    while (matchOp("||")) {
      node = new ConditionNode.Or(node, and());
    }
    return node;
  }

  private ConditionNode and() {
    ConditionNode node = equality();
    while (matchOp("&&")) {
      node = new ConditionNode.And(node, equality());
    }
    return node;
  }

  private ConditionNode equality() {
    ConditionNode node = relational();
    while (true) {
      if (matchOp("==")) {
        node = new ConditionNode.Equality(node, relational(), false);
      } else if (matchOp("!=")) {
        node = new ConditionNode.Equality(node, relational(), true);
      } else {
        return node;
      }
    }
  }

  private ConditionNode relational() {
    ConditionNode node = unary();
    while (true) {
      final ConditionNode.Comparison comparison = comparisonOf(peek());
      if (comparison == null) {
        return node;
      }
      index++;
      node = new ConditionNode.Relational(node, unary(), comparison);
    }
  }

  private ConditionNode unary() {
    if (matchOp("!")) {
      return new ConditionNode.Not(unary());
    }
    return primary();
  }

  private ConditionNode primary() {
    final Token token = peek();

    switch (token.type()) {
      case LPAREN:
        index++;
        final ConditionNode node = or();
        final Token closing = peek();
        if (closing.type() != Token.Type.RPAREN) {
          throw error("Expected ')'", closing);
        }
        index++;
        return node;
      case STRING:
        index++;
        return stringNode(token.text());
      case TERM:
        index++;
        return termNode(token.text());
      case EOF:
        throw error("Unexpected end of expression", token);
      default:
        throw error("Unexpected '" + token.text() + "'", token);
    }
  }

  /**
   * Classifies a bare term. Only terms holding a placeholder or an argument stay dynamic; everything
   * else becomes a constant here and costs nothing at evaluation time.
   */
  private static ConditionNode termNode(final String text) {
    if (text.equalsIgnoreCase("true")) {
      return new ConditionNode.Constant(Boolean.TRUE);
    }
    if (text.equalsIgnoreCase("false")) {
      return new ConditionNode.Constant(Boolean.FALSE);
    }

    final Double number = ConditionNode.parseNumber(text);
    if (number != null) {
      return new ConditionNode.Constant(number);
    }

    if (isDynamic(text)) {
      return new ConditionNode.Dynamic(text);
    }

    return new ConditionNode.Constant(text);
  }

  private static ConditionNode stringNode(final String text) {
    return isDynamic(text)
        ? new ConditionNode.DynamicString(text)
        : new ConditionNode.Constant(text);
  }

  private static boolean isDynamic(final String text) {
    return text.indexOf('%') >= 0 || text.indexOf('{') >= 0;
  }

  private static ConditionNode.Comparison comparisonOf(final Token token) {
    if (token.type() != Token.Type.OP) {
      return null;
    }
    for (final ConditionNode.Comparison comparison : ConditionNode.Comparison.values()) {
      if (comparison.symbol().equals(token.text())) {
        return comparison;
      }
    }
    return null;
  }

  private Token peek() {
    return tokens.get(index);
  }

  private boolean matchOp(final String symbol) {
    if (peek().is(Token.Type.OP, symbol)) {
      index++;
      return true;
    }
    return false;
  }

  private ConditionParseException error(final String message, final Token token) {
    return new ConditionParseException(message, expression, token.position());
  }
}
