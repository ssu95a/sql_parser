package ru.inversion.util.parser.sql.ast;


import ru.inversion.util.parser.text.TextRange;

/**
 * Базовый класс SQL-инструкций.
 *
 * Примеры будущих наследников:
 * SELECT, INSERT, UPDATE, DELETE.
 */
public abstract class SqlStatement extends SqlNode {

    protected SqlStatement(TextRange range) {
        super(range);
    }
}