package ru.inversion.util.parser.sql.parser;

import ru.inversion.util.parser.diagnostic.DiagnosticBag;
import ru.inversion.util.parser.lexer.LexerResult;
import ru.inversion.util.parser.lexer.Token;
import ru.inversion.util.parser.sql.lexer.SqlLexer;
import ru.inversion.util.parser.sql.lexer.SqlTokenKind;

import java.util.Objects;

public final class SqlParser {

    private final SqlTokenCursor cursor;
    private final DiagnosticBag diagnostics;
    private final LexerResult<SqlTokenKind> lexerResult;

    public SqlParser(CharSequence source)
    {
        Objects.requireNonNull(source, "source");

        this.lexerResult = new SqlLexer().tokenize(source);
        this.cursor      = new SqlTokenCursor(lexerResult);
        this.diagnostics = new DiagnosticBag();
    }

    /**
     * Поглощает ожидаемый токен.
     *
     * При несовпадении регистрирует ошибку, но не перемещает
     * курсор. Стратегия восстановления определяется вызывающим
     * методом грамматики.
     */
    private boolean expect(SqlTokenKind expectedKind) {
        Objects.requireNonNull(
                expectedKind,
                "expectedKind"
        );

        if (cursor.consumeIf(expectedKind)) {
            return true;
        }

        Token<SqlTokenKind> actual =
                cursor.current();

        diagnostics.error(
                SqlDiagnosticCodes.EXPECTED_TOKEN,
                actual.range(),
                "Ожидался токен "
                        + expectedKind
                        + ", найден "
                        + describe(actual)
        );

        return false;
    }

    /**
     * Поглощает ожидаемое SQL-слово без учёта регистра.
     */
    private boolean expectWord(String expectedWord) {
        Objects.requireNonNull(
                expectedWord,
                "expectedWord"
        );

        if (cursor.consumeWordIf(expectedWord)) {
            return true;
        }

        Token<SqlTokenKind> actual =
                cursor.current();

        diagnostics.error(
                SqlDiagnosticCodes.EXPECTED_WORD,
                actual.range(),
                "Ожидалось слово \""
                        + expectedWord
                        + "\", найден "
                        + describe(actual)
        );

        return false;
    }

    private String describe(
            Token<SqlTokenKind> token
    ) {
        if (token.kind()
                == SqlTokenKind.END_OF_FILE) {
            return "конец входного текста";
        }

        return token.kind()
                + " \""
                + cursor.result().text(token)
                + "\"";
    }

    private <T> SqlParseResult<T> result(T root) {
        return new SqlParseResult<T>( lexerResult, root, diagnostics.diagnostics() );
    }
}