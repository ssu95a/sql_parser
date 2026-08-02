package ru.inversion.util.parser.sql.ast;

import ru.inversion.util.parser.lexer.Token;
import ru.inversion.util.parser.sql.lexer.SqlTokenKind;
import ru.inversion.util.parser.text.TextRange;

import java.util.Objects;

/**
 * Выражение в круглых скобках.
 *
 * Примеры:
 *   (123)
 *   (:parameter)
 *   ((value))
 *
 * Правая скобка может отсутствовать в ошибочном SQL.
 */
public final class ParenthesizedExpression
        extends SqlExpression {

    private final Token<SqlTokenKind> leftParenthesis;
    private final SqlExpression expression;
    private final Token<SqlTokenKind> rightParenthesis;

    public ParenthesizedExpression(
            Token<SqlTokenKind> leftParenthesis,
            SqlExpression expression,
            Token<SqlTokenKind> rightParenthesis
    ) {
        super(createRange(
                leftParenthesis,
                expression,
                rightParenthesis
        ));

        this.leftParenthesis = leftParenthesis;
        this.expression = expression;
        this.rightParenthesis = rightParenthesis;
    }

    public Token<SqlTokenKind> leftParenthesis() {
        return leftParenthesis;
    }

    public SqlExpression expression() {
        return expression;
    }

    /**
     * Может вернуть null для незакрытого выражения.
     */
    public Token<SqlTokenKind> rightParenthesis() {
        return rightParenthesis;
    }

    public boolean hasRightParenthesis() {
        return rightParenthesis != null;
    }

    private static TextRange createRange(
            Token<SqlTokenKind> leftParenthesis,
            SqlExpression expression,
            Token<SqlTokenKind> rightParenthesis
    ) {
        requireKind(
                leftParenthesis,
                SqlTokenKind.LEFT_PARENTHESIS,
                "leftParenthesis"
        );

        Objects.requireNonNull(expression, "expression");

        if (rightParenthesis != null) {
            requireKind(
                    rightParenthesis,
                    SqlTokenKind.RIGHT_PARENTHESIS,
                    "rightParenthesis"
            );
        }

        int end = rightParenthesis != null
                ? rightParenthesis.range().end()
                : expression.end();

        return new TextRange(
                leftParenthesis.range().start(),
                end
        );
    }

    private static void requireKind(
            Token<SqlTokenKind> token,
            SqlTokenKind expectedKind,
            String name
    ) {
        Objects.requireNonNull(token, name);

        if (token.kind() != expectedKind) {
            throw new IllegalArgumentException(
                    name
                            + " must be "
                            + expectedKind
                            + ", found "
                            + token.kind()
            );
        }
    }
}