package ru.inversion.util.parser.sql.ast;

import org.junit.Test;
import ru.inversion.util.parser.lexer.LexerResult;
import ru.inversion.util.parser.lexer.Token;
import ru.inversion.util.parser.sql.lexer.SqlLexer;
import ru.inversion.util.parser.sql.lexer.SqlTokenKind;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

public class LiteralExpressionTest {

    @Test
    public void integerLiteralMustBeAccepted() {
        Token<SqlTokenKind> token =
                token("123");

        LiteralExpression expression =
                new LiteralExpression(token);

        assertSame(token, expression.token());

        assertEquals(
                SqlTokenKind.INTEGER_LITERAL,
                expression.literalKind()
        );

        assertEquals(token.range(), expression.range());
        assertEquals(0, expression.start());
        assertEquals(3, expression.end());
    }

    @Test
    public void decimalLiteralMustBeAccepted() {
        Token<SqlTokenKind> token =
                token("123.45");

        LiteralExpression expression =
                new LiteralExpression(token);

        assertEquals(
                SqlTokenKind.DECIMAL_LITERAL,
                expression.literalKind()
        );

        assertEquals(token.range(), expression.range());
    }

    @Test
    public void stringLiteralMustBeAccepted() {
        Token<SqlTokenKind> token =
                token("'some text'");

        LiteralExpression expression =
                new LiteralExpression(token);

        assertEquals(
                SqlTokenKind.STRING_LITERAL,
                expression.literalKind()
        );

        assertSame(token, expression.token());
    }

    @Test(expected = IllegalArgumentException.class)
    public void wordMustBeRejected() {
        new LiteralExpression(
                token("value")
        );
    }

    @Test(expected = IllegalArgumentException.class)
    public void parameterMustBeRejected() {
        new LiteralExpression(
                token(":value")
        );
    }

    @Test(expected = NullPointerException.class)
    public void tokenMustNotBeNull() {
        new LiteralExpression(null);
    }

    private static Token<SqlTokenKind> token(
            String source
    ) {
        LexerResult<SqlTokenKind> result =
                new SqlLexer().tokenize(source);

        assertEquals(2, result.tokens().size());

        return result.tokens().get(0);
    }
}