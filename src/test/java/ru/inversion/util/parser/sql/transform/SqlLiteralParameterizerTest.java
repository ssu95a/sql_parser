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

public class SqlLiteralParameterizerTest {

    private final SqlLiteralParameterizer parameterizer =
            new SqlLiteralParameterizer(NONE);

    @Test
    public void returnsUnchangedSqlWhenLiteralsAreAbsent() {
        ParameterizedSql result =
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
        ParameterizedSql result =
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
        ParameterizedSql result =
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
        ParameterizedSql result =
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
        ParameterizedSql result =
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
        ParameterizedSql result =
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
        ParameterizedSql result =
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
        ParameterizedSql result =
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
        ParameterizedSql result =
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
        ParameterizedSql result =
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
        ParameterizedSql result =
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
        ParameterizedSql result =
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
        ParameterizedSql result =
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

        ParameterizedSql result =
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

        ParameterizedSql result =
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
        ParameterizedSql result =
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

        ParameterizedSql result =
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

        ParameterizedSql result =
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

        ParameterizedSql result =
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

        ParameterizedSql result =
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
}