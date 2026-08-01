package ru.inversion.util.parser.sql.lexer;

import org.junit.Test;
import ru.inversion.util.parser.lexer.LexerResult;
import ru.inversion.util.parser.lexer.Token;
import ru.inversion.util.parser.text.TextRange;

import static org.junit.Assert.assertEquals;

/**
 * Базовые контрактные тесты SQL lexer-а.
 */
public class SqlLexerTest_Str {

    @Test
    public void emptyStringMustBeSingleToken() {
        assertToken(
                "''",
                SqlTokenKind.STRING_LITERAL
        );
    }

    @Test
    public void stringMustBeSingleToken() {
        assertToken(
                "'some text'",
                SqlTokenKind.STRING_LITERAL
        );
    }

    @Test
    public void doubledQuoteMustRemainInsideString() {
        String sql = "'John''s car'";

        LexerResult<SqlTokenKind> result =
                new SqlLexer().tokenize(sql);

        assertEquals(2, result.tokens().size());

        Token<SqlTokenKind> string = result.tokens().get(0);

        assertEquals(
                SqlTokenKind.STRING_LITERAL,
                string.kind()
        );

        assertEquals(sql, result.text(string));
    }

    @Test
    public void stringMayContainLineBreaks() {
        String sql = "'first\r\nsecond\nthird'";

        LexerResult<SqlTokenKind> result =
                new SqlLexer().tokenize(sql);

        assertEquals(
                SqlTokenKind.STRING_LITERAL,
                result.tokens().get(0).kind()
        );

        assertEquals(
                sql,
                result.text(result.tokens().get(0))
        );
    }

    @Test
    public void unclosedStringMustContinueToEndOfSource() {
        String sql = "'unclosed text";

        LexerResult<SqlTokenKind> result =
                new SqlLexer().tokenize(sql);

        Token<SqlTokenKind> string = result.tokens().get(0);

        assertEquals(
                SqlTokenKind.STRING_LITERAL,
                string.kind()
        );

        assertEquals(
                new TextRange(0, sql.length()),
                string.range()
        );

        assertEquals(sql, result.text(string));
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