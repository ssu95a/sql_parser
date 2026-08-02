package ru.inversion.util.parser.sql.ast;

import org.junit.Test;
import ru.inversion.util.parser.lexer.LexerResult;
import ru.inversion.util.parser.lexer.Token;
import ru.inversion.util.parser.sql.lexer.SqlLexer;
import ru.inversion.util.parser.sql.lexer.SqlTokenKind;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

public class ParameterExpressionTest {

    @Test
    public void jdbcParameterMustBeAccepted() {
        Token<SqlTokenKind> token =
                token("?");

        ParameterExpression expression =
                new ParameterExpression(token);

        assertSame(token, expression.token());

        assertEquals(
                SqlTokenKind.JDBC_PARAMETER,
                expression.parameterKind()
        );

        assertEquals(token.range(), expression.range());
        assertEquals(0, expression.start());
        assertEquals(1, expression.end());
    }

    @Test
    public void namedParameterMustBeAccepted() {
        Token<SqlTokenKind> token =
                token(":customerId");

        ParameterExpression expression =
                new ParameterExpression(token);

        assertEquals(
                SqlTokenKind.NAMED_PARAMETER,
                expression.parameterKind()
        );

        assertSame(token, expression.token());
        assertEquals(token.range(), expression.range());
    }

    @Test
    public void postgresParameterMustBeAccepted() {
        Token<SqlTokenKind> token =
                token("$25");

        ParameterExpression expression =
                new ParameterExpression(token);

        assertEquals(
                SqlTokenKind.POSTGRES_POSITIONAL_PARAMETER,
                expression.parameterKind()
        );

        assertEquals(token.range(), expression.range());
    }

    @Test(expected = IllegalArgumentException.class)
    public void literalMustBeRejected() {
        new ParameterExpression(
                token("123")
        );
    }

    @Test(expected = IllegalArgumentException.class)
    public void wordMustBeRejected() {
        new ParameterExpression(
                token("value")
        );
    }

    @Test(expected = NullPointerException.class)
    public void tokenMustNotBeNull() {
        new ParameterExpression(null);
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