package ru.inversion.util.parser.sql.transform;

import ru.inversion.util.parser.lexer.LexerResult;
import ru.inversion.util.parser.lexer.Token;
import ru.inversion.util.parser.sql.dialect.SqlSyntaxDialect;
import ru.inversion.util.parser.sql.dialect.SqlSyntaxFeature;
import ru.inversion.util.parser.sql.lexer.SqlLexer;
import ru.inversion.util.parser.sql.lexer.SqlTokenKind;
import ru.inversion.util.parser.text.SourceText;
import ru.inversion.util.parser.text.TextChange;
import ru.inversion.util.parser.text.TextChangeApplier;
import ru.inversion.util.parser.text.TextRange;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Заменяет обычные строковые и числовые SQL-литералы
 * на JDBC-параметры {@code ?}.
 *
 * <p>Поддерживаются:</p>
 *
 * <ul>
 *     <li>обычные строки в одинарных кавычках;</li>
 *     <li>целые числовые литералы;</li>
 *     <li>десятичные числовые литералы.</li>
 * </ul>
 *
 * <p>Не изменяются:</p>
 *
 * <ul>
 *     <li>существующие параметры {@code ?}, {@code :name},
 *     {@code $1};</li>
 *     <li>quoted identifiers;</li>
 *     <li>комментарии;</li>
 *     <li>диалектные строковые литералы с префиксами;</li>
 *     <li>типизированные литералы DATE, TIMESTAMP,
 *     INTERVAL и аналогичные;</li>
 *     <li>неподдерживаемые числовые формы вроде
 *     {@code 1e3} и {@code 0xFF}.</li>
 * </ul>
 *
 * <p>Список {@link ParameterizedSql#parameters()} содержит
 * только значения литералов, заменённых этим проходом.
 * Значения уже существующих параметров в него не входят.</p>
 */
public final class SqlLiteralParameterizer {

    private final SqlLexer lexer;
    private final SqlSyntaxDialect dialect;

    public SqlLiteralParameterizer(SqlSyntaxDialect dialect) {
        this.lexer =
                new SqlLexer();
        this.dialect =
                Objects.requireNonNull(
                        dialect,
                        "dialect"
                );
    }

    /**
     * Параметризует поддерживаемые литералы.
     *
     * @param sql исходный SQL
     *
     * @return SQL с JDBC-параметрами и значения
     *
     * @throws NullPointerException
     *         если sql равен null
     *
     * @throws IllegalArgumentException
     *         если найден незакрытый обычный
     *         строковый литерал
     */
    public ParameterizedSql parameterize(
            CharSequence sql
    ) {
        Objects.requireNonNull(
                sql,
                "sql"
        );

        LexerResult<SqlTokenKind> lexerResult =
                lexer.tokenize(sql);

        SourceText source =
                lexerResult.source();

        List<Token<SqlTokenKind>> tokens =
                lexerResult.tokens();

        List<TextChange> changes =
                new ArrayList<TextChange>();

        List<Object> parameters =
                new ArrayList<Object>();

        for (int index = 0;
             index < tokens.size();
             index++) {

            Token<SqlTokenKind> token =
                    tokens.get(index);

            SqlTokenKind kind =
                    token.kind();

            if (kind == SqlTokenKind.STRING_LITERAL) {
                parameterizeStringLiteral(
                        tokens,
                        index,
                        source,
                        token,
                        changes,
                        parameters
                );

                continue;
            }

            if (kind == SqlTokenKind.INTEGER_LITERAL) {
                parameterizeIntegerLiteral(
                        source,
                        token,
                        changes,
                        parameters
                );

                continue;
            }

            if (kind == SqlTokenKind.DECIMAL_LITERAL) {
                parameterizeDecimalLiteral(
                        source,
                        token,
                        changes,
                        parameters
                );
            }
        }

        String parameterizedSql =
                TextChangeApplier.apply(
                        sql,
                        changes
                );

        return new ParameterizedSql(
                parameterizedSql,
                parameters
        );
    }

    private static void parameterizeStringLiteral(
            List<Token<SqlTokenKind>> tokens,
            int tokenIndex,
            SourceText source,
            Token<SqlTokenKind> token,
            List<TextChange> changes,
            List<Object> parameters
    ) {
        if (!shouldParameterizeStringLiteral(
                tokens,
                tokenIndex,
                source
        )) {
            return;
        }

        String literalText =
                token.text(source);

        String value =
                parseStringLiteral(
                        literalText,
                        token.range().start()
                );

        parameters.add(value);

        changes.add(
                replacement(token.range())
        );
    }

    private static void parameterizeIntegerLiteral(
            SourceText source,
            Token<SqlTokenKind> token,
            List<TextChange> changes,
            List<Object> parameters
    ) {
        if (!isStandaloneNumericLiteral(
                source,
                token.range()
        )) {
            return;
        }

        String literalText =
                token.text(source);

        try {
            parameters.add(
                    new BigInteger(literalText)
            );
        } catch (NumberFormatException exception) {
            throw invalidNumericLiteral(
                    literalText,
                    token.range().start(),
                    exception
            );
        }

        changes.add(
                replacement(token.range())
        );
    }

    private static void parameterizeDecimalLiteral(
            SourceText source,
            Token<SqlTokenKind> token,
            List<TextChange> changes,
            List<Object> parameters
    ) {
        if (!isStandaloneNumericLiteral(
                source,
                token.range()
        )) {
            return;
        }

        String literalText =
                token.text(source);

        try {
            parameters.add(
                    new BigDecimal(literalText)
            );
        } catch (NumberFormatException exception) {
            throw invalidNumericLiteral(
                    literalText,
                    token.range().start(),
                    exception
            );
        }

        changes.add(
                replacement(token.range())
        );
    }

    private static TextChange replacement(
            TextRange range
    ) {
        return new TextChange(
                range,
                "?"
        );
    }

    /**
     * Диалектные формы пока оставляются без изменений.
     *
     * Примеры:
     *
     * <pre>
     * E'text'
     * N'text'
     * q'[text]'
     * U&amp;'text'
     * </pre>
     */
    private static boolean shouldParameterizeStringLiteral(
            List<Token<SqlTokenKind>> tokens,
            int tokenIndex,
            SourceText source
    ) {
        Token<SqlTokenKind> token =
                tokens.get(tokenIndex);

        int start =
                token.range().start();

        if (hasAdjacentStringPrefix(
                source,
                start
        )) {
            return false;
        }

        int previousIndex =
                previousNonTriviaTokenIndex(
                        tokens,
                        tokenIndex - 1
                );

        if (previousIndex < 0) {
            return true;
        }

        Token<SqlTokenKind> previous =
                tokens.get(previousIndex);

        if (previous.kind()
                != SqlTokenKind.WORD) {

            return true;
        }

        String previousWord =
                previous.text(source)
                        .toLowerCase(Locale.ROOT);

        return !isTypedLiteralPrefix(
                previousWord
        );
    }

    private static boolean hasAdjacentStringPrefix(
            SourceText source,
            int stringStart
    ) {
        int previousCharacter =
                source.get(stringStart - 1);

        if (previousCharacter
                == SourceText.EOF) {

            return false;
        }

        char value =
                (char) previousCharacter;

        return Character.isLetterOrDigit(value)
                || value == '_'
                || value == '$'
                || value == '&';
    }

    /**
     * Не параметризуем известные типизированные литералы.
     *
     * Примеры:
     *
     * <pre>
     * DATE '2026-08-04'
     * TIMESTAMP '2026-08-04 12:00:00'
     * INTERVAL '1 day'
     * BIT '1010'
     * </pre>
     */
    private static boolean isTypedLiteralPrefix(
            String word
    ) {
        return "date".equals(word)
                || "time".equals(word)
                || "timestamp".equals(word)
                || "interval".equals(word)
                || "bit".equals(word)
                || "varbit".equals(word)
                || "national".equals(word)
                || "nchar".equals(word)
                || "character".equals(word)
                || "char".equals(word)
                || "varchar".equals(word)
                || "varchar2".equals(word)
                || "nvarchar2".equals(word)
                || "raw".equals(word)
                || "json".equals(word)
                || "jsonb".equals(word)
                || "xml".equals(word)
                || "uuid".equals(word)
                || "zone".equals(word);
    }

    /**
     * Не заменяет числовой фрагмент, если он является
     * частью пока не поддерживаемой числовой формы.
     *
     * Примеры:
     *
     * <pre>
     * 1e3
     * 0xFF
     * 1_000
     * :1
     * </pre>
     */
    private static boolean isStandaloneNumericLiteral(
            SourceText source,
            TextRange range
    ) {
        int previousCharacter =
                source.get(range.start() - 1);

        int nextCharacter =
                source.get(range.end());

        return !isNumericNeighbor(
                previousCharacter
        ) && !isNumericNeighbor(
                nextCharacter
        );
    }

    private static boolean isNumericNeighbor(
            int character
    ) {
        if (character == SourceText.EOF) {
            return false;
        }

        char value =
                (char) character;

        return Character.isLetterOrDigit(value)
                || value == '_'
                || value == '$'
                || value == ':';
    }

    private static String parseStringLiteral(
            String literalText,
            int offset
    ) {
        if (literalText.length() < 2
                || literalText.charAt(0) != '\''
                || literalText.charAt(
                literalText.length() - 1
        ) != '\'') {

            throw new IllegalArgumentException(
                    "Unterminated string literal at offset "
                            + offset
            );
        }

        String content =
                literalText.substring(
                        1,
                        literalText.length() - 1
                );

        return content.replace(
                "''",
                "'"
        );
    }

    private static IllegalArgumentException
    invalidNumericLiteral(
            String literalText,
            int offset,
            NumberFormatException cause
    ) {
        return new IllegalArgumentException(
                "Invalid numeric literal '"
                        + literalText
                        + "' at offset "
                        + offset,
                cause
        );
    }

    private static int previousNonTriviaTokenIndex(
            List<Token<SqlTokenKind>> tokens,
            int startIndex
    ) {
        int index =
                startIndex;

        while (index >= 0
                && tokens.get(index)
                .kind()
                .isTrivia()) {

            index--;
        }

        return index;
    }

    private boolean shouldParameterizeDialectString(
            SqlTokenKind kind
    ) {
        if (kind
                == SqlTokenKind.POSTGRES_DOLLAR_QUOTED_STRING) {

            return dialect.supports(
                    SqlSyntaxFeature
                            .POSTGRES_DOLLAR_QUOTED_STRING
            );
        }

        if (kind
                == SqlTokenKind.ORACLE_Q_QUOTED_STRING) {

            return dialect.supports(
                    SqlSyntaxFeature
                            .ORACLE_Q_QUOTED_STRING
            );
        }

        return false;
    }
}