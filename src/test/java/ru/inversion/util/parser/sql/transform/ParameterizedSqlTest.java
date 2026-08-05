package ru.inversion.util.parser.sql.transform;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ParameterizedSqlTest  {

    @Test
    public void storesSqlAndBindingsInJdbcOrder() {
        SqlParameterBinding first =
                SqlParameterBinding.jdbcPositional(
                        1,
                        1
                );

        SqlParameterBinding second =
                SqlParameterBinding.generatedLiteral(
                        2,
                        "ACTIVE"
                );

        PreparedSql result =
                new PreparedSql(
                        "select * from t "
                                + "where id = ? "
                                + "and status = ?",
                        Arrays.asList(
                                first,
                                second
                        )
                );

        assertEquals(
                "select * from t "
                        + "where id = ? "
                        + "and status = ?",
                result.sql()
        );

        assertEquals(
                2,
                result.bindings().size()
        );

        assertSame(
                first,
                result.bindings().get(0)
        );

        assertSame(
                second,
                result.bindings().get(1)
        );
    }

    @Test
    public void copiesBindingList() {
        List<SqlParameterBinding> bindings =
                new ArrayList<SqlParameterBinding>();

        bindings.add(
                SqlParameterBinding.generatedLiteral(
                        1,
                        "ACTIVE"
                )
        );

        PreparedSql result =
                new PreparedSql(
                        "select ?",
                        bindings
                );

        bindings.add(
                SqlParameterBinding.generatedLiteral(
                        2,
                        "DELETED"
                )
        );

        assertEquals(
                1,
                result.bindings().size()
        );
    }

    @Test
    public void exposesImmutableBindingList() {
        PreparedSql result =
                new PreparedSql(
                        "select ?",
                        Collections.singletonList(
                                SqlParameterBinding
                                        .generatedLiteral(
                                                1,
                                                "ACTIVE"
                                        )
                        )
                );

        try {
            result.bindings().add(
                    SqlParameterBinding.generatedLiteral(
                            2,
                            "DELETED"
                    )
            );

            fail(
                    "Expected immutable binding list"
            );
        } catch (UnsupportedOperationException expected) {
            // Expected.
        }
    }

    @Test
    public void mapsRepeatedNamedParameterToAllPositions() {
        PreparedSql result =
                new PreparedSql(
                        "select * from t "
                                + "where manager_id = ? "
                                + "and status = ? "
                                + "and owner_id = ?",
                        Arrays.asList(
                                SqlParameterBinding.named(
                                        1,
                                        "userId"
                                ),
                                SqlParameterBinding
                                        .generatedLiteral(
                                                2,
                                                "ACTIVE"
                                        ),
                                SqlParameterBinding.named(
                                        3,
                                        "userId"
                                )
                        )
                );

        assertEquals(
                Arrays.asList(1, 3),
                result.positionsOf("userId")
        );

        assertEquals(
                Arrays.asList(1, 3),
                result.namedPositions()
                        .get("userId")
        );
    }

    @Test
    public void preservesNamedParameterOrder() {
        PreparedSql result =
                new PreparedSql(
                        "select ?, ?, ?",
                        Arrays.asList(
                                SqlParameterBinding.named(
                                        1,
                                        "second"
                                ),
                                SqlParameterBinding.named(
                                        2,
                                        "first"
                                ),
                                SqlParameterBinding.named(
                                        3,
                                        "second"
                                )
                        )
                );

        List<String> names =
                new ArrayList<String>(
                        result.namedPositions()
                                .keySet()
                );

        assertEquals(
                Arrays.asList(
                        "second",
                        "first"
                ),
                names
        );
    }

    @Test
    public void exposesImmutableNamedPositionMap() {
        PreparedSql result =
                new PreparedSql(
                        "select ?",
                        Collections.singletonList(
                                SqlParameterBinding.named(
                                        1,
                                        "userId"
                                )
                        )
                );

        Map<String, List<Integer>> positions =
                result.namedPositions();

        try {
            positions.put(
                    "other",
                    Collections.singletonList(2)
            );

            fail(
                    "Expected immutable named position map"
            );
        } catch (UnsupportedOperationException expected) {
            // Expected.
        }
    }

    @Test
    public void exposesImmutableNamedPositionList() {
        PreparedSql result =
                new PreparedSql(
                        "select ?",
                        Collections.singletonList(
                                SqlParameterBinding.named(
                                        1,
                                        "userId"
                                )
                        )
                );

        try {
            result.positionsOf("userId")
                    .add(2);

            fail(
                    "Expected immutable position list"
            );
        } catch (UnsupportedOperationException expected) {
            // Expected.
        }
    }

    @Test
    public void returnsEmptyPositionsForUnknownName() {
        PreparedSql result =
                new PreparedSql(
                        "select ?",
                        Collections.singletonList(
                                SqlParameterBinding.named(
                                        1,
                                        "userId"
                                )
                        )
                );

        assertTrue(
                result.positionsOf("unknown")
                        .isEmpty()
        );
    }

    @Test
    public void rejectsNonSequentialJdbcPositions() {
        try {
            new PreparedSql(
                    "select ?",
                    Collections.singletonList(
                            SqlParameterBinding.named(
                                    2,
                                    "userId"
                            )
                    )
            );

            fail(
                    "Expected non-sequential JDBC "
                            + "position to be rejected"
            );
        } catch (IllegalArgumentException expected) {
            assertEquals(
                    "Expected JDBC position 1, actual 2",
                    expected.getMessage()
            );
        }
    }

    @Test
    public void allowsEmptyBindingList() {
        PreparedSql result =
                new PreparedSql(
                        "select current_timestamp",
                        Collections
                                .<SqlParameterBinding>emptyList()
                );

        assertTrue(
                result.bindings().isEmpty()
        );

        assertTrue(
                result.namedPositions().isEmpty()
        );
    }

    @Test(expected = NullPointerException.class)
    public void rejectsNullSql() {
        new PreparedSql(
                null,
                Collections
                        .<SqlParameterBinding>emptyList()
        );
    }

    @Test(expected = NullPointerException.class)
    public void rejectsNullBindingList() {
        new PreparedSql(
                "select 1",
                null
        );
    }

    @Test(expected = NullPointerException.class)
    public void rejectsNullBinding() {
        new PreparedSql(
                "select ?",
                Collections
                        .<SqlParameterBinding>singletonList(
                                null
                        )
        );
    }
}