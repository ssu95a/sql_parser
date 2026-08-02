package ru.inversion.util.parser.sql.ast;

import ru.inversion.util.parser.lexer.Token;
import ru.inversion.util.parser.sql.lexer.SqlTokenKind;

/**
 * Ссылка на SQL-имя.
 *
 * Поддерживаемые токены:
 *   WORD
 *   QUOTED_IDENTIFIER
 *
 * Квалифицированные имена вроде schema.table позднее будут
 * представлены отдельным составным узлом.
 */
public final class NameExpression extends TokenExpression {

    public NameExpression( Token<SqlTokenKind> token ) {
        super(token, NameExpression::checkName);
    }

    /**
     * Конкретный лексический вид имени.
     */
    public SqlTokenKind nameKind() {
        return tokenKind();
    }

    public boolean isQuoted() {return tokenKind() == SqlTokenKind.QUOTED_IDENTIFIER; }

    private static void checkName( SqlTokenKind kind )
    {
        if( kind != SqlTokenKind.WORD && kind != SqlTokenKind.QUOTED_IDENTIFIER)
            throw new IllegalArgumentException( "Token is not a name: " + kind );
    }
}