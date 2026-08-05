package ru.inversion.util.parser.sql.transform;

import org.junit.Test;

import java.math.BigInteger;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import static ru.inversion.util.parser.sql.dialect
        .TestSqlSyntaxDialects.NONE;

import static ru.inversion.util.parser.sql.dialect
        .TestSqlSyntaxDialects.POSTGRES;

public class SqlParameterCompilerTest {

    private final SqlParameterCompiler compiler =
            new SqlParameterCompiler(NONE);

    @Test
    public void returnsSqlWithoutBindingsWhenParametersAreAbsent() {
        PreparedSql result =
                compiler.compile(
                        "select current_timestamp from t"
                );

        assertEquals(
                "select current_timestamp from t",
                result.sql()
        );

        assertTrue(
                result.bindings().isEmpty()
        );

        assertTrue(
                result.namedPositions().isEmpty()
        );
    }

    @Test
    public void compilesAllParameterKindsInJdbcOrder() {
        PreparedSql result =
                compiler.compile(
                        "select ?, "
                                + ":userId, "
                                + ":userId, "
                                + "$2, "
                                + "'ACTIVE', "
                                + "10"
                );

        assertEquals(
                "select ?, ?, ?, ?, ?, ?",
                result.sql()
        );

        assertEquals(
                6,
                result.bindings().size()
        );

        assertBinding(
                result,
                1,
                SqlBindingKind.JDBC_POSITIONAL,
                null,
                1,
                null
        );

        assertBinding(
                result,
                2,
                SqlBindingKind.NAMED,
                "userId",
                null,
                null
        );

        assertBinding(
                result,
                3,
                SqlBindingKind.NAMED,
                "userId",
                null,
                null
        );

        assertBinding(
                result,
                4,
                SqlBindingKind.NUMBERED_POSITIONAL,
                null,
                2,
                null
        );

        assertBinding(
                result,
                5,
                SqlBindingKind.GENERATED_LITERAL,
                null,
                null,
                "ACTIVE"
        );

        assertBinding(
                result,
                6,
                SqlBindingKind.GENERATED_LITERAL,
                null,
                null,
                new BigInteger("10")
        );

        assertEquals(
                Arrays.asList(2, 3),
                result.positionsOf("userId")
        );
    }

    @Test
    public void assignsSourcePositionsToOriginalJdbcParameters() {
        PreparedSql result =
                compiler.compile(
                        "select ?, ?, ?"
                );

        assertEquals(
                "select ?, ?, ?",
                result.sql()
        );

        assertBinding(
                result,
                1,
                SqlBindingKind.JDBC_POSITIONAL,
                null,
                1,
                null
        );

        assertBinding(
                result,
                2,
                SqlBindingKind.JDBC_POSITIONAL,
                null,
                2,
                null
        );

        assertBinding(
                result,
                3,
                SqlBindingKind.JDBC_POSITIONAL,
                null,
                3,
                null
        );
    }

    @Test
    public void preservesRepeatedNumberedSourcePosition() {
        PreparedSql result =
                compiler.compile(
                        "select $2, $2, $1"
                );

        assertEquals(
                "select ?, ?, ?",
                result.sql()
        );

        assertBinding(
                result,
                1,
                SqlBindingKind.NUMBERED_POSITIONAL,
                null,
                2,
                null
        );

        assertBinding(
                result,
                2,
                SqlBindingKind.NUMBERED_POSITIONAL,
                null,
                2,
                null
        );

        assertBinding(
                result,
                3,
                SqlBindingKind.NUMBERED_POSITIONAL,
                null,
                1,
                null
        );
    }

    @Test
    public void compilesParametersInsideDirectiveProtectedSection() {
        String sql =
                "select "
                        + "/* @parameterize:off */ "
                        + ":userId, 'protected', ? "
                        + "/* @parameterize:on */, "
                        + "'generated'";

        PreparedSql result =
                compiler.compile(sql);

        assertEquals(
                "select "
                        + "/* @parameterize:off */ "
                        + "?, 'protected', ? "
                        + "/* @parameterize:on */, "
                        + "?",
                result.sql()
        );

        assertBinding(
                result,
                1,
                SqlBindingKind.NAMED,
                "userId",
                null,
                null
        );

        assertBinding(
                result,
                2,
                SqlBindingKind.JDBC_POSITIONAL,
                null,
                1,
                null
        );

        assertBinding(
                result,
                3,
                SqlBindingKind.GENERATED_LITERAL,
                null,
                null,
                "generated"
        );
    }

    @Test
    public void compilesParametersInsideLegacyProtectedSection() {
        PreparedSql result =
                compiler.compile(
                        "select "
                                + "~ :userId, 'protected', ? ~, "
                                + "'generated'"
                );

        assertEquals(
                "select "
                        + " ?, 'protected', ? , "
                        + "?",
                result.sql()
        );

        assertBinding(
                result,
                1,
                SqlBindingKind.NAMED,
                "userId",
                null,
                null
        );

        assertBinding(
                result,
                2,
                SqlBindingKind.JDBC_POSITIONAL,
                null,
                1,
                null
        );

        assertBinding(
                result,
                3,
                SqlBindingKind.GENERATED_LITERAL,
                null,
                null,
                "generated"
        );
    }

    @Test
    public void ignoresParameterTextInsideCommentsAndHints() {
        String sql =
                "select "
                        + "/* :comment $1 ? 'literal' */ "
                        + "/*+ SOME_HINT(:hint $2 ? 10) */ "
                        + ":outside, 'value'";

        PreparedSql result =
                compiler.compile(sql);

        assertEquals(
                "select "
                        + "/* :comment $1 ? 'literal' */ "
                        + "/*+ SOME_HINT(:hint $2 ? 10) */ "
                        + "?, ?",
                result.sql()
        );

        assertEquals(
                2,
                result.bindings().size()
        );

        assertBinding(
                result,
                1,
                SqlBindingKind.NAMED,
                "outside",
                null,
                null
        );

        assertBinding(
                result,
                2,
                SqlBindingKind.GENERATED_LITERAL,
                null,
                null,
                "value"
        );
    }

    @Test
    public void parameterizesSupportedDialectLiteral() {
        SqlParameterCompiler postgres =
                new SqlParameterCompiler(
                        POSTGRES
                );

        PreparedSql result =
                postgres.compile(
                        "select :name, $body$text$body$"
                );

        assertEquals(
                "select ?, ?",
                result.sql()
        );

        assertBinding(
                result,
                1,
                SqlBindingKind.NAMED,
                "name",
                null,
                null
        );

        assertBinding(
                result,
                2,
                SqlBindingKind.GENERATED_LITERAL,
                null,
                null,
                "text"
        );
    }

    @Test
    public void avoidsCollisionWithInternalSentinelName() {
        PreparedSql result =
                compiler.compile(
                        "select "
                                + ":__sql_parser_jdbc_0_1, "
                                + "?"
                );

        assertEquals(
                "select ?, ?",
                result.sql()
        );

        assertBinding(
                result,
                1,
                SqlBindingKind.NAMED,
                "__sql_parser_jdbc_0_1",
                null,
                null
        );

        assertBinding(
                result,
                2,
                SqlBindingKind.JDBC_POSITIONAL,
                null,
                1,
                null
        );
    }

    @Test
    public void rejectsZeroPostgresPosition() {
        String sql =
                "select $0";

        try {
            compiler.compile(sql);

            fail(
                    "Expected invalid PostgreSQL "
                            + "position to be rejected"
            );
        } catch (IllegalArgumentException expected) {
            assertEquals(
                    "Invalid PostgreSQL positional "
                            + "parameter '$0' at offset "
                            + sql.indexOf("$0"),
                    expected.getMessage()
            );
        }
    }

    @Test(expected = NullPointerException.class)
    public void rejectsNullSql() {
        compiler.compile(null);
    }

    @Test(expected = NullPointerException.class)
    public void rejectsNullDialect() {
        new SqlParameterCompiler(null);
    }

    private static void assertBinding(
            PreparedSql result,
            int jdbcPosition,
            SqlBindingKind kind,
            String name,
            Integer sourcePosition,
            Object generatedValue
    ) {
        SqlParameterBinding binding =
                result.bindings().get(
                        jdbcPosition - 1
                );

        assertEquals(
                jdbcPosition,
                binding.jdbcPosition()
        );

        assertEquals(
                kind,
                binding.kind()
        );

        assertEquals(
                name,
                binding.name()
        );

        assertEquals(
                sourcePosition,
                binding.sourcePosition()
        );

        assertEquals(
                generatedValue,
                binding.generatedValue()
        );
    }
}
