package ru.inversion.util.parser.sql.ast;

import ru.inversion.util.parser.lexer.Token;
import ru.inversion.util.parser.sql.lexer.SqlTokenKind;

import java.util.function.Predicate;

/**
 * SQL-параметр:
 *
 *   ?
 *   :name
 *   $1
 */
public final class ParameterExpression extends TokenExpression {

    public ParameterExpression( Token<SqlTokenKind> token )
    {
        super(token, sqlTokenKind -> {
            if (!sqlTokenKind.isParameter())
                throw new IllegalArgumentException( "Token is not a parameter: " + token.kind() );
        });
    }

    public SqlTokenKind parameterKind() { return tokenKind(); }
}