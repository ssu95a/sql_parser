package ru.inversion.util.parser.sql.lexer.recognizer;

import org.junit.Test;
import ru.inversion.util.parser.lexer.LexerResult;
import ru.inversion.util.parser.lexer.Token;
import ru.inversion.util.parser.lexer.TokenMatch;
import ru.inversion.util.parser.sql.lexer.SqlLexer;
import ru.inversion.util.parser.sql.lexer.SqlTokenKind;
import ru.inversion.util.parser.text.SourceText;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class OracleQQuotedStringRecognizerTest {

    private final OracleQQuotedStringRecognizer recognizer =
            new OracleQQuotedStringRecognizer();

    @Test
    public void recognizesSquareBracketDelimiter() {
        assertRecognized(
                "q'[text]'"
        );
    }

    @Test
    public void recognizesCurlyBracketDelimiter() {
        assertRecognized(
                "q'{text}'"
        );
    }

    @Test
    public void recognizesParenthesisDelimiter() {
        assertRecognized(
                "q'(text)'"
        );
    }

    @Test
    public void recognizesAngleBracketDelimiter() {
        assertRecognized(
                "q'<text>'"
        );
    }

    @Test
    public void recognizesSameCharacterDelimiter() {
        assertRecognized(
                "q'!text!'"
        );
    }

    @Test
    public void recognizesUpperCasePrefix() {
        assertRecognized(
                "Q'[text]'"
        );
    }

    @Test
    public void recognizesEmptyContent() {
        assertRecognized(
                "q'[]'"
        );
    }

    @Test
    public void allowsSingleQuotesInsideContent() {
        assertRecognized(
                "q'[John's car]'"
        );
    }

    @Test
    public void allowsOpeningDelimiterInsideContent() {
        assertRecognized(
                "q'[array[index]]'"
        );
    }

    @Test
    public void allowsClosingDelimiterWhenNotFollowedByQuote() {
        assertRecognized(
                "q'!first!second!'"
        );
    }

    @Test
    public void preservesArbitrarySqlContent() {
        String literal =
                "q'{\n"
                        + "    select 'text'\n"
                        + "    from table_name\n"
                        + "    where value = 10\n"
                        + "}'";

        assertRecognized(literal);
    }

    @Test
    public void recognizesLiteralAtNonZeroOffset() {
        String text =
                "select q'[text]' from t";

        int offset =
                text.indexOf("q'");

        TokenMatch<SqlTokenKind> match =
                recognizer.match(
                        new SourceText(text),
                        offset
                );

        assertNotNull(match);

        assertEquals(
                SqlTokenKind.ORACLE_Q_QUOTED_STRING,
                match.kind()
        );

        assertEquals(
                text.indexOf(" from t"),
                match.endOffset()
        );
    }

    @Test
    public void stopsBeforeFollowingToken() {
        String text =
                "q'[text]'tail";

        TokenMatch<SqlTokenKind> match =
                recognizer.match(
                        new SourceText(text),
                        0
                );

        assertNotNull(match);

        assertEquals(
                text.indexOf("tail"),
                match.endOffset()
        );
    }

    @Test
    public void unterminatedSquareBracketStringContinuesToEnd() {
        String text =
                "q'[unterminated";

        TokenMatch<SqlTokenKind> match =
                recognizer.match(
                        new SourceText(text),
                        0
                );

        assertNotNull(match);

        assertEquals(
                text.length(),
                match.endOffset()
        );
    }

    @Test
    public void unterminatedSameDelimiterStringContinuesToEnd() {
        String text =
                "q'!unterminated";

        TokenMatch<SqlTokenKind> match =
                recognizer.match(
                        new SourceText(text),
                        0
                );

        assertNotNull(match);

        assertEquals(
                text.length(),
                match.endOffset()
        );
    }

    @Test
    public void mismatchedPairedDelimiterDoesNotCloseLiteral() {
        String text =
                "q'[text}'";

        TokenMatch<SqlTokenKind> match =
                recognizer.match(
                        new SourceText(text),
                        0
                );

        assertNotNull(match);

        assertEquals(
                text.length(),
                match.endOffset()
        );
    }

    @Test
    public void doesNotRecognizeOrdinaryStringLiteral() {
        assertNotRecognized(
                "'text'"
        );
    }

    @Test
    public void doesNotRecognizeOrdinaryWord() {
        assertNotRecognized(
                "query"
        );
    }

    @Test
    public void doesNotRecognizeQWithoutQuote() {
        assertNotRecognized(
                "q[text]"
        );
    }

    @Test
    public void doesNotRecognizeSpaceDelimiter() {
        assertNotRecognized(
                "q' text '"
        );
    }

    @Test
    public void doesNotRecognizeTabDelimiter() {
        assertNotRecognized(
                "q'\ttext\t'"
        );
    }

    @Test
    public void lexerRecognizesWholeOracleQuotedString() {
        String sql =
                "select q'[John's car]' from t";

        LexerResult<SqlTokenKind> result =
                new SqlLexer().tokenize(sql);

        List<Token<SqlTokenKind>> tokens =
                result.tokens();

        assertEquals(
                SqlTokenKind.WORD,
                tokens.get(0).kind()
        );

        assertEquals(
                SqlTokenKind.WHITESPACE,
                tokens.get(1).kind()
        );

        assertEquals(
                SqlTokenKind.ORACLE_Q_QUOTED_STRING,
                tokens.get(2).kind()
        );

        assertEquals(
                "q'[John's car]'",
                result.text(
                        tokens.get(2)
                )
        );

        assertEquals(
                SqlTokenKind.WHITESPACE,
                tokens.get(3).kind()
        );

        assertEquals(
                SqlTokenKind.WORD,
                tokens.get(4).kind()
        );
    }

    @Test
    public void lexerKeepsOrdinaryQWordAsWord() {
        LexerResult<SqlTokenKind> result =
                new SqlLexer().tokenize(
                        "query"
                );

        Token<SqlTokenKind> token =
                result.tokens().get(0);

        assertEquals(
                SqlTokenKind.WORD,
                token.kind()
        );

        assertEquals(
                "query",
                result.text(token)
        );
    }

    @Test(expected = NullPointerException.class)
    public void rejectsNullSource() {
        recognizer.match(
                null,
                0
        );
    }

    private void assertRecognized(
            String text
    ) {
        TokenMatch<SqlTokenKind> match =
                recognizer.match(
                        new SourceText(text),
                        0
                );

        assertNotNull(match);

        assertEquals(
                SqlTokenKind.ORACLE_Q_QUOTED_STRING,
                match.kind()
        );

        assertEquals(
                text.length(),
                match.endOffset()
        );
    }

    private void assertNotRecognized(
            String text
    ) {
        TokenMatch<SqlTokenKind> match =
                recognizer.match(
                        new SourceText(text),
                        0
                );

        assertNull(match);
    }
}