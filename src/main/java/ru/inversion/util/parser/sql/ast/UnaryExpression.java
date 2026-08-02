package ru.inversion.util.parser.sql.ast;

import ru.inversion.util.parser.lexer.Token;
import ru.inversion.util.parser.sql.lexer.SqlTokenKind;
import ru.inversion.util.parser.text.TextRange;

import java.util.Objects;

/**
 * Унарное SQL-выражение.
 *
 * Примеры:
 *   -1
 *   +value
 *   ~mask
 */
public final class UnaryExpression
        extends SqlExpression {

    private final Token<SqlTokenKind> operator;
    private final SqlExpression operand;

    public UnaryExpression(
            Token<SqlTokenKind> operator,
            SqlExpression operand
    ) {
        super(createRange(operator, operand));

        this.operator = operator;
        this.operand = operand;
    }

    public Token<SqlTokenKind> operator() {
        return operator;
    }

    public SqlExpression operand() {
        return operand;
    }

    private static TextRange createRange(
            Token<SqlTokenKind> operator,
            SqlExpression operand
    ) {
        Objects.requireNonNull(operator, "operator");
        Objects.requireNonNull(operand, "operand");

        if (operator.kind() != SqlTokenKind.OPERATOR) {
            throw new IllegalArgumentException(
                    "operator must be OPERATOR, found "
                            + operator.kind()
            );
        }

        if (operator.range().end() > operand.start()) {
            throw new IllegalArgumentException(
                    "operator must precede operand"
            );
        }

        return new TextRange(
                operator.range().start(),
                operand.end()
        );
    }
}