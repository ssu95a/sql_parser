package ru.inversion.util.parser.sql.lexer;

import org.junit.Test;
import ru.inversion.util.parser.lexer.LexerResult;
import ru.inversion.util.parser.lexer.Token;
import ru.inversion.util.parser.lexer.TokenMatch;
import ru.inversion.util.parser.sql.lexer.recognizer.PreprocessorDirectiveRecognizer;
import ru.inversion.util.parser.text.SourceText;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class PreprocessorDirectiveRecognizerTest {

    private final PreprocessorDirectiveRecognizer recognizer =
            new PreprocessorDirectiveRecognizer();

    @Test
    public void recognizesParameterizeOff() {
        assertRecognized(
                "/* @parameterize:off */"
        );
    }

    @Test
    public void recognizesParameterizeOn() {
        assertRecognized(
                "/* @parameterize:on */"
        );
    }

    @Test
    public void recognizesCompactOffDirective() {
        assertRecognized(
                "/*@parameterize:off*/"
        );
    }

    @Test
    public void recognizesCompactOnDirective() {
        assertRecognized(
                "/*@parameterize:on*/"
        );
    }

    @Test
    public void ignoresDirectiveCase() {
        assertRecognized(
                "/* @PARAMETERIZE:OFF */"
        );

        assertRecognized(
                "/* @Parameterize:On */"
        );
    }

    @Test
    public void allowsWhitespaceAroundColon() {
        assertRecognized(
                "/* @parameterize : off */"
        );

        assertRecognized(
                "/* @parameterize\t:\ton */"
        );
    }

    @Test
    public void allowsMultilineWhitespace() {
        assertRecognized(
                "/*\n"
                        + "    @parameterize\n"
                        + "    :\n"
                        + "    off\n"
                        + "*/"
        );
    }

    @Test
    public void recognizesDirectiveAtNonZeroOffset() {
        String sql =
                "select /* @parameterize:off */ 'text'";

        int offset =
                sql.indexOf("/*");

        TokenMatch<SqlTokenKind> match =
                recognizer.match(
                        new SourceText(sql),
                        offset
                );

        assertNotNull(match);

        assertEquals(
                SqlTokenKind.PREPROCESSOR_DIRECTIVE,
                match.kind()
        );

        assertEquals(
                sql.indexOf(" 'text'"),
                match.endOffset()
        );
    }

    @Test
    public void stopsImmediatelyAfterClosingComment() {
        String text =
                "/* @parameterize:off */tail";

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
    public void doesNotRecognizeOrdinaryBlockComment() {
        assertNotRecognized(
                "/* ordinary comment */"
        );
    }

    @Test
    public void requiresAtSign() {
        assertNotRecognized(
                "/* parameterize:off */"
        );
    }

    @Test
    public void requiresParameterizeName() {
        assertNotRecognized(
                "/* @bind:off */"
        );
    }

    @Test
    public void requiresColon() {
        assertNotRecognized(
                "/* @parameterize off */"
        );
    }

    @Test
    public void rejectsUnknownCommand() {
        assertNotRecognized(
                "/* @parameterize:disable */"
        );
    }

    @Test
    public void rejectsExtendedOffCommand() {
        assertNotRecognized(
                "/* @parameterize:offline */"
        );
    }

    @Test
    public void rejectsExtendedOnCommand() {
        assertNotRecognized(
                "/* @parameterize:only */"
        );
    }

    @Test
    public void rejectsTextBeforeDirective() {
        assertNotRecognized(
                "/* explanation @parameterize:off */"
        );
    }

    @Test
    public void rejectsTextAfterDirective() {
        assertNotRecognized(
                "/* @parameterize:off explanation */"
        );
    }

    @Test
    public void rejectsMultipleCommandsInOneComment() {
        assertNotRecognized(
                "/* @parameterize:off on */"
        );
    }

    @Test
    public void rejectsUnterminatedDirectiveComment() {
        assertNotRecognized(
                "/* @parameterize:off"
        );
    }

    @Test
    public void lexerClassifiesOffDirective() {
        LexerResult<SqlTokenKind> result =
                new SqlLexer().tokenize(
                        "/* @parameterize:off */"
                );

        Token<SqlTokenKind> token =
                result.tokens().get(0);

        assertEquals(
                SqlTokenKind.PREPROCESSOR_DIRECTIVE,
                token.kind()
        );

        assertEquals(
                "/* @parameterize:off */",
                result.text(token)
        );

        assertTrue(
                token.kind().isTrivia()
        );

        assertTrue(
                token.kind().isDirective()
        );
    }

    @Test
    public void lexerClassifiesOnDirective() {
        LexerResult<SqlTokenKind> result =
                new SqlLexer().tokenize(
                        "/* @parameterize:on */"
                );

        Token<SqlTokenKind> token =
                result.tokens().get(0);

        assertEquals(
                SqlTokenKind.PREPROCESSOR_DIRECTIVE,
                token.kind()
        );

        assertEquals(
                "/* @parameterize:on */",
                result.text(token)
        );
    }

    @Test
    public void lexerKeepsOrdinaryCommentAsBlockComment() {
        LexerResult<SqlTokenKind> result =
                new SqlLexer().tokenize(
                        "/* ordinary comment */"
                );

        Token<SqlTokenKind> token =
                result.tokens().get(0);

        assertEquals(
                SqlTokenKind.BLOCK_COMMENT,
                token.kind()
        );
    }

    @Test
    public void lexerKeepsMalformedDirectiveAsBlockComment() {
        LexerResult<SqlTokenKind> result =
                new SqlLexer().tokenize(
                        "/* @parameterize:disable */"
                );

        Token<SqlTokenKind> token =
                result.tokens().get(0);

        assertEquals(
                SqlTokenKind.BLOCK_COMMENT,
                token.kind()
        );

        assertEquals(
                "/* @parameterize:disable */",
                result.text(token)
        );
    }

    @Test
    public void lexerPreservesDirectivesAmongSqlTokens() {
        String sql =
                "select 'before', "
                        + "/* @parameterize:off */ "
                        + "'protected' "
                        + "/* @parameterize:on */, "
                        + "'after'";

        LexerResult<SqlTokenKind> result =
                new SqlLexer().tokenize(sql);

        List<Token<SqlTokenKind>> tokens =
                result.tokens();

        int directiveCount =
                0;

        for (Token<SqlTokenKind> token
                : tokens) {

            if (token.kind()
                    == SqlTokenKind
                    .PREPROCESSOR_DIRECTIVE) {

                directiveCount++;
            }
        }

        assertEquals(
                2,
                directiveCount
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
                SqlTokenKind.PREPROCESSOR_DIRECTIVE,
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