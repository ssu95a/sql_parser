package ru.inversion.util.parser.sql.ast;

import org.junit.Test;
import ru.inversion.util.parser.lexer.LexerResult;
import ru.inversion.util.parser.lexer.Token;
import ru.inversion.util.parser.sql.lexer.SqlLexer;
import ru.inversion.util.parser.sql.lexer.SqlTokenKind;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

public class UnaryExpressionTest {

    @Test
    public void expressionMustContainOperatorAndOperand() {
        LexerResult<SqlTokenKind> lexerResult =
                new SqlLexer().tokenize("-123");

        Token<SqlTokenKind> operator =
                lexerResult.tokens().get(0);

        Token<SqlTokenKind> literal =
                lexerResult.tokens().get(1);

        LiteralExpression operand =
                new LiteralExpression(literal);

        UnaryExpression expression =
                new UnaryExpression(
                        operator,
                        operand
                );

        assertSame(operator, expression.operator());
        assertSame(operand, expression.operand());

        assertEquals(0, expression.start());
        assertEquals(4, expression.end());
    }

    @Test(expected = IllegalArgumentException.class)
    public void operatorTokenMustBeOperator() {
        LexerResult<SqlTokenKind> lexerResult =
                new SqlLexer().tokenize("value 123");

        new UnaryExpression(
                lexerResult.tokens().get(0),
                new LiteralExpression(
                        lexerResult.tokens().get(2)
                )
        );
    }

    @Test(expected = NullPointerException.class)
    public void operatorMustNotBeNull() {
        new UnaryExpression(
                null,
                literal("123")
        );
    }

    @Test(expected = NullPointerException.class)
    public void operandMustNotBeNull() {
        Token<SqlTokenKind> operator =
                new SqlLexer()
                        .tokenize("-")
                        .tokens()
                        .get(0);

        new UnaryExpression(operator, null);
    }

    private static LiteralExpression literal(
            String sql
    ) {
        return new LiteralExpression(
                new SqlLexer()
                        .tokenize(sql)
                        .tokens()
                        .get(0)
        );
    }
}