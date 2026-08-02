package ru.inversion.util.parser.sql.ast;

import ru.inversion.util.parser.text.TextRange;

/**
 * Базовый класс SQL-выражений.
 * <p>
 * Примеры будущих наследников:
 * литерал, имя столбца, параметр, вызов функции,
 * унарное и бинарное выражение.
 */
public abstract class SqlExpression extends SqlNode {

    protected SqlExpression(TextRange range) {
        super(range);
    }
}