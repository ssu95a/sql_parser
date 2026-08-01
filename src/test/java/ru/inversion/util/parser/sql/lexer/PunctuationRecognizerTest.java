package ru.inversion.util.parser.sql.lexer;

import org.junit.Test;
import ru.inversion.util.parser.lexer.LexerResult;
import ru.inversion.util.parser.lexer.Token;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class PunctuationRecognizerTest {

    @Test
    public void leftParenthesisMustBeRecognized() {
        assertToken(
                "(",
                SqlTokenKind.LEFT_PARENTHESIS
        );
    }

    @Test
    public void rightParenthesisMustBeRecognized() {
        assertToken(
                ")",
                SqlTokenKind.RIGHT_PARENTHESIS
        );
    }

    @Test
    public void leftBracketMustBeRecognized() {
        assertToken(
                "[",
                SqlTokenKind.LEFT_BRACKET
        );
    }

    @Test
    public void rightBracketMustBeRecognized() {
        assertToken(
                "]",
                SqlTokenKind.RIGHT_BRACKET
        );
    }

    @Test
    public void leftBraceMustBeRecognized() {
        assertToken(
                "{",
                SqlTokenKind.LEFT_BRACE
        );
    }

    @Test
    public void rightBraceMustBeRecognized() {
        assertToken(
                "}",
                SqlTokenKind.RIGHT_BRACE
        );
    }

    @Test
    public void commaMustBeRecognized() {
        assertToken(
                ",",
                SqlTokenKind.COMMA
        );
    }

    @Test
    public void dotMustBeRecognized() {
        assertToken(
                ".",
                SqlTokenKind.DOT
        );
    }

    @Test
    public void semicolonMustBeRecognized() {
        assertToken(
                ";",
                SqlTokenKind.SEMICOLON
        );
    }

    @Test
    public void punctuationSequenceMustBeRecognized() {
        String sql = "([{}]),.;";

        LexerResult<SqlTokenKind> result =
                new SqlLexer().tokenize(sql);

        List<Token<SqlTokenKind>> tokens = result.tokens();

        assertEquals(10, tokens.size());

        assertToken(
                result,
                0,
                SqlTokenKind.LEFT_PARENTHESIS,
                "("
        );

        assertToken(
                result,
                1,
                SqlTokenKind.LEFT_BRACKET,
                "["
        );

        assertToken(
                result,
                2,
                SqlTokenKind.LEFT_BRACE,
                "{"
        );

        assertToken(
                result,
                3,
                SqlTokenKind.RIGHT_BRACE,
                "}"
        );

        assertToken(
                result,
                4,
                SqlTokenKind.RIGHT_BRACKET,
                "]"
        );

        assertToken(
                result,
                5,
                SqlTokenKind.RIGHT_PARENTHESIS,
                ")"
        );

        assertToken(
                result,
                6,
                SqlTokenKind.COMMA,
                ","
        );

        assertToken(
                result,
                7,
                SqlTokenKind.DOT,
                "."
        );

        assertToken(
                result,
                8,
                SqlTokenKind.SEMICOLON,
                ";"
        );

        assertEquals(
                SqlTokenKind.END_OF_FILE,
                tokens.get(9).kind()
        );
    }

    /**
     * Проверяет конфликт точки и десятичного литерала.
     *
     * LexerEngine должен выбрать самое длинное совпадение:
     * NumberRecognizer возвращает ".45",
     * FixedTextRecognizer возвращает только ".".
     */
    @Test
    public void decimalMustWinOverDot() {
        assertToken(
                ".45",
                SqlTokenKind.DECIMAL_LITERAL
        );
    }

    @Test
    public void qualifiedNameMustContainDotToken() {
        String sql = "schema.table";

        LexerResult<SqlTokenKind> result =
                new SqlLexer().tokenize(sql);

        assertEquals(4, result.tokens().size());

        assertToken(
                result,
                0,
                SqlTokenKind.WORD,
                "schema"
        );

        assertToken(
                result,
                1,
                SqlTokenKind.DOT,
                "."
        );

        assertToken(
                result,
                2,
                SqlTokenKind.WORD,
                "table"
        );

        assertEquals(
                SqlTokenKind.END_OF_FILE,
                result.tokens().get(3).kind()
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