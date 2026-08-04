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

public class PostgresDollarQuotedStringRecognizerTest {

    private final PostgresDollarQuotedStringRecognizer
            recognizer =
            new PostgresDollarQuotedStringRecognizer();

    @Test
    public void recognizesEmptyUntaggedString() {
        assertRecognized(
                "$$$$"
        );
    }

    @Test
    public void recognizesUntaggedString() {
        assertRecognized(
                "$$text$$"
        );
    }

    @Test
    public void recognizesNamedTag() {
        assertRecognized(
                "$body$text$body$"
        );
    }

    @Test
    public void recognizesTagStartingWithUnderscore() {
        assertRecognized(
                "$_body$text$_body$"
        );
    }

    @Test
    public void recognizesDigitsAfterFirstTagCharacter() {
        assertRecognized(
                "$body123$text$body123$"
        );
    }

    @Test
    public void recognizesUpperCaseTag() {
        assertRecognized(
                "$Body_TEXT123$text$Body_TEXT123$"
        );
    }

    @Test
    public void preservesArbitraryContent() {
        String literal =
                "$body$\n"
                        + "    'quoted text'\n"
                        + "    \"quoted identifier\"\n"
                        + "    -- line comment\n"
                        + "    /* block comment */\n"
                        + "    123\n"
                        + "$body$";

        assertRecognized(literal);
    }

    @Test
    public void ignoresDifferentTagInsideContent() {
        String literal =
                "$outer$"
                        + "before "
                        + "$inner$text$inner$ "
                        + "after"
                        + "$outer$";

        assertRecognized(literal);
    }

    @Test
    public void closesOnlyWithMatchingTag() {
        String text =
                "$outer$"
                        + "value"
                        + "$different$"
                        + "more"
                        + "$outer$"
                        + "tail";

        TokenMatch<SqlTokenKind> match =
                recognizer.match(
                        new SourceText(text),
                        0
                );

        assertNotNull(match);

        assertEquals(
                SqlTokenKind
                        .POSTGRES_DOLLAR_QUOTED_STRING,
                match.kind()
        );

        assertEquals(
                text.indexOf("tail"),
                match.endOffset()
        );
    }

    @Test
    public void recognizesLiteralAtNonZeroOffset() {
        String text =
                "select $tag$text$tag$ from t";

        int offset =
                text.indexOf("$tag$");

        TokenMatch<SqlTokenKind> match =
                recognizer.match(
                        new SourceText(text),
                        offset
                );

        assertNotNull(match);

        assertEquals(
                text.indexOf(" from t"),
                match.endOffset()
        );
    }

    @Test
    public void unterminatedUntaggedStringContinuesToEnd() {
        String text =
                "$$unterminated";

        TokenMatch<SqlTokenKind> match =
                recognizer.match(
                        new SourceText(text),
                        0
                );

        assertNotNull(match);

        assertEquals(
                SqlTokenKind
                        .POSTGRES_DOLLAR_QUOTED_STRING,
                match.kind()
        );

        assertEquals(
                text.length(),
                match.endOffset()
        );
    }

    @Test
    public void unterminatedTaggedStringContinuesToEnd() {
        String text =
                "$tag$unterminated";

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
    public void doesNotRecognizeOrdinaryText() {
        assertNotRecognized(
                "text"
        );
    }

    @Test
    public void doesNotRecognizeSingleDollar() {
        assertNotRecognized(
                "$"
        );
    }

    @Test
    public void doesNotRecognizeUnfinishedOpeningTag() {
        assertNotRecognized(
                "$tag"
        );
    }

    @Test
    public void doesNotRecognizeTagStartingWithDigit() {
        assertNotRecognized(
                "$1tag$text$1tag$"
        );
    }

    @Test
    public void doesNotRecognizeNumericTag() {
        assertNotRecognized(
                "$1$text$1$"
        );
    }

    @Test
    public void doesNotRecognizeTagContainingHyphen() {
        assertNotRecognized(
                "$some-tag$text$some-tag$"
        );
    }

    @Test
    public void doesNotCapturePostgresPositionalParameter() {
        LexerResult<SqlTokenKind> result =
                new SqlLexer().tokenize(
                        "$1"
                );

        Token<SqlTokenKind> token =
                result.tokens().get(0);

        assertEquals(
                SqlTokenKind
                        .POSTGRES_POSITIONAL_PARAMETER,
                token.kind()
        );

        assertEquals(
                "$1",
                result.text(token)
        );
    }

    @Test
    public void lexerRecognizesDollarQuotedStringBeforeParameter() {
        String sql =
                "$tag$text$tag$ $1";

        LexerResult<SqlTokenKind> result =
                new SqlLexer().tokenize(sql);

        List<Token<SqlTokenKind>> tokens =
                result.tokens();

        assertEquals(
                SqlTokenKind
                        .POSTGRES_DOLLAR_QUOTED_STRING,
                tokens.get(0).kind()
        );

        assertEquals(
                "$tag$text$tag$",
                result.text(tokens.get(0))
        );

        assertEquals(
                SqlTokenKind.WHITESPACE,
                tokens.get(1).kind()
        );

        assertEquals(
                SqlTokenKind
                        .POSTGRES_POSITIONAL_PARAMETER,
                tokens.get(2).kind()
        );

        assertEquals(
                "$1",
                result.text(tokens.get(2))
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
                SqlTokenKind
                        .POSTGRES_DOLLAR_QUOTED_STRING,
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