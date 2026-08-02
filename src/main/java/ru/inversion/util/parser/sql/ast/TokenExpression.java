package ru.inversion.util.parser.sql.ast;

import ru.inversion.util.parser.lexer.Token;
import ru.inversion.util.parser.sql.lexer.SqlTokenKind;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Базовый класс SQL-выражений, представленных одним токеном.
 *
 * Примеры:
 *   literal
 *   parameter
 *   identifier
 */
public abstract class TokenExpression extends SqlExpression {

    private final Token<SqlTokenKind> token;

    protected TokenExpression( Token<SqlTokenKind> token, Consumer<SqlTokenKind> check )
    {
        super( requireToken( token, check ).range() );
        this.token = token;
    }

    /**
     * Исходный lexer-токен.
     */
    public final Token<SqlTokenKind> token() {
        return token;
    }

    /**
     * Вид исходного токена.
     */
    public final SqlTokenKind tokenKind() {
        return token.kind();
    }

    private static Token<SqlTokenKind> requireToken( Token<SqlTokenKind> token, Consumer<SqlTokenKind> check )
    {
        Objects.requireNonNull( token, "token");
        check.accept(token.kind());
        return token;
    }
}