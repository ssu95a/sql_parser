package ru.inversion.util.parser.sql.ast;

import ru.inversion.util.parser.lexer.Token;
import ru.inversion.util.parser.sql.lexer.SqlTokenKind;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * Базовый класс SQL-выражений, представленных одним токеном.
 */
public abstract class TokenExpression
        extends SqlExpression {

    private final Token<SqlTokenKind> token;

    protected TokenExpression(
            Token<SqlTokenKind> token,
            Consumer<SqlTokenKind> checker
    ) {
        super(requireToken(token, checker).range());
        this.token = token;
    }

    public final Token<SqlTokenKind> token() {
        return token;
    }

    public final SqlTokenKind tokenKind() {
        return token.kind();
    }

    private static Token<SqlTokenKind> requireToken(
            Token<SqlTokenKind> token,
            Consumer<SqlTokenKind> checker
    ) {
        Objects.requireNonNull(token, "token");
        Objects.requireNonNull(checker, "checker");

        checker.accept(token.kind());

        return token;
    }
}