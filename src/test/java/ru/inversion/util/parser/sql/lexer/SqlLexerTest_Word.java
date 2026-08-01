package ru.inversion.util.parser.sql.lexer;

import org.junit.Test;
import ru.inversion.util.parser.lexer.LexerResult;

import static org.junit.Assert.assertEquals;

/**
 * Базовые контрактные тесты SQL lexer-а.
 */
public class SqlLexerTest_Word {

    @Test
    public void wordMustBeSingleToken() {
        assertToken("select", SqlTokenKind.WORD);
    }

    @Test
    public void wordMayContainDigitsUnderscoreAndDollar() {
        assertToken("_table_12$value", SqlTokenKind.WORD);
    }

    @Test
    public void dollarMustNotStartWord() {
        LexerResult<SqlTokenKind> result =
                new SqlLexer().tokenize("$value");

        assertEquals(SqlTokenKind.UNKNOWN, result.tokens().get(0).kind());
        assertEquals("$", result.text(result.tokens().get(0)));

        assertEquals(SqlTokenKind.WORD, result.tokens().get(1).kind());
        assertEquals("value", result.text(result.tokens().get(1)));
    }

    private static void assertToken(
            String source,
            SqlTokenKind expectedKind
    ) {
        LexerResult<SqlTokenKind> result =
                new SqlLexer().tokenize(source);

        assertEquals(2, result.tokens().size());
        assertEquals(expectedKind, result.tokens().get(0).kind());
        assertEquals(source, result.text(result.tokens().get(0)));
        assertEquals(
                SqlTokenKind.END_OF_FILE,
                result.tokens().get(1).kind()
        );
    }
}