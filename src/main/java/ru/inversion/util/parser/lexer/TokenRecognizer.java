package ru.inversion.util.parser.lexer;


import ru.inversion.util.parser.text.SourceText;

/**
 * Одно лексическое правило.
 *
 * Реализация не должна изменять SourceText или внешнее состояние.
 */
public interface TokenRecognizer<K extends TokenKind> {

    /**
     * Возвращает совпадение либо null, если правило неприменимо.
     */
    TokenMatch<K> match(SourceText source, int offset);

    /** */
    default boolean isDigit( int character ) {
        return character >= '0' && character <= '9';
    }

}