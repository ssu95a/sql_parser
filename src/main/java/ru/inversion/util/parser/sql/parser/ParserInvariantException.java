package ru.inversion.util.parser.sql.parser;

/**
 * Нарушение внутреннего контракта SQL parser-а.
 *
 * Не используется для сообщения о синтаксических ошибках SQL.
 */
public final class ParserInvariantException
        extends IllegalStateException {

    public ParserInvariantException(String message) {
        super(message);
    }

    public ParserInvariantException(
            String message,
            Throwable cause
    ) {
        super(message, cause);
    }
}