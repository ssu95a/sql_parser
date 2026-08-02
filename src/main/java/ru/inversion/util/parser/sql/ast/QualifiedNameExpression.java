package ru.inversion.util.parser.sql.ast;

import ru.inversion.util.parser.lexer.Token;
import ru.inversion.util.parser.sql.lexer.SqlTokenKind;
import ru.inversion.util.parser.text.TextRange;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Квалифицированное SQL-имя.
 *
 * Примеры:
 *   schema.table
 *   table.column
 *   catalog.schema.table
 *   "Some Schema"."Some Table"
 *
 * Также поддерживается частичный AST:
 *   schema.
 */
public final class QualifiedNameExpression extends SqlExpression {

    private final List<NameExpression> parts;
    private final List<Token<SqlTokenKind>> dots;

    public QualifiedNameExpression( List<NameExpression> parts, List<Token<SqlTokenKind>> dots )
    {
        super(createRange(parts, dots));
        this.parts = immutableCopy( parts, "parts" );
        this.dots  = immutableCopy( dots, "dots" );
    }

    /**
     * Части квалифицированного имени.
     */
    public List<NameExpression> parts() {
        return parts;
    }

    /**
     * Токены точек между частями имени.
     *
     * Последняя точка может не иметь правой части,
     * если parser восстанавливается после ошибки.
     */
    public List<Token<SqlTokenKind>> dots() {
        return dots;
    }

    public int partCount() {
        return parts.size();
    }

    public NameExpression firstPart() {
        return parts.get(0);
    }

    public NameExpression lastPart() {
        return parts.get(parts.size() - 1);
    }

    /**
     * Возвращает true для частичного имени вроде schema.
     */
    public boolean hasTrailingDot() {
        return dots.size() == parts.size();
    }

    public boolean isComplete() {
        return !hasTrailingDot();
    }

    private static TextRange createRange(
            List<NameExpression> parts,
            List<Token<SqlTokenKind>> dots
    ) {
        validate(parts, dots);

        NameExpression firstPart =
                parts.get(0);

        int end;

        if (dots.size() == parts.size()) {
            end = dots.get(dots.size() - 1)
                    .range()
                    .end();
        } else {
            end = parts.get(parts.size() - 1)
                    .end();
        }

        return new TextRange(
                firstPart.start(),
                end
        );
    }

    private static void validate(
            List<NameExpression> parts,
            List<Token<SqlTokenKind>> dots
    ) {
        Objects.requireNonNull(parts, "parts");
        Objects.requireNonNull(dots, "dots");

        if (parts.isEmpty()) {
            throw new IllegalArgumentException(
                    "parts is empty"
            );
        }

        /*
         * Без точки это обычный NameExpression.
         */
        if (dots.isEmpty()) {
            throw new IllegalArgumentException(
                    "Qualified name must contain at least one dot"
            );
        }

        /*
         * Полное имя:
         *   a.b       parts=2, dots=1
         *
         * Частичное имя:
         *   a.        parts=1, dots=1
         */
        if (dots.size() != parts.size() - 1
                && dots.size() != parts.size()) {
            throw new IllegalArgumentException(
                    "Invalid parts/dots count: parts="
                            + parts.size()
                            + ", dots="
                            + dots.size()
            );
        }

        for (int index = 0;
             index < parts.size();
             index++) {

            Objects.requireNonNull(
                    parts.get(index),
                    "parts[" + index + "]"
            );
        }

        for (int index = 0;
             index < dots.size();
             index++) {

            Token<SqlTokenKind> dot =
                    Objects.requireNonNull(
                            dots.get(index),
                            "dots[" + index + "]"
                    );

            if (dot.kind() != SqlTokenKind.DOT) {
                throw new IllegalArgumentException(
                        "dots["
                                + index
                                + "] must be DOT, found "
                                + dot.kind()
                );
            }

            NameExpression left =
                    parts.get(index);

            if (left.end() > dot.range().start()) {
                throw new IllegalArgumentException(
                        "Part must precede dot at index "
                                + index
                );
            }

            int rightIndex = index + 1;

            if (rightIndex < parts.size()) {
                NameExpression right =
                        parts.get(rightIndex);

                if (dot.range().end()
                        > right.start()) {
                    throw new IllegalArgumentException(
                            "Dot must precede part at index "
                                    + rightIndex
                    );
                }
            }
        }
    }

    private static <T> List<T> immutableCopy(
            List<T> source,
            String name
    ) {
        Objects.requireNonNull(source, name);

        return Collections.unmodifiableList(
                new ArrayList<T>(source)
        );
    }
}