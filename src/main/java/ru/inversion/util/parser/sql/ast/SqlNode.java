package ru.inversion.util.parser.sql.ast;

import ru.inversion.util.parser.text.TextRange;

import java.util.Objects;

/**
 * Базовый класс всех узлов SQL AST.
 *
 * Диапазон относится к исходному тексту и использует формат
 * [start, end).
 *
 * Обычно range не включает ведущие и завершающие trivia:
 * пробелы и комментарии остаются в полном потоке lexer-а.
 */
public abstract class SqlNode {

    private final TextRange range;

    protected SqlNode( TextRange range ) {
        this.range = Objects.requireNonNull(range, "range");
    }

    /**
     * Диапазон узла в исходном тексте.
     */
    public final TextRange range() {
        return range;
    }

    public final int start() {
        return range.start();
    }

    public final int end() {
        return range.end();
    }

    public final int length() {
        return range.length();
    }

    /**
     * Пустой диапазон может использоваться для отсутствующего
     * или синтетического узла в позиции ошибки.
     */
    public final boolean isEmpty() {
        return range.isEmpty();
    }
}