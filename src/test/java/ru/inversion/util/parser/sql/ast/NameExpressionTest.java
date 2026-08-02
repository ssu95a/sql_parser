package ru.inversion.util.parser.sql.ast;

import org.junit.Test;
import ru.inversion.util.parser.lexer.LexerResult;
import ru.inversion.util.parser.lexer.Token;
import ru.inversion.util.parser.sql.lexer.SqlLexer;
import ru.inversion.util.parser.sql.lexer.SqlTokenKind;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class NameExpressionTest {

    @Test
    public void wordMustBeAccepted() {
        Token<SqlTokenKind> token =
                token("customer_id");

        NameExpression expression =
                new NameExpression(token);

        assertSame(token, expression.token());

        assertEquals(
                SqlTokenKind.WORD,
                expression.nameKind()
        );

        assertFalse(expression.isQuoted());
        assertEquals(token.range(), expression.range());
    }

    @Test
    public void quotedIdentifierMustBeAccepted() {
        Token<SqlTokenKind> token =
                token("\"Customer Name\"");

        NameExpression expression =
                new NameExpression(token);

        assertSame(token, expression.token());

        assertEquals(
                SqlTokenKind.QUOTED_IDENTIFIER,
                expression.nameKind()
        );

        assertTrue(expression.isQuoted());
        assertEquals(token.range(), expression.range());
    }

    @Test
    public void quotedKeywordMustBeAccepted() {
        Token<SqlTokenKind> token =
                token("\"select\"");

        NameExpression expression =
                new NameExpression(token);

        assertEquals(
                SqlTokenKind.QUOTED_IDENTIFIER,
                expression.nameKind()
        );

        assertTrue(expression.isQuoted());
    }

    @Test(expected = IllegalArgumentException.class)
    public void literalMustBeRejected() {
        new NameExpression(
                token("123")
        );
    }

    @Test(expected = IllegalArgumentException.class)
    public void parameterMustBeRejected() {
        new NameExpression(
                token(":customerId")
        );
    }

    @Test(expected = IllegalArgumentException.class)
    public void operatorMustBeRejected() {
        new NameExpression(
                token("+")
        );
    }

    @Test(expected = NullPointerException.class)
    public void tokenMustNotBeNull() {
        new NameExpression(null);
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