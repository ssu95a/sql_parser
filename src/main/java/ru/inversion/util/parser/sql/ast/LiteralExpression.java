package ru.inversion.util.parser.sql.ast;

import ru.inversion.util.parser.lexer.Token;
import ru.inversion.util.parser.sql.lexer.SqlTokenKind;

import java.util.function.Consumer;

/**
 * SQL-литерал.
 */
public final class LiteralExpression extends TokenExpression {

    public LiteralExpression( Token<SqlTokenKind> token ) {
        super(token, new Consumer<SqlTokenKind>() {
            @Override
            public void accept(SqlTokenKind sqlTokenKind) {
                if (!sqlTokenKind.isLiteral())
                    throw new IllegalArgumentException( "Token is not a literal: " + sqlTokenKind );
            }
        });
    }

    public SqlTokenKind literalKind() {
        return tokenKind();
    }
}