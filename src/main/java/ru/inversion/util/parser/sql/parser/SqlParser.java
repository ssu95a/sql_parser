package ru.inversion.util.parser.sql.parser;

import ru.inversion.util.parser.diagnostic.DiagnosticBag;
import ru.inversion.util.parser.lexer.LexerResult;
import ru.inversion.util.parser.lexer.Token;
import ru.inversion.util.parser.sql.ast.*;
import ru.inversion.util.parser.sql.lexer.SqlLexer;
import ru.inversion.util.parser.sql.lexer.SqlTokenKind;
import ru.inversion.util.parser.sql.ast.UnaryExpression;
import ru.inversion.util.parser.sql.ast.BinaryExpression;

import java.util.Objects;

public final class SqlParser {

    private final SqlTokenCursor cursor;
    private final DiagnosticBag diagnostics;
    private final LexerResult<SqlTokenKind> lexerResult;

    public SqlParser(CharSequence source)
    {
        Objects.requireNonNull(source, "source");

        this.lexerResult = new SqlLexer().tokenize(source);
        this.cursor      = new SqlTokenCursor(lexerResult);
        this.diagnostics = new DiagnosticBag();
    }

    /**
     * Поглощает ожидаемый токен.
     *
     * При несовпадении регистрирует ошибку, но не перемещает
     * курсор. Стратегия восстановления определяется вызывающим
     * методом грамматики.
     */
    private boolean expect(SqlTokenKind expectedKind) {
        Objects.requireNonNull(
                expectedKind,
                "expectedKind"
        );

        if (cursor.consumeIf(expectedKind)) {
            return true;
        }

        Token<SqlTokenKind> actual =
                cursor.current();

        diagnostics.error(
                SqlDiagnosticCodes.EXPECTED_TOKEN,
                actual.range(),
                "Ожидался токен "
                        + expectedKind
                        + ", найден "
                        + describe(actual)
        );

        return false;
    }

    /**
     * Поглощает ожидаемое SQL-слово без учёта регистра.
     */
    private boolean expectWord(String expectedWord) {
        Objects.requireNonNull(
                expectedWord,
                "expectedWord"
        );

        if (cursor.consumeWordIf(expectedWord)) {
            return true;
        }

        Token<SqlTokenKind> actual =
                cursor.current();

        diagnostics.error(
                SqlDiagnosticCodes.EXPECTED_WORD,
                actual.range(),
                "Ожидалось слово \""
                        + expectedWord
                        + "\", найден "
                        + describe(actual)
        );

        return false;
    }

    private String describe(
            Token<SqlTokenKind> token
    ) {
        if (token.kind()
                == SqlTokenKind.END_OF_FILE) {
            return "конец входного текста";
        }

        return token.kind()
                + " \""
                + cursor.result().text(token)
                + "\"";
    }

    private <T> SqlParseResult<T> result(T root) {
        return new SqlParseResult<T>( lexerResult, root, diagnostics.diagnostics() );
    }

    public SqlParseResult<SqlExpression> parseExpression() {
        SqlExpression expression =
                parseExpressionCore();

        /*
         * Проверка оставшихся токенов выполняется только
         * для всей входной строки.
         */
        if (expression != null && !cursor.isEnd()) {
            Token<SqlTokenKind> unexpected =
                    cursor.current();

            diagnostics.error(
                    SqlDiagnosticCodes.UNEXPECTED_TOKEN,
                    unexpected.range(),
                    "Неожиданный токен после выражения: "
                            + describe(unexpected)
            );
        }

        return result(expression);
    }

    private SqlExpression parseExpressionCore() {
        return parseBinaryExpression(0);
    }

    private SqlExpression parsePrimaryExpression() {
        Token<SqlTokenKind> token =
                cursor.current();

        SqlTokenKind kind = token.kind();


        if (kind == SqlTokenKind.LEFT_PARENTHESIS) {
            return parseParenthesizedExpression();
        }

        if (kind.isLiteral()) {
            cursor.consume();
            return new LiteralExpression(token);
        }

        if (kind.isParameter()) {
            cursor.consume();
            return new ParameterExpression(token);
        }

        if (kind == SqlTokenKind.WORD
                || kind == SqlTokenKind.QUOTED_IDENTIFIER) {
            cursor.consume();
            return new NameExpression(token);
        }

        diagnostics.error(
                SqlDiagnosticCodes.EXPECTED_EXPRESSION,
                token.range(),
                "Ожидалось SQL-выражение, найден "
                        + describe(token)
        );

        /*
         * Обеспечиваем продвижение при ошибке.
         * EOF остаётся на месте.
         */
        if (!cursor.isEnd()) {
            cursor.consume();
        }

        return null;
    }

    private SqlExpression parseParenthesizedExpression() {
        Token<SqlTokenKind> leftParenthesis =
                cursor.consume();

        if (cursor.is(SqlTokenKind.RIGHT_PARENTHESIS)
                || cursor.isEnd()) {

            Token<SqlTokenKind> actual =
                    cursor.current();

            diagnostics.error(
                    SqlDiagnosticCodes.EXPECTED_EXPRESSION,
                    actual.range(),
                    "Ожидалось выражение после открывающей скобки, найден "
                            + describe(actual)
            );

            cursor.consumeIf(
                    SqlTokenKind.RIGHT_PARENTHESIS
            );

            return null;
        }

        /*
         * Важно: внутренний метод без проверки конца SQL.
         * parseExpression() здесь вызывать нельзя.
         */
        SqlExpression expression =
                parseExpressionCore();

        if (expression == null) {
            return null;
        }

        Token<SqlTokenKind> rightParenthesis = null;

        if (cursor.is(SqlTokenKind.RIGHT_PARENTHESIS)) {
            rightParenthesis = cursor.consume();
        } else {
            Token<SqlTokenKind> actual =
                    cursor.current();

            diagnostics.error(
                    SqlDiagnosticCodes.EXPECTED_RIGHT_PARENTHESIS,
                    actual.range(),
                    "Ожидалась закрывающая скобка, найден "
                            + describe(actual)
            );
        }

        return new ParenthesizedExpression(
                leftParenthesis,
                expression,
                rightParenthesis
        );
    }

    private SqlExpression parseUnaryExpression() {
        if (!isUnaryOperator()) {
            return parsePrimaryExpression();
        }

        Token<SqlTokenKind> operator =
                cursor.consume();

        SqlExpression operand =
                parseUnaryExpression();

        if (operand == null) {
            return null;
        }

        return new UnaryExpression(
                operator,
                operand
        );
    }

    private boolean isUnaryOperator() {
        if (!cursor.is(SqlTokenKind.OPERATOR)) {
            return false;
        }

        String operator =
                lexerResult.text(cursor.current());

        return "+".equals(operator)
                || "-".equals(operator)
                || "~".equals(operator);
    }

    private SqlExpression parseBinaryExpression(
            int minimumPrecedence
    ) {
        SqlExpression left =
                parseUnaryExpression();

        if (left == null) {
            return null;
        }

        while (true) {
            Token<SqlTokenKind> operator =
                    cursor.current();

            int precedence =
                    binaryPrecedence(operator);

            if (precedence < minimumPrecedence) {
                break;
            }

            cursor.consume();

            /*
             * Все текущие бинарные операторы левоассоциативны.
             *
             * Поэтому правый операнд разбирается с более высоким
             * минимальным приоритетом.
             */
            SqlExpression right =
                    parseBinaryExpression(
                            precedence + 1
                    );

            if (right == null) {
                /*
                 * Диагностика уже создана внутри разбора
                 * правого выражения.
                 *
                 * Сохраняем успешно разобранную левую часть
                 * как частичный AST.
                 */
                return left;
            }

            left = new BinaryExpression(
                    left,
                    operator,
                    right
            );
        }

        return left;
    }

    private int binaryPrecedence(
            Token<SqlTokenKind> token
    ) {
        SqlTokenKind kind = token.kind();

        if (kind == SqlTokenKind.WORD) {
            String text =
                    lexerResult.text(token);

            if ("OR".equalsIgnoreCase(text)) {
                return 1;
            }

            if ("AND".equalsIgnoreCase(text)) {
                return 2;
            }

            return -1;
        }

        if (kind != SqlTokenKind.OPERATOR) {
            return -1;
        }

        String text =
                lexerResult.text(token);

        if ("=".equals(text)
                || "<>".equals(text)
                || "!=".equals(text)
                || "<".equals(text)
                || "<=".equals(text)
                || ">".equals(text)
                || ">=".equals(text)) {
            return 3;
        }

        if ("+".equals(text)
                || "-".equals(text)) {
            return 4;
        }

        if ("*".equals(text)
                || "/".equals(text)
                || "%".equals(text)) {
            return 5;
        }

        return -1;
    }
}