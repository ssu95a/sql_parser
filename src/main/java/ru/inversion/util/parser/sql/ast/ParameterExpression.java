package ru.inversion.util.parser.sql.ast;

import ru.inversion.util.parser.lexer.Token;
import ru.inversion.util.parser.sql.lexer.SqlTokenKind;

/**
 * SQL-параметр: ?, :name или $1.
 */
public final class ParameterExpression
        extends TokenExpression {

    public ParameterExpression(
            Token<SqlTokenKind> token
    ) {
        super(token, ParameterExpression::checkParameter);
    }

    public SqlTokenKind parameterKind() {
        return tokenKind();
    }

    private static void checkParameter( SqlTokenKind kind )
    {
        if( !kind.isParameter() )
            throw new IllegalArgumentException( "Token is not a parameter: " + kind );
    }
}