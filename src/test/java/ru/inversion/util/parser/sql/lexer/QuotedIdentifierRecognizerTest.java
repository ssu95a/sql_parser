package ru.inversion.util.parser.sql.lexer;

import org.junit.Test;
import ru.inversion.util.parser.lexer.LexerResult;
import ru.inversion.util.parser.lexer.Token;
import ru.inversion.util.parser.text.TextRange;

import static org.junit.Assert.assertEquals;

public class QuotedIdentifierRecognizerTest {

    @Test
    public void emptyQuotedIdentifierMustBeSingleToken() {
        assertToken(
                "\"\"",
                SqlTokenKind.QUOTED_IDENTIFIER
        );
    }

    @Test
    public void quotedIdentifierMustBeSingleToken() {
        assertToken(
                "\"Column Name\"",
                SqlTokenKind.QUOTED_IDENTIFIER
        );
    }

    @Test
    public void quotedKeywordMustRemainIdentifier() {
        assertToken(
                "\"select\"",
                SqlTokenKind.QUOTED_IDENTIFIER
        );
    }

    @Test
    public void doubledQuoteMustRemainInsideIdentifier() {
        String sql = "\"Column\"\"Name\"";

        LexerResult<SqlTokenKind> result =
                new SqlLexer().tokenize(sql);

        Token<SqlTokenKind> identifier =
                result.tokens().get(0);

        assertEquals(
                SqlTokenKind.QUOTED_IDENTIFIER,
                identifier.kind()
        );

        assertEquals(sql, result.text(identifier));
    }

    @Test
    public void quotedIdentifierMayContainLineBreaks() {
        String sql = "\"first\r\nsecond\"";

        LexerResult<SqlTokenKind> result =
                new SqlLexer().tokenize(sql);

        assertEquals(
                SqlTokenKind.QUOTED_IDENTIFIER,
                result.tokens().get(0).kind()
        );

        assertEquals(
                sql,
                result.text(result.tokens().get(0))
        );
    }

    @Test
    public void unclosedQuotedIdentifierMustContinueToEndOfSource() {
        String sql = "\"unclosed identifier";

        LexerResult<SqlTokenKind> result =
                new SqlLexer().tokenize(sql);

        Token<SqlTokenKind> identifier =
                result.tokens().get(0);

        assertEquals(
                SqlTokenKind.QUOTED_IDENTIFIER,
                identifier.kind()
        );

        assertEquals(
                new TextRange(0, sql.length()),
                identifier.range()
        );

        assertEquals(sql, result.text(identifier));
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