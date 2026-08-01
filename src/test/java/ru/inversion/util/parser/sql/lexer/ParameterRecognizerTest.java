package ru.inversion.util.parser.sql.lexer;

import org.junit.Test;
import ru.inversion.util.parser.lexer.LexerResult;
import ru.inversion.util.parser.lexer.Token;

import static org.junit.Assert.assertEquals;

public class ParameterRecognizerTest {

    @Test
    public void jdbcParameterMustBeRecognized() {
        assertToken(
                "?",
                SqlTokenKind.JDBC_PARAMETER
        );
    }

    @Test
    public void namedParameterMustBeRecognized() {
        assertToken(
                ":name",
                SqlTokenKind.NAMED_PARAMETER
        );
    }

    @Test
    public void namedParameterMayStartWithUnderscore() {
        assertToken(
                ":_name2",
                SqlTokenKind.NAMED_PARAMETER
        );
    }

    @Test
    public void namedParameterMayContainDollar() {
        assertToken(
                ":user$id",
                SqlTokenKind.NAMED_PARAMETER
        );
    }

    @Test
    public void digitMustNotStartNamedParameter() {
        LexerResult<SqlTokenKind> result =
                new SqlLexer().tokenize(":1");

        assertEquals(
                SqlTokenKind.UNKNOWN,
                result.tokens().get(0).kind()
        );

        assertEquals(
                ":",
                result.text(result.tokens().get(0))
        );

        assertEquals(
                SqlTokenKind.INTEGER_LITERAL,
                result.tokens().get(1).kind()
        );

        assertEquals(
                "1",
                result.text(result.tokens().get(1))
        );
    }

    @Test
    public void postgresParameterMustBeRecognized() {
        assertToken(
                "$1",
                SqlTokenKind.POSTGRES_POSITIONAL_PARAMETER
        );
    }

    @Test
    public void postgresParameterMayContainSeveralDigits() {
        assertToken(
                "$123",
                SqlTokenKind.POSTGRES_POSITIONAL_PARAMETER
        );
    }

    @Test
    public void dollarWithoutDigitsMustNotBeParameter() {
        LexerResult<SqlTokenKind> result =
                new SqlLexer().tokenize("$value");

        assertEquals(
                SqlTokenKind.UNKNOWN,
                result.tokens().get(0).kind()
        );

        assertEquals(
                "$",
                result.text(result.tokens().get(0))
        );

        assertEquals(
                SqlTokenKind.WORD,
                result.tokens().get(1).kind()
        );

        assertEquals(
                "value",
                result.text(result.tokens().get(1))
        );
    }

    @Test
    public void postgresCastMustRemainOperator() {
        assertToken(
                "::",
                SqlTokenKind.OPERATOR
        );
    }

    @Test
    public void assignmentMustRemainOperator() {
        assertToken(
                ":=",
                SqlTokenKind.OPERATOR
        );
    }

    @Test
    public void parametersInsideExpressionMustRemainSeparate() {
        String sql = ":left + $2 = ?";

        LexerResult<SqlTokenKind> result =
                new SqlLexer().tokenize(sql);

        assertToken(
                result,
                0,
                SqlTokenKind.NAMED_PARAMETER,
                ":left"
        );

        assertToken(
                result,
                2,
                SqlTokenKind.OPERATOR,
                "+"
        );

        assertToken(
                result,
                4,
                SqlTokenKind.POSTGRES_POSITIONAL_PARAMETER,
                "$2"
        );

        assertToken(
                result,
                6,
                SqlTokenKind.OPERATOR,
                "="
        );

        assertToken(
                result,
                8,
                SqlTokenKind.JDBC_PARAMETER,
                "?"
        );
    }

    private static void assertToken(
            String source,
            SqlTokenKind expectedKind
    ) {
        LexerResult<SqlTokenKind> result =
                new SqlLexer().tokenize(source);

        assertEquals(2, result.tokens().size());

        assertEquals(
                expectedKind,
                result.tokens().get(0).kind()
        );

        assertEquals(
                source,
                result.text(result.tokens().get(0))
        );

        assertEquals(
                SqlTokenKind.END_OF_FILE,
                result.tokens().get(1).kind()
        );
    }

    private static void assertToken(
            LexerResult<SqlTokenKind> result,
            int index,
            SqlTokenKind expectedKind,
            String expectedText
    ) {
        Token<SqlTokenKind> token =
                result.tokens().get(index);

        assertEquals(expectedKind, token.kind());
        assertEquals(expectedText, result.text(token));
    }
}