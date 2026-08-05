package ru.inversion.util.parser.sql.transform;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class SqlParameterBindingTest {

    @Test
    public void createsJdbcPositionalBinding() {
        SqlParameterBinding binding =
                SqlParameterBinding.jdbcPositional(
                        3,
                        2
                );

        assertEquals(
                3,
                binding.jdbcPosition()
        );

        assertEquals(
                SqlBindingKind.JDBC_POSITIONAL,
                binding.kind()
        );

        assertEquals(
                Integer.valueOf(2),
                binding.sourcePosition()
        );

        assertNull(
                binding.name()
        );

        assertNull(
                binding.generatedValue()
        );

        assertFalse(
                binding.isNamed()
        );

        assertFalse(
                binding.isGenerated()
        );
    }

    @Test
    public void createsNamedBinding() {
        SqlParameterBinding binding =
                SqlParameterBinding.named(
                        2,
                        "userId"
                );

        assertEquals(
                2,
                binding.jdbcPosition()
        );

        assertEquals(
                SqlBindingKind.NAMED,
                binding.kind()
        );

        assertEquals(
                "userId",
                binding.name()
        );

        assertNull(
                binding.sourcePosition()
        );

        assertTrue(
                binding.isNamed()
        );

        assertFalse(
                binding.isGenerated()
        );
    }

    @Test
    public void createsNumberedPositionalBinding() {
        SqlParameterBinding binding =
                SqlParameterBinding
                        .numberedPositional(
                                4,
                                2
                        );

        assertEquals(
                4,
                binding.jdbcPosition()
        );

        assertEquals(
                SqlBindingKind.NUMBERED_POSITIONAL,
                binding.kind()
        );

        assertEquals(
                Integer.valueOf(2),
                binding.sourcePosition()
        );
    }

    @Test
    public void createsGeneratedLiteralBinding() {
        SqlParameterBinding binding =
                SqlParameterBinding.generatedLiteral(
                        1,
                        "ACTIVE"
                );

        assertEquals(
                SqlBindingKind.GENERATED_LITERAL,
                binding.kind()
        );

        assertEquals(
                "ACTIVE",
                binding.generatedValue()
        );

        assertTrue(
                binding.isGenerated()
        );
    }

    @Test
    public void allowsNullGeneratedLiteralValue() {
        SqlParameterBinding binding =
                SqlParameterBinding.generatedLiteral(
                        1,
                        null
                );

        assertTrue(
                binding.isGenerated()
        );

        assertNull(
                binding.generatedValue()
        );
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsZeroJdbcPosition() {
        SqlParameterBinding.generatedLiteral(
                0,
                "ACTIVE"
        );
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsNegativeJdbcPosition() {
        SqlParameterBinding.generatedLiteral(
                -1,
                "ACTIVE"
        );
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsZeroJdbcSourcePosition() {
        SqlParameterBinding.jdbcPositional(
                1,
                0
        );
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsZeroNumberedSourcePosition() {
        SqlParameterBinding.numberedPositional(
                1,
                0
        );
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsEmptyNamedParameter() {
        SqlParameterBinding.named(
                1,
                ""
        );
    }

    @Test(expected = NullPointerException.class)
    public void rejectsNullNamedParameter() {
        SqlParameterBinding.named(
                1,
                null
        );
    }
}