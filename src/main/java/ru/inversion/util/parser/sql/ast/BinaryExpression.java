package ru.inversion.util.parser.sql.ast;

import ru.inversion.util.parser.lexer.Token;
import ru.inversion.util.parser.sql.lexer.SqlTokenKind;
import ru.inversion.util.parser.text.TextRange;

import java.util.Objects;

/**
 * Бинарное SQL-выражение.
 *
 * Примеры:
 *   a + b
 *   price >= :minimum
 *   active = 1
 */
public final class BinaryExpression extends SqlExpression {

    private final SqlExpression left;
    private final Token<SqlTokenKind> operator;
    private final SqlExpression right;

    public BinaryExpression( SqlExpression left, Token<SqlTokenKind> operator, SqlExpression right )
    {
        super( createRange(left, operator, right) );

        this.left     = left;
        this.operator = operator;
        this.right    = right;
    }

    public SqlExpression left() {
        return left;
    }

    public Token<SqlTokenKind> operator() {
        return operator;
    }

    public SqlExpression right() {
        return right;
    }

    private static TextRange createRange( SqlExpression left, Token<SqlTokenKind> operator, SqlExpression right )
    {
        Objects.requireNonNull( left, "left");
        Objects.requireNonNull( operator, "operator");
        Objects.requireNonNull( right, "right");

        if( operator.kind() != SqlTokenKind.OPERATOR && operator.kind() != SqlTokenKind.WORD )
            throw new IllegalArgumentException( "Binary operator must be OPERATOR or WORD, found " + operator.kind() );

        if( left.end() > operator.range().start() )
            throw new IllegalArgumentException( "Left operand must precede operator" );

        if (operator.range().end() > right.start())
            throw new IllegalArgumentException( "Operator must precede right operand" );

        return new TextRange( left.start(), right.end() );
    }
}