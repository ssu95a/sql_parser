package ru.inversion.util.parser.sql.lexer;

import org.junit.Test;
import ru.inversion.util.parser.lexer.LexerResult;

import static org.junit.Assert.assertEquals;

/**
 * Базовые контрактные тесты SQL lexer-а.
 */
public class NumberRecognizerTest {

    @Test
    public void integerMustBeSingleToken() {
        assertToken(
                "123",
                SqlTokenKind.INTEGER_LITERAL
        );
    }

    @Test
    public void decimalWithIntegerPartMustBeSingleToken() {
        assertToken(
                "123.45",
                SqlTokenKind.DECIMAL_LITERAL
        );
    }

    @Test
    public void decimalWithoutIntegerPartMustBeSingleToken() {
        assertToken(
                ".45",
                SqlTokenKind.DECIMAL_LITERAL
        );
    }

    @Test
    public void decimalWithoutFractionPartMustBeSingleToken() {
        assertToken(
                "123.",
                SqlTokenKind.DECIMAL_LITERAL
        );
    }

    @Test
    public void singleDotMustNotBeNumber() {
        LexerResult<SqlTokenKind> result =
                new SqlLexer().tokenize(".");

        assertEquals(
                SqlTokenKind.UNKNOWN,
                result.tokens().get(0).kind()
        );

        assertEquals(
                ".",
                result.text(result.tokens().get(0))
        );
    }

    @Test
    public void doubleDotMustNotBePartOfNumber() {
        LexerResult<SqlTokenKind> result =
                new SqlLexer().tokenize("123..45");

        assertEquals(
                SqlTokenKind.INTEGER_LITERAL,
                result.tokens().get(0).kind()
        );

        assertEquals(
                "123",
                result.text(result.tokens().get(0))
        );
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