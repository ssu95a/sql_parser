package ru.inversion.util.parser.sql.ast;

import ru.inversion.util.parser.lexer.Token;
import ru.inversion.util.parser.sql.lexer.SqlTokenKind;

/**
 * SQL-литерал.
 */
public final class LiteralExpression extends TokenExpression {

    public LiteralExpression( Token<SqlTokenKind> token ) {
        super(token, LiteralExpression::checkLiteral);
    }

    public SqlTokenKind literalKind() {
        return tokenKind();
    }

    private static void checkLiteral( SqlTokenKind kind )
    {
        if( !kind.isLiteral() )
            throw new IllegalArgumentException( "Token is not a literal: " + kind );
    }
}