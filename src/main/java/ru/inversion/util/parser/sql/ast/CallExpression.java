package ru.inversion.util.parser.sql.ast;

import ru.inversion.util.parser.lexer.Token;
import ru.inversion.util.parser.sql.lexer.SqlTokenKind;
import ru.inversion.util.parser.text.TextRange;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Вызов SQL-функции.
 *
 * Примеры:
 *   now()
 *   sum(price)
 *   coalesce(a, b, 0)
 *   schema.calculate_tax(price)
 *
 * Правая скобка может отсутствовать в частичном AST.
 * Также может сохраняться завершающая запятая.
 */
public final class CallExpression
        extends SqlExpression {

    private final SqlExpression callee;
    private final Token<SqlTokenKind> leftParenthesis;
    private final List<SqlExpression> arguments;
    private final List<Token<SqlTokenKind>> commas;
    private final Token<SqlTokenKind> rightParenthesis;

    public CallExpression(
            SqlExpression callee,
            Token<SqlTokenKind> leftParenthesis,
            List<SqlExpression> arguments,
            List<Token<SqlTokenKind>> commas,
            Token<SqlTokenKind> rightParenthesis
    ) {
        super(createRange(
                callee,
                leftParenthesis,
                arguments,
                commas,
                rightParenthesis
        ));

        this.callee = callee;
        this.leftParenthesis = leftParenthesis;
        this.arguments = immutableCopy(
                arguments,
                "arguments"
        );
        this.commas = immutableCopy(
                commas,
                "commas"
        );
        this.rightParenthesis = rightParenthesis;
    }

    public SqlExpression callee() {
        return callee;
    }

    public Token<SqlTokenKind> leftParenthesis() {
        return leftParenthesis;
    }

    public List<SqlExpression> arguments() {
        return arguments;
    }

    public List<Token<SqlTokenKind>> commas() {
        return commas;
    }

    public Token<SqlTokenKind> rightParenthesis() {
        return rightParenthesis;
    }

    public int argumentCount() {
        return arguments.size();
    }

    public boolean hasArguments() {
        return !arguments.isEmpty();
    }

    public boolean hasRightParenthesis() {
        return rightParenthesis != null;
    }

    /**
     * Завершающая запятая присутствует в:
     *
     *   function(argument,
     *   function(argument,)
     */
    public boolean hasTrailingComma() {
        return !commas.isEmpty()
                && commas.size() == arguments.size();
    }

    private static TextRange createRange(
            SqlExpression callee,
            Token<SqlTokenKind> leftParenthesis,
            List<SqlExpression> arguments,
            List<Token<SqlTokenKind>> commas,
            Token<SqlTokenKind> rightParenthesis
    ) {
        validate(
                callee,
                leftParenthesis,
                arguments,
                commas,
                rightParenthesis
        );

        int end;

        if (rightParenthesis != null) {
            end = rightParenthesis.range().end();
        } else if (!commas.isEmpty()
                && commas.size() == arguments.size()) {
            end = commas.get(commas.size() - 1)
                    .range()
                    .end();
        } else if (!arguments.isEmpty()) {
            end = arguments.get(arguments.size() - 1)
                    .end();
        } else {
            end = leftParenthesis.range().end();
        }

        return new TextRange(
                callee.start(),
                end
        );
    }

    private static void validate(
            SqlExpression callee,
            Token<SqlTokenKind> leftParenthesis,
            List<SqlExpression> arguments,
            List<Token<SqlTokenKind>> commas,
            Token<SqlTokenKind> rightParenthesis
    ) {
        Objects.requireNonNull(callee, "callee");

        requireKind(
                leftParenthesis,
                SqlTokenKind.LEFT_PARENTHESIS,
                "leftParenthesis"
        );

        Objects.requireNonNull(arguments, "arguments");
        Objects.requireNonNull(commas, "commas");

        if (rightParenthesis != null) {
            requireKind(
                    rightParenthesis,
                    SqlTokenKind.RIGHT_PARENTHESIS,
                    "rightParenthesis"
            );
        }

        if (callee.end()
                > leftParenthesis.range().start()) {
            throw new IllegalArgumentException(
                    "callee must precede leftParenthesis"
            );
        }

        validateItems(
                leftParenthesis,
                arguments,
                commas,
                rightParenthesis
        );
    }

    private static void validateItems(
            Token<SqlTokenKind> leftParenthesis,
            List<SqlExpression> arguments,
            List<Token<SqlTokenKind>> commas,
            Token<SqlTokenKind> rightParenthesis
    ) {
        for (int index = 0;
             index < arguments.size();
             index++) {

            Objects.requireNonNull(
                    arguments.get(index),
                    "arguments[" + index + "]"
            );
        }

        for (int index = 0;
             index < commas.size();
             index++) {

            requireKind(
                    commas.get(index),
                    SqlTokenKind.COMMA,
                    "commas[" + index + "]"
            );
        }

        /*
         * function()
         */
        if (arguments.isEmpty()) {
            if (!commas.isEmpty()) {
                throw new IllegalArgumentException(
                        "Call without arguments cannot contain commas"
                );
            }
        } else {
            /*
             * Полный список:
             *   function(a, b)
             *   arguments=2, commas=1
             *
             * Список с завершающей запятой:
             *   function(a,
             *   function(a,)
             *   arguments=1, commas=1
             */
            int minimumCommaCount =
                    arguments.size() - 1;

            int maximumCommaCount =
                    arguments.size();

            if (commas.size() < minimumCommaCount
                    || commas.size() > maximumCommaCount) {
                throw new IllegalArgumentException(
                        "Invalid arguments/commas count: arguments="
                                + arguments.size()
                                + ", commas="
                                + commas.size()
                );
            }
        }

        if (!arguments.isEmpty()) {
            SqlExpression firstArgument =
                    arguments.get(0);

            if (leftParenthesis.range().end()
                    > firstArgument.start()) {
                throw new IllegalArgumentException(
                        "leftParenthesis must precede first argument"
                );
            }
        }

        for (int index = 0;
             index < commas.size();
             index++) {

            Token<SqlTokenKind> comma =
                    commas.get(index);

            SqlExpression leftArgument =
                    arguments.get(index);

            if (leftArgument.end()
                    > comma.range().start()) {
                throw new IllegalArgumentException(
                        "Argument must precede comma at index "
                                + index
                );
            }

            int rightArgumentIndex =
                    index + 1;

            if (rightArgumentIndex
                    < arguments.size()) {

                SqlExpression rightArgument =
                        arguments.get(
                                rightArgumentIndex
                        );

                if (comma.range().end()
                        > rightArgument.start()) {
                    throw new IllegalArgumentException(
                            "Comma must precede argument at index "
                                    + rightArgumentIndex
                    );
                }
            }
        }

        if (rightParenthesis != null) {
            int previousEnd =
                    previousItemEnd(
                            leftParenthesis,
                            arguments,
                            commas
                    );

            if (previousEnd
                    > rightParenthesis.range().start()) {
                throw new IllegalArgumentException(
                        "rightParenthesis must follow call contents"
                );
            }
        }
    }

    private static int previousItemEnd(
            Token<SqlTokenKind> leftParenthesis,
            List<SqlExpression> arguments,
            List<Token<SqlTokenKind>> commas
    ) {
        if (!commas.isEmpty()
                && commas.size() == arguments.size()) {
            return commas.get(commas.size() - 1)
                    .range()
                    .end();
        }

        if (!arguments.isEmpty()) {
            return arguments.get(arguments.size() - 1)
                    .end();
        }

        return leftParenthesis.range().end();
    }

    private static void requireKind(
            Token<SqlTokenKind> token,
            SqlTokenKind expectedKind,
            String name
    ) {
        Objects.requireNonNull(token, name);

        if (token.kind() != expectedKind) {
            throw new IllegalArgumentException(
                    name
                            + " must be "
                            + expectedKind
                            + ", found "
                            + token.kind()
            );
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