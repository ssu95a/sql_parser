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
 * Заменяет поддерживаемые строковые и числовые
 * SQL-литералы на JDBC-параметры {@code ?}.
 *
 * <p>Всегда поддерживаются:</p>
 *
 * <ul>
 *     <li>обычные строки в одинарных кавычках;</li>
 *     <li>целые числовые литералы;</li>
 *     <li>десятичные числовые литералы.</li>
 * </ul>
 *
 * <p>В зависимости от переданного диалекта
 * также могут поддерживаться:</p>
 *
 * <ul>
 *     <li>PostgreSQL dollar-quoted strings;</li>
 *     <li>Oracle q-quoted strings.</li>
 * </ul>
 *
 * <p>Не изменяются:</p>
 *
 * <ul>
 *     <li>существующие параметры {@code ?},
 *     {@code :name}, {@code $1};</li>
 *     <li>quoted identifiers;</li>
 *     <li>комментарии;</li>
 *     <li>не поддерживаемые выбранным диалектом
 *     формы строковых литералов;</li>
 *     <li>типизированные литералы DATE, TIMESTAMP,
 *     INTERVAL и аналогичные;</li>
 *     <li>неподдерживаемые числовые формы вроде
 *     {@code 1e3} и {@code 0xFF}.</li>
 * </ul>
 *
 * <p>Список {@link ParameterizedSql#parameters()}
 * содержит только значения литералов, заменённых
 * этим проходом. Значения уже существующих
 * параметров в него не входят.</p>
 */
public final class SqlLiteralParameterizer {

    private static final String PARAMETERIZE_OFF_DIRECTIVE =
            "/*@parameterize:off*/";

    private static final String PARAMETERIZE_ON_DIRECTIVE =
            "/*@parameterize:on*/";

    private enum ParameterizationDirective {
        OFF,
        ON
    }

    private final SqlLexer lexer;
    private final SqlSyntaxDialect dialect;

    public SqlLiteralParameterizer(
            SqlSyntaxDialect dialect
    ) {
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
     * <p>Участки между директивами</p>
     *
     * <pre>
     * /* @parameterize:off *&#47;
     * ...
     * /* @parameterize:on *&#47;
     * </pre>
     *
     * <p>сохраняются без изменений.</p>
     *
     * @param sql исходный SQL
     *
     * @return SQL с JDBC-параметрами и значения
     *
     * @throws NullPointerException
     *         если sql равен null
     *
     * @throws IllegalArgumentException
     *         если найден незакрытый поддерживаемый
     *         строковый литерал или нарушена
     *         последовательность директив
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

        boolean parameterizationEnabled =
                true;

        int protectionStartOffset =
                -1;

        for (int index = 0;
             index < tokens.size();
             index++) {

            Token<SqlTokenKind> token =
                    tokens.get(index);

            SqlTokenKind kind =
                    token.kind();

            /*
             * Новый читаемый формат:
             *
             *   /* @parameterize:off *\/
             *   /* @parameterize:on  *\/
             *
             * Директивы сохраняются в итоговом SQL.
             */
            if (kind
                    == SqlTokenKind
                    .PREPROCESSOR_DIRECTIVE) {

                ParameterizationDirective directive =
                        parseParameterizationDirective(
                                token.text(source),
                                token.range().start()
                        );

                if (directive
                        == ParameterizationDirective.OFF) {

                    if (!parameterizationEnabled) {
                        throw nestedParameterizationOff(
                                token.range().start()
                        );
                    }

                    parameterizationEnabled =
                            false;

                    protectionStartOffset =
                            token.range().start();
                } else {
                    if (parameterizationEnabled) {
                        throw unexpectedParameterizationOn(
                                token.range().start()
                        );
                    }

                    parameterizationEnabled =
                            true;

                    protectionStartOffset =
                            -1;
                }

                continue;
            }

            /*
             * Legacy-формат, используемый в production:
             *
             *   ~ защищённый SQL ~
             *
             * Каждая тильда переключает состояние.
             * Сами тильды удаляются из итогового SQL.
             */
            if (isLegacyParameterizationMarker(
                    token,
                    source
            )) {
                changes.add(
                        new TextChange(
                                token.range(),
                                ""
                        )
                );

                if (parameterizationEnabled) {
                    parameterizationEnabled =
                            false;

                    protectionStartOffset =
                            token.range().start();
                } else {
                    parameterizationEnabled =
                            true;

                    protectionStartOffset =
                            -1;
                }

                continue;
            }

            /*
             * Токены внутри защищённого участка остаются
             * без изменений и не добавляются в parameters.
             *
             * Переключатели выше всё равно обрабатываются,
             * чтобы защищённый участок можно было закрыть.
             */
            if (!parameterizationEnabled) {
                continue;
            }

            switch (kind) {
                case STRING_LITERAL:
                    parameterizeStringLiteral(
                            tokens,
                            index,
                            source,
                            token,
                            changes,
                            parameters
                    );
                    break;

                case POSTGRES_DOLLAR_QUOTED_STRING:
                case ORACLE_Q_QUOTED_STRING:
                    parameterizeDialectStringLiteral(
                            source,
                            token,
                            changes,
                            parameters
                    );
                    break;

                case INTEGER_LITERAL:
                    parameterizeIntegerLiteral(
                            source,
                            token,
                            changes,
                            parameters
                    );
                    break;

                case DECIMAL_LITERAL:
                    parameterizeDecimalLiteral(
                            source,
                            token,
                            changes,
                            parameters
                    );
                    break;

                default:
                    break;
            }
        }

        /*
         * Нечётное количество legacy-маркеров либо
         * @parameterize:off без последующего включения.
         */
        if (!parameterizationEnabled) {
            throw unclosedParameterizationProtection(
                    protectionStartOffset
            );
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

        addParameter(
                token,
                value,
                changes,
                parameters
        );
    }

    private void parameterizeDialectStringLiteral(
            SourceText source,
            Token<SqlTokenKind> token,
            List<TextChange> changes,
            List<Object> parameters
    ) {
        SqlTokenKind kind =
                token.kind();

        if (!shouldParameterizeDialectString(kind)) {
            return;
        }

        String literalText =
                token.text(source);

        String value;

        if (kind
                == SqlTokenKind
                .POSTGRES_DOLLAR_QUOTED_STRING) {

            value =
                    parsePostgresDollarQuotedString(
                            literalText,
                            token.range().start()
                    );
        } else if (kind
                == SqlTokenKind
                .ORACLE_Q_QUOTED_STRING) {

            value =
                    parseOracleQQuotedString(
                            literalText,
                            token.range().start()
                    );
        } else {
            throw new IllegalArgumentException(
                    "Unsupported dialect string token: "
                            + kind
            );
        }

        addParameter(
                token,
                value,
                changes,
                parameters
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

        BigInteger value;

        try {
            value =
                    new BigInteger(literalText);
        } catch (NumberFormatException exception) {
            throw invalidNumericLiteral(
                    literalText,
                    token.range().start(),
                    exception
            );
        }

        addParameter(
                token,
                value,
                changes,
                parameters
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

        BigDecimal value;

        try {
            value =
                    new BigDecimal(literalText);
        } catch (NumberFormatException exception) {
            throw invalidNumericLiteral(
                    literalText,
                    token.range().start(),
                    exception
            );
        }

        addParameter(
                token,
                value,
                changes,
                parameters
        );
    }

    private static void addParameter(
            Token<SqlTokenKind> token,
            Object value,
            List<TextChange> changes,
            List<Object> parameters
    ) {
        parameters.add(value);

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
     * Диалектные формы, которые пока не представлены
     * отдельным токеном, оставляются без изменений.
     *
     * <p>Например:</p>
     *
     * <pre>
     * E'text'
     * N'text'
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

    private boolean shouldParameterizeDialectString(
            SqlTokenKind kind
    ) {
        if (kind
                == SqlTokenKind
                .POSTGRES_DOLLAR_QUOTED_STRING) {

            return dialect.supports(
                    SqlSyntaxFeature
                            .POSTGRES_DOLLAR_QUOTED_STRING
            );
        }

        if (kind
                == SqlTokenKind
                .ORACLE_Q_QUOTED_STRING) {

            return dialect.supports(
                    SqlSyntaxFeature
                            .ORACLE_Q_QUOTED_STRING
            );
        }

        return false;
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
     * Не параметризуем известные типизированные
     * литералы.
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
     * Не заменяет числовой фрагмент, если он
     * является частью пока не поддерживаемой
     * числовой формы.
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

    private static String parsePostgresDollarQuotedString(
            String literalText,
            int offset
    ) {
        int openingDelimiterEnd =
                literalText.indexOf(
                        '$',
                        1
                );

        if (openingDelimiterEnd < 1) {
            throw unterminatedPostgresString(
                    offset
            );
        }

        String delimiter =
                literalText.substring(
                        0,
                        openingDelimiterEnd + 1
                );

        int delimiterLength =
                delimiter.length();

        if (literalText.length()
                < delimiterLength * 2
                || !literalText.endsWith(delimiter)) {

            throw unterminatedPostgresString(
                    offset
            );
        }

        return literalText.substring(
                delimiterLength,
                literalText.length()
                        - delimiterLength
        );
    }

    private static String parseOracleQQuotedString(
            String literalText,
            int offset
    ) {
        if (literalText.length() < 5
                || !isOracleQ(
                literalText.charAt(0)
        )
                || literalText.charAt(1) != '\'') {

            throw unterminatedOracleString(
                    offset
            );
        }

        char openingDelimiter =
                literalText.charAt(2);

        char closingDelimiter =
                oracleClosingDelimiter(
                        openingDelimiter
                );

        int closingDelimiterIndex =
                literalText.length() - 2;

        if (literalText.charAt(
                closingDelimiterIndex
        ) != closingDelimiter
                || literalText.charAt(
                literalText.length() - 1
        ) != '\'') {

            throw unterminatedOracleString(
                    offset
            );
        }

        return literalText.substring(
                3,
                closingDelimiterIndex
        );
    }

    private static boolean isOracleQ(
            char character
    ) {
        return character == 'q'
                || character == 'Q';
    }

    private static char oracleClosingDelimiter(
            char openingDelimiter
    ) {
        switch (openingDelimiter) {
            case '[':
                return ']';

            case '{':
                return '}';

            case '(':
                return ')';

            case '<':
                return '>';

            default:
                return openingDelimiter;
        }
    }

    private static IllegalArgumentException
    unterminatedPostgresString(
            int offset
    ) {
        return new IllegalArgumentException(
                "Unterminated PostgreSQL "
                        + "dollar-quoted string at offset "
                        + offset
        );
    }

    private static IllegalArgumentException
    unterminatedOracleString(
            int offset
    ) {
        return new IllegalArgumentException(
                "Unterminated Oracle "
                        + "q-quoted string at offset "
                        + offset
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

    private static ParameterizationDirective
    parseParameterizationDirective(
            String directiveText,
            int offset
    ) {
        String normalized =
                normalizeDirective(
                        directiveText
                );

        if (PARAMETERIZE_OFF_DIRECTIVE.equals(
                normalized
        )) {
            return ParameterizationDirective.OFF;
        }

        if (PARAMETERIZE_ON_DIRECTIVE.equals(
                normalized
        )) {
            return ParameterizationDirective.ON;
        }

        /*
         * При корректной работе lexer-а сюда попасть
         * невозможно: PREPROCESSOR_DIRECTIVE создаётся
         * только для двух поддерживаемых команд.
         */
        throw new IllegalArgumentException(
                "Unsupported preprocessor directive "
                        + "at offset "
                        + offset
        );
    }

    private static String normalizeDirective(
            String directiveText
    ) {
        StringBuilder normalized =
                new StringBuilder(
                        directiveText.length()
                );

        for (int index = 0;
             index < directiveText.length();
             index++) {

            char character =
                    directiveText.charAt(index);

            if (Character.isWhitespace(character)
                    || Character.isSpaceChar(character)) {

                continue;
            }

            normalized.append(
                    toLowerAscii(character)
            );
        }

        return normalized.toString();
    }

    private static char toLowerAscii(
            char character
    ) {
        if (character >= 'A'
                && character <= 'Z') {

            return (char) (
                    character
                            + ('a' - 'A')
            );
        }

        return character;
    }

    private static IllegalArgumentException
    nestedParameterizationOff(
            int offset
    ) {
        return new IllegalArgumentException(
                "Nested @parameterize:off "
                        + "directive at offset "
                        + offset
        );
    }

    private static IllegalArgumentException
    unexpectedParameterizationOn(
            int offset
    ) {
        return new IllegalArgumentException(
                "Unexpected @parameterize:on "
                        + "directive at offset "
                        + offset
        );
    }

    private static IllegalArgumentException
    unclosedParameterizationOff(
            int offset
    ) {
        return new IllegalArgumentException(
                "Unclosed @parameterize:off "
                        + "directive at offset "
                        + offset
        );
    }

    private static boolean isLegacyParameterizationMarker(
            Token<SqlTokenKind> token,
            SourceText source
    ) {
        return token.kind()
                == SqlTokenKind.OPERATOR
                && "~".equals(
                token.text(source)
        );
    }

    private static IllegalArgumentException
    unclosedParameterizationProtection(
            int offset
    ) {
        return new IllegalArgumentException(
                "Unclosed parameterization protection "
                        + "at offset "
                        + offset
        );
    }
}