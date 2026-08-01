package ru.inversion.util.parser.sql.parser;

import ru.inversion.util.parser.diagnostic.Diagnostic;
import ru.inversion.util.parser.lexer.LexerResult;
import ru.inversion.util.parser.sql.lexer.SqlTokenKind;
import ru.inversion.util.parser.text.SourceText;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Неизменяемый результат синтаксического разбора SQL.
 * <p>
 * @param <T> тип корневого AST-узла
 */
public final class SqlParseResult<T> {

    private final LexerResult<SqlTokenKind> lexerResult;
    private final T root;
    private final List<Diagnostic> diagnostics;
    private final boolean hasErrors;

    public SqlParseResult(
            LexerResult<SqlTokenKind> lexerResult,
            T root,
            List<Diagnostic> diagnostics
    ) {
        this.lexerResult = Objects.requireNonNull(
                lexerResult,
                "lexerResult"
        );

        this.root = root;

        Objects.requireNonNull(
                diagnostics,
                "diagnostics"
        );

        List<Diagnostic> diagnosticSnapshot =
                new ArrayList<Diagnostic>(
                        diagnostics.size()
                );

        boolean errorFound = false;

        for (Diagnostic diagnostic : diagnostics) {
            Diagnostic checkedDiagnostic =
                    Objects.requireNonNull(
                            diagnostic,
                            "diagnostic"
                    );

            diagnosticSnapshot.add(checkedDiagnostic);

            if (checkedDiagnostic.isError()) {
                errorFound = true;
            }
        }

        this.diagnostics =
                Collections.unmodifiableList(
                        diagnosticSnapshot
                );

        this.hasErrors = errorFound;
    }

    /**
     * Полный результат lexer-а, включая trivia и EOF.
     */
    public LexerResult<SqlTokenKind> lexerResult() {
        return lexerResult;
    }

    /**
     * Исходный текст.
     */
    public SourceText source() {
        return lexerResult.source();
    }

    /**
     * Корневой AST-узел.
     *
     * Может быть null, если parser не смог построить корень.
     */
    public T root() {
        return root;
    }

    public boolean hasRoot() {
        return root != null;
    }

    /**
     * Неизменяемый список диагностик.
     */
    public List<Diagnostic> diagnostics() {
        return diagnostics;
    }

    public boolean hasDiagnostics() {
        return !diagnostics.isEmpty();
    }

    public boolean hasErrors() {
        return hasErrors;
    }

    /**
     * Успешным считается результат с корнем и без ошибок.
     *
     * Предупреждения не делают разбор неуспешным.
     */
    public boolean isSuccessful() {
        return root != null && !hasErrors;
    }
}