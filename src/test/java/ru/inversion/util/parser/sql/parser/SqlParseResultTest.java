package ru.inversion.util.parser.sql.parser;

import org.junit.Test;
import ru.inversion.util.parser.diagnostic.Diagnostic;
import ru.inversion.util.parser.diagnostic.DiagnosticSeverity;
import ru.inversion.util.parser.lexer.LexerResult;
import ru.inversion.util.parser.sql.lexer.SqlLexer;
import ru.inversion.util.parser.sql.lexer.SqlTokenKind;
import ru.inversion.util.parser.text.TextRange;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class SqlParseResultTest {

    @Test
    public void successfulResultMustContainRoot() {
        LexerResult<SqlTokenKind> lexerResult =
                new SqlLexer().tokenize("select");

        Object root = new Object();

        SqlParseResult<Object> result =
                new SqlParseResult<Object>(
                        lexerResult,
                        root,
                        Collections.<Diagnostic>emptyList()
                );

        assertSame(lexerResult, result.lexerResult());
        assertSame(root, result.root());

        assertTrue(result.hasRoot());
        assertFalse(result.hasDiagnostics());
        assertFalse(result.hasErrors());
        assertTrue(result.isSuccessful());
    }

    @Test
    public void resultMayHaveNoRoot() {
        LexerResult<SqlTokenKind> lexerResult =
                new SqlLexer().tokenize("");

        Diagnostic diagnostic = error(
                new TextRange(0, 0)
        );

        SqlParseResult<Object> result =
                new SqlParseResult<Object>(
                        lexerResult,
                        null,
                        Collections.singletonList(
                                diagnostic
                        )
                );

        assertNull(result.root());
        assertFalse(result.hasRoot());
        assertTrue(result.hasErrors());
        assertFalse(result.isSuccessful());
    }

    @Test
    public void warningMustNotMakeResultErroneous() {
        LexerResult<SqlTokenKind> lexerResult =
                new SqlLexer().tokenize("select");

        Diagnostic warning = new Diagnostic(
                "SQL100",
                DiagnosticSeverity.WARNING,
                "Warning",
                new TextRange(0, 6)
        );

        SqlParseResult<Object> result =
                new SqlParseResult<Object>(
                        lexerResult,
                        new Object(),
                        Collections.singletonList(
                                warning
                        )
                );

        assertTrue(result.hasDiagnostics());
        assertFalse(result.hasErrors());
        assertTrue(result.isSuccessful());
    }

    @Test
    public void errorMustMakeResultUnsuccessful() {
        LexerResult<SqlTokenKind> lexerResult =
                new SqlLexer().tokenize("select");

        SqlParseResult<Object> result =
                new SqlParseResult<Object>(
                        lexerResult,
                        new Object(),
                        Collections.singletonList(
                                error(new TextRange(0, 6))
                        )
                );

        assertTrue(result.hasRoot());
        assertTrue(result.hasErrors());
        assertFalse(result.isSuccessful());
    }

    @Test
    public void diagnosticsMustBeCopied() {
        LexerResult<SqlTokenKind> lexerResult =
                new SqlLexer().tokenize("select");

        List<Diagnostic> diagnostics =
                new ArrayList<Diagnostic>();

        SqlParseResult<Object> result =
                new SqlParseResult<Object>(
                        lexerResult,
                        new Object(),
                        diagnostics
                );

        diagnostics.add(
                error(new TextRange(0, 6))
        );

        assertTrue(result.diagnostics().isEmpty());
        assertFalse(result.hasErrors());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void diagnosticsMustBeImmutable() {
        SqlParseResult<Object> result =
                new SqlParseResult<Object>(
                        new SqlLexer().tokenize("select"),
                        new Object(),
                        Collections.<Diagnostic>emptyList()
                );

        result.diagnostics().add(
                error(new TextRange(0, 6))
        );
    }

    @Test
    public void sourceMustComeFromLexerResult() {
        String sql = "select value";

        SqlParseResult<Object> result =
                new SqlParseResult<Object>(
                        new SqlLexer().tokenize(sql),
                        new Object(),
                        Collections.<Diagnostic>emptyList()
                );

        assertEquals(sql, result.source().toString());
    }

    private static Diagnostic error(TextRange range) {
        return new Diagnostic(
                SqlDiagnosticCodes.UNEXPECTED_TOKEN,
                DiagnosticSeverity.ERROR,
                "Unexpected token",
                range
        );
    }
}