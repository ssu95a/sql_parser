package ru.inversion.util.parser.sql.transform;

import ru.inversion.util.parser.lexer.LexerResult;
import ru.inversion.util.parser.lexer.Token;
import ru.inversion.util.parser.sql.dialect.SqlSyntaxDialect;
import ru.inversion.util.parser.sql.lexer.SqlLexer;
import ru.inversion.util.parser.sql.lexer.SqlTokenKind;
import ru.inversion.util.parser.text.SourceText;
import ru.inversion.util.parser.text.TextChange;
import ru.inversion.util.parser.text.TextChangeApplier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Компилирует SQL в единый JDBC-вид.
 *
 * <p>Результирующий SQL использует только placeholder {@code ?}.
 * Одновременно строится полный список {@link SqlParameterBinding}
 * в порядке JDBC-позиций.</p>
 *
 * <p>Поддерживаются:</p>
 *
 * <ul>
 *     <li>исходные JDBC-параметры {@code ?};</li>
 *     <li>именованные параметры {@code :name};</li>
 *     <li>PostgreSQL-параметры {@code $1}, {@code $2};</li>
 *     <li>литералы, поддерживаемые
 *     {@link SqlLiteralParameterizer}.</li>
 * </ul>
 *
 * <p>Защитные участки отключают только замену литералов.
 * Уже существующие параметры внутри защищённого участка
 * всё равно входят в итоговый binding plan.</p>
 */
public final class SqlParameterCompiler {

    private static final String SENTINEL_PREFIX_BASE =
            "__sql_parser_jdbc_";

    private final SqlLexer lexer;
    private final SqlLiteralParameterizer literalParameterizer;

    public SqlParameterCompiler(
            SqlSyntaxDialect dialect
    ) {
        Objects.requireNonNull(
                dialect,
                "dialect"
        );

        this.lexer =
                new SqlLexer();

        this.literalParameterizer =
                new SqlLiteralParameterizer(
                        dialect
                );
    }

    /**
     * Компилирует SQL в JDBC-форму.
     *
     * @param sql исходный SQL
     *
     * @return SQL только с {@code ?} и полный binding plan
     */
    public PreparedSql compile(
            CharSequence sql
    ) {
        Objects.requireNonNull(
                sql,
                "sql"
        );

        MaskedSql maskedSql =
                maskOriginalJdbcParameters(
                        sql
                );

        ParameterizedSql parameterized =
                literalParameterizer.parameterize(
                        maskedSql.sql
                );

        return createPreparedSql(
                parameterized,
                maskedSql.jdbcSourcePositions
        );
    }

    /**
     * Временно заменяет исходные {@code ?} на уникальные
     * именованные параметры.
     *
     * <p>После прохода SqlLiteralParameterizer любой
     * настоящий {@code ?} в SQL гарантированно является
     * placeholder, созданным из литерала.</p>
     */
    private MaskedSql maskOriginalJdbcParameters(
            CharSequence sql
    ) {
        LexerResult<SqlTokenKind> lexerResult =
                lexer.tokenize(sql);

        SourceText source =
                lexerResult.source();

        List<Token<SqlTokenKind>> tokens =
                lexerResult.tokens();

        String sentinelPrefix =
                chooseSentinelPrefix(
                        tokens,
                        source
                );

        List<TextChange> changes =
                new ArrayList<TextChange>();

        Map<String, Integer> jdbcSourcePositions =
                new HashMap<String, Integer>();

        int sourcePosition = 0;

        for (Token<SqlTokenKind> token : tokens) {
            if (token.kind()
                    != SqlTokenKind.JDBC_PARAMETER) {

                continue;
            }

            sourcePosition++;

            String sentinelName =
                    sentinelPrefix
                            + sourcePosition;

            jdbcSourcePositions.put(
                    sentinelName,
                    sourcePosition
            );

            changes.add(
                    new TextChange(
                            token.range(),
                            ":" + sentinelName
                    )
            );
        }

        String masked =
                TextChangeApplier.apply(
                        sql,
                        changes
                );

        return new MaskedSql(
                masked,
                jdbcSourcePositions
        );
    }

    private PreparedSql createPreparedSql(
            ParameterizedSql parameterized,
            Map<String, Integer> jdbcSourcePositions
    ) {
        String sourceSql =
                parameterized.sql();

        LexerResult<SqlTokenKind> lexerResult =
                lexer.tokenize(sourceSql);

        SourceText source =
                lexerResult.source();

        List<TextChange> changes =
                new ArrayList<TextChange>();

        List<SqlParameterBinding> bindings =
                new ArrayList<SqlParameterBinding>();

        List<Object> generatedValues =
                parameterized.parameters();

        int generatedValueIndex = 0;
        int jdbcPosition = 0;

        for (Token<SqlTokenKind> token
                : lexerResult.tokens()) {

            SqlTokenKind kind =
                    token.kind();

            switch (kind) {
                case JDBC_PARAMETER:
                    jdbcPosition++;

                    if (generatedValueIndex
                            >= generatedValues.size()) {

                        throw generatedParameterMismatch(
                                generatedValues.size(),
                                generatedValueIndex + 1
                        );
                    }

                    bindings.add(
                            SqlParameterBinding
                                    .generatedLiteral(
                                            jdbcPosition,
                                            generatedValues.get(
                                                    generatedValueIndex
                                            )
                                    )
                    );

                    generatedValueIndex++;
                    break;

                case NAMED_PARAMETER:
                    jdbcPosition++;

                    String name =
                            parameterName(
                                    token,
                                    source
                            );

                    Integer sourcePosition =
                            jdbcSourcePositions.get(
                                    name
                            );

                    if (sourcePosition != null) {
                        bindings.add(
                                SqlParameterBinding
                                        .jdbcPositional(
                                                jdbcPosition,
                                                sourcePosition
                                        )
                        );
                    } else {
                        bindings.add(
                                SqlParameterBinding.named(
                                        jdbcPosition,
                                        name
                                )
                        );
                    }

                    changes.add(
                            jdbcReplacement(token)
                    );
                    break;

                case POSTGRES_POSITIONAL_PARAMETER:
                    jdbcPosition++;

                    bindings.add(
                            SqlParameterBinding
                                    .numberedPositional(
                                            jdbcPosition,
                                            postgresSourcePosition(
                                                    token,
                                                    source
                                            )
                                    )
                    );

                    changes.add(
                            jdbcReplacement(token)
                    );
                    break;

                default:
                    break;
            }
        }

        if (generatedValueIndex
                != generatedValues.size()) {

            throw generatedParameterMismatch(
                    generatedValues.size(),
                    generatedValueIndex
            );
        }

        String jdbcSql =
                TextChangeApplier.apply(
                        sourceSql,
                        changes
                );

        return new PreparedSql(
                jdbcSql,
                bindings
        );
    }

    private static String chooseSentinelPrefix(
            List<Token<SqlTokenKind>> tokens,
            SourceText source
    ) {
        int suffix = 0;

        while (true) {
            String candidate =
                    SENTINEL_PREFIX_BASE
                            + suffix
                            + "_";

            if (!hasNamedParameterWithPrefix(
                    tokens,
                    source,
                    candidate
            )) {
                return candidate;
            }

            suffix++;

            if (suffix < 0) {
                throw new IllegalStateException(
                        "Unable to allocate JDBC "
                                + "parameter sentinel prefix"
                );
            }
        }
    }

    private static boolean hasNamedParameterWithPrefix(
            List<Token<SqlTokenKind>> tokens,
            SourceText source,
            String prefix
    ) {
        for (Token<SqlTokenKind> token : tokens) {
            if (token.kind()
                    != SqlTokenKind.NAMED_PARAMETER) {

                continue;
            }

            String name =
                    parameterName(
                            token,
                            source
                    );

            if (name.startsWith(prefix)) {
                return true;
            }
        }

        return false;
    }

    private static String parameterName(
            Token<SqlTokenKind> token,
            SourceText source
    ) {
        String text =
                token.text(source);

        return text.substring(1);
    }

    private static int postgresSourcePosition(
            Token<SqlTokenKind> token,
            SourceText source
    ) {
        String text =
                token.text(source);

        String numberText =
                text.substring(1);

        int position;

        try {
            position =
                    Integer.parseInt(
                            numberText
                    );
        } catch (NumberFormatException exception) {
            throw invalidPostgresPosition(
                    text,
                    token.range().start(),
                    exception
            );
        }

        if (position < 1) {
            throw invalidPostgresPosition(
                    text,
                    token.range().start(),
                    null
            );
        }

        return position;
    }

    private static TextChange jdbcReplacement(
            Token<SqlTokenKind> token
    ) {
        return new TextChange(
                token.range(),
                "?"
        );
    }

    private static IllegalArgumentException
    invalidPostgresPosition(
            String parameterText,
            int offset,
            NumberFormatException cause
    ) {
        String message =
                "Invalid PostgreSQL positional parameter '"
                        + parameterText
                        + "' at offset "
                        + offset;

        return cause == null
                ? new IllegalArgumentException(message)
                : new IllegalArgumentException(
                        message,
                        cause
                );
    }

    private static IllegalStateException
    generatedParameterMismatch(
            int expected,
            int actual
    ) {
        return new IllegalStateException(
                "Generated parameter count mismatch: "
                        + "expected "
                        + expected
                        + ", actual "
                        + actual
        );
    }

    private static final class MaskedSql {

        private final String sql;
        private final Map<String, Integer>
                jdbcSourcePositions;

        private MaskedSql(
                String sql,
                Map<String, Integer>
                        jdbcSourcePositions
        ) {
            this.sql =
                    Objects.requireNonNull(
                            sql,
                            "sql"
                    );

            this.jdbcSourcePositions =
                    Objects.requireNonNull(
                            jdbcSourcePositions,
                            "jdbcSourcePositions"
                    );
        }
    }
}
