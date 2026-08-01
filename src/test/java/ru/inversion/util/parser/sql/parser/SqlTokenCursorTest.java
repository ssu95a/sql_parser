package ru.inversion.util.parser.sql.parser;

import org.junit.Test;
import ru.inversion.util.parser.lexer.Token;
import ru.inversion.util.parser.sql.lexer.SqlLexer;
import ru.inversion.util.parser.sql.lexer.SqlTokenKind;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class SqlTokenCursorTest {

    @Test
    public void currentMustSkipLeadingTrivia() {
        SqlTokenCursor cursor = cursor(
                " \t/* comment */ SELECT"
        );

        assertEquals(
                SqlTokenKind.WORD,
                cursor.current().kind()
        );

        assertTrue(cursor.isWord("select"));
    }

    @Test
    public void consumeMustSkipTriviaBetweenTokens() {
        SqlTokenCursor cursor = cursor(
                "select  /* comment */  value"
        );

        assertTrue(cursor.isWord("select"));

        Token<SqlTokenKind> consumed =
                cursor.consume();

        assertEquals(
                SqlTokenKind.WORD,
                consumed.kind()
        );

        assertTrue(cursor.isWord("value"));
    }

    @Test
    public void wordComparisonMustIgnoreCase() {
        SqlTokenCursor cursor = cursor("SeLeCt");

        assertTrue(cursor.isWord("select"));
        assertTrue(cursor.isWord("SELECT"));
        assertFalse(cursor.isWord("from"));
    }

    @Test
    public void nonWordMustNotMatchKeyword() {
        SqlTokenCursor cursor = cursor("'select'");

        assertEquals(
                SqlTokenKind.STRING_LITERAL,
                cursor.current().kind()
        );

        assertFalse(cursor.isWord("select"));
    }

    @Test
    public void peekMustSkipTrivia() {
        SqlTokenCursor cursor = cursor(
                "select /* one */ value -- two\n from"
        );

        assertTrue(cursor.isWord("select"));

        assertEquals(
                SqlTokenKind.WORD,
                cursor.peek(1).kind()
        );

        assertEquals(
                "value",
                cursor.result().text(cursor.peek(1))
        );

        assertEquals(
                "from",
                cursor.result().text(cursor.peek(2))
        );
    }

    @Test
    public void peekZeroMustReturnCurrentToken() {
        SqlTokenCursor cursor = cursor("select");

        assertSame(
                cursor.current(),
                cursor.peek(0)
        );
    }

    @Test
    public void cursorMustReachEndOfFile() {
        SqlTokenCursor cursor = cursor("select");

        assertFalse(cursor.isEnd());

        cursor.consume();

        assertTrue(cursor.isEnd());
        assertEquals(
                SqlTokenKind.END_OF_FILE,
                cursor.current().kind()
        );
    }

    @Test
    public void consumingEndOfFileMustNotAdvanceCursor() {
        SqlTokenCursor cursor = cursor("");

        Token<SqlTokenKind> first =
                cursor.consume();

        Token<SqlTokenKind> second =
                cursor.consume();

        assertSame(first, second);
        assertTrue(cursor.isEnd());
    }

    @Test
    public void peekBeyondEndMustReturnEndOfFile() {
        SqlTokenCursor cursor = cursor(
                "select from"
        );

        assertEquals(
                SqlTokenKind.END_OF_FILE,
                cursor.peek(2).kind()
        );

        assertEquals(
                SqlTokenKind.END_OF_FILE,
                cursor.peek(100).kind()
        );
    }

    @Test
    public void onlyTriviaMustLeadToEndOfFile() {
        SqlTokenCursor cursor = cursor(
                " \t/* comment */-- line\n"
        );

        assertTrue(cursor.isEnd());
    }

    @Test(expected = IllegalArgumentException.class)
    public void negativePeekDistanceMustBeRejected() {
        cursor("select").peek(-1);
    }

    private static SqlTokenCursor cursor(String sql) {
        return new SqlTokenCursor(
                new SqlLexer().tokenize(sql)
        );
    }

    @Test
    public void consumeIfMustConsumeMatchingToken() {
        SqlTokenCursor cursor = cursor("(value)");

        assertTrue(
                cursor.consumeIf(
                        SqlTokenKind.LEFT_PARENTHESIS
                )
        );

        assertTrue(cursor.isWord("value"));
    }

    @Test
    public void consumeIfMustNotConsumeDifferentToken() {
        SqlTokenCursor cursor = cursor("value");

        assertFalse(
                cursor.consumeIf(
                        SqlTokenKind.LEFT_PARENTHESIS
                )
        );

        assertTrue(cursor.isWord("value"));
    }

    @Test
    public void consumeIfMustSkipFollowingTrivia() {
        SqlTokenCursor cursor = cursor(
                "(  /* comment */ value"
        );

        assertTrue(
                cursor.consumeIf(
                        SqlTokenKind.LEFT_PARENTHESIS
                )
        );

        assertTrue(cursor.isWord("value"));
    }

    @Test
    public void consumeWordIfMustConsumeMatchingWord() {
        SqlTokenCursor cursor = cursor(
                "SELECT value"
        );

        assertTrue(cursor.consumeWordIf("select"));
        assertTrue(cursor.isWord("value"));
    }

    @Test
    public void consumeWordIfMustIgnoreCase() {
        SqlTokenCursor cursor = cursor(
                "SeLeCt value"
        );

        assertTrue(cursor.consumeWordIf("SELECT"));
        assertTrue(cursor.isWord("value"));
    }

    @Test
    public void consumeWordIfMustNotConsumeDifferentWord() {
        SqlTokenCursor cursor = cursor(
                "select value"
        );

        assertFalse(cursor.consumeWordIf("from"));
        assertTrue(cursor.isWord("select"));
    }

    @Test
    public void consumeWordIfMustNotConsumeNonWord() {
        SqlTokenCursor cursor = cursor(
                "'select' value"
        );

        assertFalse(cursor.consumeWordIf("select"));

        assertEquals(
                SqlTokenKind.STRING_LITERAL,
                cursor.current().kind()
        );
    }

    @Test
    public void consumeEndOfFileIfMustRemainAtEnd() {
        SqlTokenCursor cursor = cursor("");

        assertTrue(
                cursor.consumeIf(
                        SqlTokenKind.END_OF_FILE
                )
        );

        assertTrue(cursor.isEnd());
    }
}