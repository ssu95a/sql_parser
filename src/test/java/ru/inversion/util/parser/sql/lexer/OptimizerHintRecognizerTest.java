package ru.inversion.util.parser.sql.lexer;

import org.junit.Test;
import ru.inversion.util.parser.lexer.LexerResult;
import ru.inversion.util.parser.lexer.Token;
import ru.inversion.util.parser.lexer.TokenMatch;
import ru.inversion.util.parser.sql.lexer.recognizer
        .OptimizerHintRecognizer;
import ru.inversion.util.parser.text.SourceText;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class OptimizerHintRecognizerTest {

    private final OptimizerHintRecognizer recognizer =
            new OptimizerHintRecognizer();

    @Test
    public void recognizesOptimizerHint() {
        assertRecognized(
                "/*+ INDEX(t IDX_T) */"
        );
    }

    @Test
    public void recognizesEmptyOptimizerHint() {
        assertRecognized(
                "/*+*/"
        );
    }

    @Test
    public void recognizesHintWithoutWhitespace() {
        assertRecognized(
                "/*+INDEX(t IDX_T)*/"
        );
    }

    @Test
    public void recognizesMultilineOptimizerHint() {
        assertRecognized(
                "/*+\n"
                        + "    LEADING(a b)\n"
                        + "    USE_NL(b)\n"
                        + "*/"
        );
    }

    @Test
    public void recognizesHintContainingSpecialCharacters() {
        assertRecognized(
                "/*+ SOME_HINT(~ 100, 'text', :name) */"
        );
    }

    @Test
    public void recognizesHintAtNonZeroOffset() {
        String sql =
                "select /*+ INDEX(t IDX_T) */ t.id";

        int offset =
                sql.indexOf("/*+");

        TokenMatch<SqlTokenKind> match =
                recognizer.match(
                        new SourceText(sql),
                        offset
                );

        assertNotNull(match);

        assertEquals(
                SqlTokenKind.OPTIMIZER_HINT,
                match.kind()
        );

        assertEquals(
                sql.indexOf(" t.id"),
                match.endOffset()
        );
    }

    @Test
    public void stopsImmediatelyAfterClosingComment() {
        String text =
                "/*+ INDEX(t IDX_T) */tail";

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
    public void stopsAtFirstClosingCommentDelimiter() {
        String text =
                "/*+ FIRST */ second */";

        TokenMatch<SqlTokenKind> match =
                recognizer.match(
                        new SourceText(text),
                        0
                );

        assertNotNull(match);

        assertEquals(
                text.indexOf(" second"),
                match.endOffset()
        );
    }

    @Test
    public void recognizesUnterminatedHintToEndOfSource() {
        String text =
                "/*+ INDEX(t IDX_T)";

        TokenMatch<SqlTokenKind> match =
                recognizer.match(
                        new SourceText(text),
                        0
                );

        assertNotNull(match);

        assertEquals(
                SqlTokenKind.OPTIMIZER_HINT,
                match.kind()
        );

        assertEquals(
                text.length(),
                match.endOffset()
        );
    }

    @Test
    public void doesNotRecognizeOrdinaryBlockComment() {
        assertNotRecognized(
                "/* ordinary comment */"
        );
    }

    @Test
    public void doesNotAllowWhitespaceBeforePlus() {
        assertNotRecognized(
                "/* + INDEX(t IDX_T) */"
        );
    }

    @Test
    public void doesNotRecognizePreprocessorDirective() {
        assertNotRecognized(
                "/* @parameterize:off */"
        );
    }

    @Test
    public void doesNotRecognizeLineComment() {
        assertNotRecognized(
                "--+ INDEX(t IDX_T)"
        );
    }

    @Test
    public void doesNotRecognizeCommentWithoutPlus() {
        assertNotRecognized(
                "/** INDEX(t IDX_T) */"
        );
    }

    @Test
    public void lexerClassifiesOptimizerHint() {
        String text =
                "/*+ INDEX(t IDX_T) */";

        LexerResult<SqlTokenKind> result =
                new SqlLexer().tokenize(text);

        Token<SqlTokenKind> token =
                result.tokens().get(0);

        assertEquals(
                SqlTokenKind.OPTIMIZER_HINT,
                token.kind()
        );

        assertEquals(
                text,
                result.text(token)
        );

        assertTrue(
                token.kind().isTrivia()
        );

        assertFalse(
                token.kind().isDirective()
        );
    }

    @Test
    public void lexerKeepsWhitespaceBeforePlusAsBlockComment() {
        String text =
                "/* + INDEX(t IDX_T) */";

        LexerResult<SqlTokenKind> result =
                new SqlLexer().tokenize(text);

        Token<SqlTokenKind> token =
                result.tokens().get(0);

        assertEquals(
                SqlTokenKind.BLOCK_COMMENT,
                token.kind()
        );

        assertEquals(
                text,
                result.text(token)
        );
    }

    @Test
    public void lexerKeepsDirectiveSeparateFromHint() {
        LexerResult<SqlTokenKind> result =
                new SqlLexer().tokenize(
                        "/* @parameterize:off */ "
                                + "/*+ INDEX(t IDX_T) */"
                );

        List<Token<SqlTokenKind>> tokens =
                result.tokens();

        assertEquals(
                SqlTokenKind.PREPROCESSOR_DIRECTIVE,
                tokens.get(0).kind()
        );

        assertEquals(
                SqlTokenKind.WHITESPACE,
                tokens.get(1).kind()
        );

        assertEquals(
                SqlTokenKind.OPTIMIZER_HINT,
                tokens.get(2).kind()
        );
    }

    @Test
    public void lexerDoesNotTokenizeHintContentsSeparately() {
        String sql =
                "select "
                        + "/*+ SOME_HINT(~ 100, 'text') */ "
                        + "'outside'";

        LexerResult<SqlTokenKind> result =
                new SqlLexer().tokenize(sql);

        int hintCount =
                0;

        for (Token<SqlTokenKind> token
                : result.tokens()) {

            String tokenText =
                    result.text(token);

            if (token.kind()
                    == SqlTokenKind.OPTIMIZER_HINT) {

                hintCount++;

                assertEquals(
                        "/*+ SOME_HINT(~ 100, 'text') */",
                        tokenText
                );
            }

            assertFalse(
                    token.kind()
                            == SqlTokenKind.OPERATOR
                            && "~".equals(tokenText)
            );

            assertFalse(
                    token.kind()
                            == SqlTokenKind.INTEGER_LITERAL
                            && "100".equals(tokenText)
            );
        }

        assertEquals(
                1,
                hintCount
        );
    }

    @Test
    public void lexerPreservesHintAmongSqlTokens() {
        String sql =
                "select /*+ FIRST_ROWS(10) */ "
                        + "t.id from table_name t";

        LexerResult<SqlTokenKind> result =
                new SqlLexer().tokenize(sql);

        int hintCount =
                0;

        for (Token<SqlTokenKind> token
                : result.tokens()) {

            if (token.kind()
                    == SqlTokenKind.OPTIMIZER_HINT) {

                hintCount++;

                assertEquals(
                        "/*+ FIRST_ROWS(10) */",
                        result.text(token)
                );
            }
        }

        assertEquals(
                1,
                hintCount
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
                SqlTokenKind.OPTIMIZER_HINT,
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