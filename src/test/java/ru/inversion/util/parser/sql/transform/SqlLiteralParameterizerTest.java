package ru.inversion.util.parser.sql.transform;

import org.junit.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import static ru.inversion.util.parser.sql.dialect
        .TestSqlSyntaxDialects.NONE;

import static ru.inversion.util.parser.sql.dialect
        .TestSqlSyntaxDialects.ORACLE;
import static ru.inversion.util.parser.sql.dialect
        .TestSqlSyntaxDialects.POSTGRES;

public class SqlLiteralParameterizerTest {

    private final SqlLiteralParameterizer parameterizer =
            new SqlLiteralParameterizer(NONE);

    @Test
    public void returnsUnchangedSqlWhenLiteralsAreAbsent() {
        PreparedSql result =
                parameterizer.parameterize(
                        "select c1 from t"
                );

        assertEquals(
                "select c1 from t",
                result.sql()
        );

        assertTrue(
                result.parameters().isEmpty()
        );
    }

    @Test
    public void parameterizesStringLiteral() {
        PreparedSql result =
                parameterizer.parameterize(
                        "select 'text' from t"
                );

        assertEquals(
                "select ? from t",
                result.sql()
        );

        assertEquals(
                Collections.<Object>singletonList(
                        "text"
                ),
                result.parameters()
        );
    }

    @Test
    public void parameterizesEmptyStringLiteral() {
        PreparedSql result =
                parameterizer.parameterize(
                        "select '' from t"
                );

        assertEquals(
                "select ? from t",
                result.sql()
        );

        assertEquals(
                Collections.<Object>singletonList(
                        ""
                ),
                result.parameters()
        );
    }

    @Test
    public void decodesEscapedSingleQuote() {
        PreparedSql result =
                parameterizer.parameterize(
                        "select 'John''s car' from t"
                );

        assertEquals(
                "select ? from t",
                result.sql()
        );

        assertEquals(
                Collections.<Object>singletonList(
                        "John's car"
                ),
                result.parameters()
        );
    }

    @Test
    public void parameterizesIntegerLiteral() {
        PreparedSql result =
                parameterizer.parameterize(
                        "select 123 from t"
                );

        assertEquals(
                "select ? from t",
                result.sql()
        );

        assertEquals(
                Collections.<Object>singletonList(
                        new BigInteger("123")
                ),
                result.parameters()
        );
    }

    @Test
    public void preservesArbitrarilyLargeInteger() {
        PreparedSql result =
                parameterizer.parameterize(
                        "select 123456789012345678901234567890"
                );

        assertEquals(
                "select ?",
                result.sql()
        );

        assertEquals(
                Collections.<Object>singletonList(
                        new BigInteger(
                                "123456789012345678901234567890"
                        )
                ),
                result.parameters()
        );
    }

    @Test
    public void parameterizesDecimalLiteral() {
        PreparedSql result =
                parameterizer.parameterize(
                        "select 123.45 from t"
                );

        assertEquals(
                "select ? from t",
                result.sql()
        );

        assertEquals(
                Collections.<Object>singletonList(
                        new BigDecimal("123.45")
                ),
                result.parameters()
        );
    }

    @Test
    public void parameterizesDecimalWithoutIntegerPart() {
        PreparedSql result =
                parameterizer.parameterize(
                        "select .45 from t"
                );

        assertEquals(
                "select ? from t",
                result.sql()
        );

        assertEquals(
                Collections.<Object>singletonList(
                        new BigDecimal(".45")
                ),
                result.parameters()
        );
    }

    @Test
    public void parameterizesDecimalWithoutFractionPart() {
        PreparedSql result =
                parameterizer.parameterize(
                        "select 123. from t"
                );

        assertEquals(
                "select ? from t",
                result.sql()
        );

        assertEquals(
                Collections.<Object>singletonList(
                        new BigDecimal("123.")
                ),
                result.parameters()
        );
    }

    @Test
    public void preservesLiteralOrder() {
        PreparedSql result =
                parameterizer.parameterize(
                        "select 'first', 10, 'second', 20.5"
                );

        assertEquals(
                "select ?, ?, ?, ?",
                result.sql()
        );

        assertEquals(
                Arrays.<Object>asList(
                        "first",
                        new BigInteger("10"),
                        "second",
                        new BigDecimal("20.5")
                ),
                result.parameters()
        );
    }

    @Test
    public void keepsUnaryMinusOutsideParameter() {
        PreparedSql result =
                parameterizer.parameterize(
                        "select -10, +2.5"
                );

        assertEquals(
                "select -?, +?",
                result.sql()
        );

        assertEquals(
                Arrays.<Object>asList(
                        new BigInteger("10"),
                        new BigDecimal("2.5")
                ),
                result.parameters()
        );
    }

    @Test
    public void preservesExistingParameters() {
        PreparedSql result =
                parameterizer.parameterize(
                        "select ?, :name, $1, 'text', 10"
                );

        assertEquals(
                "select ?, :name, $1, ?, ?",
                result.sql()
        );

        assertEquals(
                Arrays.<Object>asList(
                        "text",
                        new BigInteger("10")
                ),
                result.parameters()
        );
    }

    @Test
    public void ignoresQuotedIdentifier() {
        PreparedSql result =
                parameterizer.parameterize(
                        "select \"123\", \"'text'\", 10"
                );

        assertEquals(
                "select \"123\", \"'text'\", ?",
                result.sql()
        );

        assertEquals(
                Collections.<Object>singletonList(
                        new BigInteger("10")
                ),
                result.parameters()
        );
    }

    @Test
    public void ignoresLiteralsInsideComments() {
        String sql =
                "select 'text', 10 "
                        + "-- 'line text' 20\n"
                        + "from t "
                        + "/* 'block text' 30 */";

        PreparedSql result =
                parameterizer.parameterize(sql);

        assertEquals(
                "select ?, ? "
                        + "-- 'line text' 20\n"
                        + "from t "
                        + "/* 'block text' 30 */",
                result.sql()
        );

        assertEquals(
                Arrays.<Object>asList(
                        "text",
                        new BigInteger("10")
                ),
                result.parameters()
        );
    }

    @Test
    public void preservesFormattingAroundLiterals() {
        String sql =
                "select\n"
                        + "    'text'  as value,\n"
                        + "    10      as number\n"
                        + "from t";

        PreparedSql result =
                parameterizer.parameterize(sql);

        assertEquals(
                "select\n"
                        + "    ?  as value,\n"
                        + "    ?      as number\n"
                        + "from t",
                result.sql()
        );
    }

    @Test
    public void parameterizesLiteralsAtAnyParenthesisDepth() {
        PreparedSql result =
                parameterizer.parameterize(
                        "select * from t "
                                + "where id in ("
                                + "select id from nested "
                                + "where name = 'nested' "
                                + "and value = 10"
                                + ")"
                );

        assertEquals(
                "select * from t "
                        + "where id in ("
                        + "select id from nested "
                        + "where name = ? "
                        + "and value = ?"
                        + ")",
                result.sql()
        );

        assertEquals(
                Arrays.<Object>asList(
                        "nested",
                        new BigInteger("10")
                ),
                result.parameters()
        );
    }

    @Test
    public void leavesPrefixedStringLiteralsUnchanged() {
        String sql =
                "select "
                        + "E'postgres', "
                        + "N'national', "
                        + "q'[oracle]', "
                        + "U&'unicode'";

        PreparedSql result =
                parameterizer.parameterize(sql);

        assertEquals(
                sql,
                result.sql()
        );

        assertTrue(
                result.parameters().isEmpty()
        );
    }

    @Test
    public void leavesTypedStringLiteralsUnchanged() {
        String sql =
                "select "
                        + "date '2026-08-04', "
                        + "timestamp '2026-08-04 12:30:00', "
                        + "interval '1 day'";

        PreparedSql result =
                parameterizer.parameterize(sql);

        assertEquals(
                sql,
                result.sql()
        );

        assertTrue(
                result.parameters().isEmpty()
        );
    }

    @Test
    public void recognizesTypedLiteralAcrossCommentTrivia() {
        String sql =
                "select date /* type comment */ '2026-08-04'";

        PreparedSql result =
                parameterizer.parameterize(sql);

        assertEquals(
                sql,
                result.sql()
        );

        assertTrue(
                result.parameters().isEmpty()
        );
    }

    @Test
    public void leavesUnsupportedNumericFormsUnchanged() {
        String sql =
                "select 1e3, 0xFF, 1_000, :1";

        PreparedSql result =
                parameterizer.parameterize(sql);

        assertEquals(
                sql,
                result.sql()
        );

        assertTrue(
                result.parameters().isEmpty()
        );
    }

    @Test
    public void rejectsUnterminatedBareStringLiteral() {
        String sql =
                "select 'unterminated";

        try {
            parameterizer.parameterize(sql);

            fail(
                    "Expected unterminated string "
                            + "to be rejected"
            );
        } catch (IllegalArgumentException expected) {
            assertEquals(
                    "Unterminated string literal at offset "
                            + sql.indexOf('\''),
                    expected.getMessage()
            );
        }
    }

    @Test(expected = NullPointerException.class)
    public void rejectsNullSql() {
        parameterizer.parameterize(null);
    }

    @Test
    public void postgresParameterizesUntaggedDollarQuotedString() {
        SqlLiteralParameterizer postgres =
                new SqlLiteralParameterizer(
                        POSTGRES
                );

        PreparedSql result =
                postgres.parameterize(
                        "select $$text$$ from t"
                );

        assertEquals(
                "select ? from t",
                result.sql()
        );

        assertEquals(
                Collections.<Object>singletonList(
                        "text"
                ),
                result.parameters()
        );
    }

    @Test
    public void postgresParameterizesTaggedDollarQuotedString() {
        SqlLiteralParameterizer postgres =
                new SqlLiteralParameterizer(
                        POSTGRES
                );

        PreparedSql result =
                postgres.parameterize(
                        "select $body$John's car$body$"
                );

        assertEquals(
                "select ?",
                result.sql()
        );

        assertEquals(
                Collections.<Object>singletonList(
                        "John's car"
                ),
                result.parameters()
        );
    }

    @Test
    public void postgresDoesNotParameterizeOracleQQuotedString() {
        SqlLiteralParameterizer postgres =
                new SqlLiteralParameterizer(
                        POSTGRES
                );

        String sql =
                "select q'[oracle text]'";

        PreparedSql result =
                postgres.parameterize(sql);

        assertEquals(
                sql,
                result.sql()
        );

        assertTrue(
                result.parameters().isEmpty()
        );
    }

    @Test
    public void oracleParameterizesSquareBracketQuotedString() {
        SqlLiteralParameterizer oracle =
                new SqlLiteralParameterizer(
                        ORACLE
                );

        PreparedSql result =
                oracle.parameterize(
                        "select q'[text]' from dual"
                );

        assertEquals(
                "select ? from dual",
                result.sql()
        );

        assertEquals(
                Collections.<Object>singletonList(
                        "text"
                ),
                result.parameters()
        );
    }

    @Test
    public void oracleParameterizesCustomDelimiterQuotedString() {
        SqlLiteralParameterizer oracle =
                new SqlLiteralParameterizer(
                        ORACLE
                );

        PreparedSql result =
                oracle.parameterize(
                        "select q'!John's car!' from dual"
                );

        assertEquals(
                "select ? from dual",
                result.sql()
        );

        assertEquals(
                Collections.<Object>singletonList(
                        "John's car"
                ),
                result.parameters()
        );
    }

    @Test
    public void oracleDoesNotParameterizePostgresDollarQuotedString() {
        SqlLiteralParameterizer oracle =
                new SqlLiteralParameterizer(
                        ORACLE
                );

        String sql =
                "select $$postgres text$$ from dual";

        PreparedSql result =
                oracle.parameterize(sql);

        assertEquals(
                sql,
                result.sql()
        );

        assertTrue(
                result.parameters().isEmpty()
        );
    }

    @Test
    public void noneDoesNotParameterizeDialectStrings() {
        String sql =
                "select $$postgres$$, q'[oracle]'";

        PreparedSql result =
                parameterizer.parameterize(sql);

        assertEquals(
                sql,
                result.sql()
        );

        assertTrue(
                result.parameters().isEmpty()
        );
    }

    @Test
    public void ordinaryStringsWorkForPostgresAndOracle() {
        SqlLiteralParameterizer postgres =
                new SqlLiteralParameterizer(
                        POSTGRES
                );

        SqlLiteralParameterizer oracle =
                new SqlLiteralParameterizer(
                        ORACLE
                );

        PreparedSql postgresResult =
                postgres.parameterize(
                        "select 'text'"
                );

        PreparedSql oracleResult =
                oracle.parameterize(
                        "select 'text'"
                );

        assertEquals(
                "select ?",
                postgresResult.sql()
        );

        assertEquals(
                "select ?",
                oracleResult.sql()
        );

        assertEquals(
                Collections.<Object>singletonList(
                        "text"
                ),
                postgresResult.parameters()
        );

        assertEquals(
                Collections.<Object>singletonList(
                        "text"
                ),
                oracleResult.parameters()
        );
    }

    @Test
    public void preservesMixedPostgresLiteralOrder() {
        SqlLiteralParameterizer postgres =
                new SqlLiteralParameterizer(
                        POSTGRES
                );

        PreparedSql result =
                postgres.parameterize(
                        "select 'first', "
                                + "$body$second$body$, "
                                + "10, "
                                + "20.5"
                );

        assertEquals(
                "select ?, ?, ?, ?",
                result.sql()
        );

        assertEquals(
                Arrays.<Object>asList(
                        "first",
                        "second",
                        new BigInteger("10"),
                        new BigDecimal("20.5")
                ),
                result.parameters()
        );
    }

    @Test
    public void preservesMixedOracleLiteralOrder() {
        SqlLiteralParameterizer oracle =
                new SqlLiteralParameterizer(
                        ORACLE
                );

        PreparedSql result =
                oracle.parameterize(
                        "select 'first', "
                                + "q'[second]', "
                                + "10 "
                                + "from dual"
                );

        assertEquals(
                "select ?, ?, ? from dual",
                result.sql()
        );

        assertEquals(
                Arrays.<Object>asList(
                        "first",
                        "second",
                        new BigInteger("10")
                ),
                result.parameters()
        );
    }

    @Test
    public void rejectsUnterminatedPostgresDollarQuotedString() {
        SqlLiteralParameterizer postgres =
                new SqlLiteralParameterizer(
                        POSTGRES
                );

        String sql =
                "select $body$unterminated";

        try {
            postgres.parameterize(sql);

            fail(
                    "Expected unterminated PostgreSQL "
                            + "string to be rejected"
            );
        } catch (IllegalArgumentException expected) {
            assertEquals(
                    "Unterminated PostgreSQL "
                            + "dollar-quoted string at offset "
                            + sql.indexOf("$body$"),
                    expected.getMessage()
            );
        }
    }

    @Test
    public void rejectsUnterminatedOracleQQuotedString() {
        SqlLiteralParameterizer oracle =
                new SqlLiteralParameterizer(
                        ORACLE
                );

        String sql =
                "select q'[unterminated";

        try {
            oracle.parameterize(sql);

            fail(
                    "Expected unterminated Oracle "
                            + "string to be rejected"
            );
        } catch (IllegalArgumentException expected) {
            assertEquals(
                    "Unterminated Oracle "
                            + "q-quoted string at offset "
                            + sql.indexOf("q'"),
                    expected.getMessage()
            );
        }
    }

    @Test
    public void unsupportedDialectDoesNotValidateForeignString() {
        String postgresSql =
                "select $body$unterminated";

        String oracleSql =
                "select q'[unterminated";

        PreparedSql nonePostgresResult =
                parameterizer.parameterize(
                        postgresSql
                );

        PreparedSql noneOracleResult =
                parameterizer.parameterize(
                        oracleSql
                );

        assertEquals(
                postgresSql,
                nonePostgresResult.sql()
        );

        assertEquals(
                oracleSql,
                noneOracleResult.sql()
        );
    }

    @Test
    public void skipsLiteralsInsideProtectedSection() {
        String sql =
                "select 'before', "
                        + "/* @parameterize:off */ "
                        + "'protected', 10, 20.5 "
                        + "/* @parameterize:on */, "
                        + "'after', 30";

        PreparedSql result =
                parameterizer.parameterize(sql);

        assertEquals(
                "select ?, "
                        + "/* @parameterize:off */ "
                        + "'protected', 10, 20.5 "
                        + "/* @parameterize:on */, "
                        + "?, ?",
                result.sql()
        );

        assertEquals(
                Arrays.<Object>asList(
                        "before",
                        "after",
                        new BigInteger("30")
                ),
                result.parameters()
        );
    }

    @Test
    public void preservesDirectiveFormatting() {
        String sql =
                "select 'before', "
                        + "/*\n"
                        + "    @PARAMETERIZE : OFF\n"
                        + "*/ "
                        + "'protected', 10 "
                        + "/* @Parameterize : On */, "
                        + "'after'";

        PreparedSql result =
                parameterizer.parameterize(sql);

        assertEquals(
                "select ?, "
                        + "/*\n"
                        + "    @PARAMETERIZE : OFF\n"
                        + "*/ "
                        + "'protected', 10 "
                        + "/* @Parameterize : On */, "
                        + "?",
                result.sql()
        );

        assertEquals(
                Arrays.<Object>asList(
                        "before",
                        "after"
                ),
                result.parameters()
        );
    }

    @Test
    public void supportsMultipleProtectedSections() {
        String sql =
                "select "
                        + "'first', "
                        + "/* @parameterize:off */ "
                        + "'keep1' "
                        + "/* @parameterize:on */, "
                        + "'second', "
                        + "/* @parameterize:off */ "
                        + "10 "
                        + "/* @parameterize:on */, "
                        + "20";

        PreparedSql result =
                parameterizer.parameterize(sql);

        assertEquals(
                "select "
                        + "?, "
                        + "/* @parameterize:off */ "
                        + "'keep1' "
                        + "/* @parameterize:on */, "
                        + "?, "
                        + "/* @parameterize:off */ "
                        + "10 "
                        + "/* @parameterize:on */, "
                        + "?",
                result.sql()
        );

        assertEquals(
                Arrays.<Object>asList(
                        "first",
                        "second",
                        new BigInteger("20")
                ),
                result.parameters()
        );
    }

    @Test
    public void supportsEmptyProtectedSection() {
        String sql =
                "select 'before', "
                        + "/* @parameterize:off */"
                        + "/* @parameterize:on */, "
                        + "'after'";

        PreparedSql result =
                parameterizer.parameterize(sql);

        assertEquals(
                "select ?, "
                        + "/* @parameterize:off */"
                        + "/* @parameterize:on */, "
                        + "?",
                result.sql()
        );

        assertEquals(
                Arrays.<Object>asList(
                        "before",
                        "after"
                ),
                result.parameters()
        );
    }

    @Test
    public void skipsDialectLiteralInsideProtectedSection() {
        SqlLiteralParameterizer postgres =
                new SqlLiteralParameterizer(
                        POSTGRES
                );

        String sql =
                "select "
                        + "$$before$$, "
                        + "/* @parameterize:off */ "
                        + "$tag$protected$tag$, 10 "
                        + "/* @parameterize:on */, "
                        + "$$after$$";

        PreparedSql result =
                postgres.parameterize(sql);

        assertEquals(
                "select "
                        + "?, "
                        + "/* @parameterize:off */ "
                        + "$tag$protected$tag$, 10 "
                        + "/* @parameterize:on */, "
                        + "?",
                result.sql()
        );

        assertEquals(
                Arrays.<Object>asList(
                        "before",
                        "after"
                ),
                result.parameters()
        );
    }

    @Test
    public void directiveTextInsideStringDoesNotChangeState() {
        String sql =
                "select "
                        + "'/* @parameterize:off */', "
                        + "10";

        PreparedSql result =
                parameterizer.parameterize(sql);

        assertEquals(
                "select ?, ?",
                result.sql()
        );

        assertEquals(
                Arrays.<Object>asList(
                        "/* @parameterize:off */",
                        new BigInteger("10")
                ),
                result.parameters()
        );
    }

    @Test
    public void directiveTextInsideOrdinaryCommentDoesNotChangeState() {
        String sql =
                "select "
                        + "/* explanation: "
                        + "@parameterize:off */ "
                        + "'text', 10";

        PreparedSql result =
                parameterizer.parameterize(sql);

        assertEquals(
                "select "
                        + "/* explanation: "
                        + "@parameterize:off */ "
                        + "?, ?",
                result.sql()
        );

        assertEquals(
                Arrays.<Object>asList(
                        "text",
                        new BigInteger("10")
                ),
                result.parameters()
        );
    }

    @Test
    public void rejectsParameterizeOnWithoutOff() {
        String sql =
                "select "
                        + "/* @parameterize:on */ "
                        + "'text'";

        int directiveOffset =
                sql.indexOf(
                        "/* @parameterize:on */"
                );

        try {
            parameterizer.parameterize(sql);

            fail(
                    "Expected unexpected on directive "
                            + "to be rejected"
            );
        } catch (IllegalArgumentException expected) {
            assertEquals(
                    "Unexpected @parameterize:on "
                            + "directive at offset "
                            + directiveOffset,
                    expected.getMessage()
            );
        }
    }

    @Test
    public void rejectsNestedParameterizeOff() {
        String sql =
                "select "
                        + "/* @parameterize:off */ "
                        + "'first' "
                        + "/* @parameterize:off */ "
                        + "'second' "
                        + "/* @parameterize:on */";

        int secondDirectiveOffset =
                sql.lastIndexOf(
                        "/* @parameterize:off */"
                );

        try {
            parameterizer.parameterize(sql);

            fail(
                    "Expected nested off directive "
                            + "to be rejected"
            );
        } catch (IllegalArgumentException expected) {
            assertEquals(
                    "Nested @parameterize:off "
                            + "directive at offset "
                            + secondDirectiveOffset,
                    expected.getMessage()
            );
        }
    }

    @Test
    public void rejectsUnclosedParameterizeOff() {
        String sql =
                "select 'before', "
                        + "/* @parameterize:off */ "
                        + "'protected', 10";

        int directiveOffset =
                sql.indexOf(
                        "/* @parameterize:off */"
                );

        try {
            parameterizer.parameterize(sql);

            fail(
                    "Expected unclosed off directive "
                            + "to be rejected"
            );
        } catch (IllegalArgumentException expected) {
            assertEquals(
                    "Unclosed @parameterize:off "
                            + "directive at offset "
                            + directiveOffset,
                    expected.getMessage()
            );
        }
    }

    @Test
    public void preservesOptimizerHintWithoutParsingItsContents() {
        String sql =
                "select "
                        + "/*+ CARDINALITY(t 100) "
                        + "SOME_HINT(~ 'inside') */ "
                        + "'outside', 20 "
                        + "from table_name t";

        PreparedSql result =
                parameterizer.parameterize(sql);

        assertEquals(
                "select "
                        + "/*+ CARDINALITY(t 100) "
                        + "SOME_HINT(~ 'inside') */ "
                        + "?, ? "
                        + "from table_name t",
                result.sql()
        );

        assertEquals(
                Arrays.<Object>asList(
                        "outside",
                        new BigInteger("20")
                ),
                result.parameters()
        );
    }
}