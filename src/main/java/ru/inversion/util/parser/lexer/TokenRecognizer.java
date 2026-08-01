package ru.inversion.util.parser.lexer;


import ru.inversion.util.parser.text.SourceText;

/**
 * Одно лексическое правило.
 *
 * Реализация не должна изменять SourceText или внешнее состояние.
 */
public interface TokenRecognizer<K> {

    /**
     * @return совпадение либо null, если правило не применимо.
     */
    TokenMatch<K> match( SourceText source, int offset );
}