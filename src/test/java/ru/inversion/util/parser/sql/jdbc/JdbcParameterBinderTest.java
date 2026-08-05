package ru.inversion.util.parser.sql.jdbc;

import org.junit.Test;
import ru.inversion.util.parser.sql.transform.PreparedSql;
import ru.inversion.util.parser.sql.transform.SqlParameterBinding;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.math.BigInteger;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class JdbcParameterBinderTest {

    @Test
    public void bindsAllParameterKindsInJdbcOrder()
            throws SQLException {

        PreparedSql preparedSql =
                new PreparedSql(
                        "select * from customer "
                                + "where department_id = ? "
                                + "and manager_id = ? "
                                + "and owner_id = ? "
                                + "and category_id = ? "
                                + "and status = ? "
                                + "and priority = ?",
                        Arrays.asList(
                                SqlParameterBinding
                                        .jdbcPositional(
                                                1,
                                                1
                                        ),
                                SqlParameterBinding.named(
                                        2,
                                        "userId"
                                ),
                                SqlParameterBinding.named(
                                        3,
                                        "userId"
                                ),
                                SqlParameterBinding
                                        .numberedPositional(
                                                4,
                                                2
                                        ),
                                SqlParameterBinding
                                        .generatedLiteral(
                                                5,
                                                "ACTIVE"
                                        ),
                                SqlParameterBinding
                                        .generatedLiteral(
                                                6,
                                                new BigInteger("10")
                                        )
                        )
                );

        RecordingStatement recording =
                new RecordingStatement();

        Map<String, Object> namedValues =
                new HashMap<String, Object>();

        namedValues.put(
                "userId",
                200L
        );

        JdbcParameterBinder.bind(
                recording.statement(),
                preparedSql,
                Arrays.<Object>asList(
                        100L,
                        300L
                ),
                namedValues
        );

        assertEquals(
                Arrays.asList(
                        new SetObjectCall(1, 100L),
                        new SetObjectCall(2, 200L),
                        new SetObjectCall(3, 200L),
                        new SetObjectCall(4, 300L),
                        new SetObjectCall(5, "ACTIVE"),
                        new SetObjectCall(
                                6,
                                new BigInteger("10")
                        )
                ),
                recording.calls()
        );
    }

    @Test
    public void allowsNullPositionalValue()
            throws SQLException {

        PreparedSql preparedSql =
                new PreparedSql(
                        "select ?",
                        Collections.singletonList(
                                SqlParameterBinding
                                        .jdbcPositional(
                                                1,
                                                1
                                        )
                        )
                );

        RecordingStatement recording =
                new RecordingStatement();

        JdbcParameterBinder.bind(
                recording.statement(),
                preparedSql,
                Collections.singletonList(
                        (Object) null
                ),
                Collections.<String, Object>emptyMap()
        );

        assertEquals(
                Collections.singletonList(
                        new SetObjectCall(1, null)
                ),
                recording.calls()
        );
    }

    @Test
    public void allowsNullNamedValue()
            throws SQLException {

        PreparedSql preparedSql =
                new PreparedSql(
                        "select ?",
                        Collections.singletonList(
                                SqlParameterBinding.named(
                                        1,
                                        "optionalValue"
                                )
                        )
                );

        Map<String, Object> namedValues =
                new HashMap<String, Object>();

        namedValues.put(
                "optionalValue",
                null
        );

        RecordingStatement recording =
                new RecordingStatement();

        JdbcParameterBinder.bind(
                recording.statement(),
                preparedSql,
                Collections.emptyList(),
                namedValues
        );

        assertEquals(
                Collections.singletonList(
                        new SetObjectCall(1, null)
                ),
                recording.calls()
        );
    }

    @Test
    public void bindsGeneratedValuesWithoutClientValues()
            throws SQLException {

        PreparedSql preparedSql =
                new PreparedSql(
                        "select ?, ?",
                        Arrays.asList(
                                SqlParameterBinding
                                        .generatedLiteral(
                                                1,
                                                "text"
                                        ),
                                SqlParameterBinding
                                        .generatedLiteral(
                                                2,
                                                new BigInteger("10")
                                        )
                        )
                );

        RecordingStatement recording =
                new RecordingStatement();

        JdbcParameterBinder.bind(
                recording.statement(),
                preparedSql,
                Collections.emptyList(),
                Collections.<String, Object>emptyMap()
        );

        assertEquals(
                Arrays.asList(
                        new SetObjectCall(1, "text"),
                        new SetObjectCall(
                                2,
                                new BigInteger("10")
                        )
                ),
                recording.calls()
        );
    }

    @Test
    public void validatesAllPositionalValuesBeforeWriting()
            throws SQLException {

        PreparedSql preparedSql =
                new PreparedSql(
                        "select ?, ?",
                        Arrays.asList(
                                SqlParameterBinding
                                        .generatedLiteral(
                                                1,
                                                "generated"
                                        ),
                                SqlParameterBinding
                                        .numberedPositional(
                                                2,
                                                2
                                        )
                        )
                );

        RecordingStatement recording =
                new RecordingStatement();

        try {
            JdbcParameterBinder.bind(
                    recording.statement(),
                    preparedSql,
                    Collections.singletonList(
                            "first"
                    ),
                    Collections.<String, Object>emptyMap()
            );

            fail(
                    "Expected missing positional parameter"
            );
        } catch (IllegalArgumentException expected) {
            assertEquals(
                    "Missing positional parameter 2 "
                            + "required by JDBC position 2",
                    expected.getMessage()
            );
        }

        assertTrue(
                recording.calls().isEmpty()
        );
    }

    @Test
    public void validatesAllNamedValuesBeforeWriting()
            throws SQLException {

        PreparedSql preparedSql =
                new PreparedSql(
                        "select ?, ?",
                        Arrays.asList(
                                SqlParameterBinding
                                        .generatedLiteral(
                                                1,
                                                "generated"
                                        ),
                                SqlParameterBinding.named(
                                        2,
                                        "missing"
                                )
                        )
                );

        RecordingStatement recording =
                new RecordingStatement();

        try {
            JdbcParameterBinder.bind(
                    recording.statement(),
                    preparedSql,
                    Collections.emptyList(),
                    Collections.<String, Object>emptyMap()
            );

            fail(
                    "Expected missing named parameter"
            );
        } catch (IllegalArgumentException expected) {
            assertEquals(
                    "Missing named parameter 'missing' "
                            + "required by JDBC position 2",
                    expected.getMessage()
            );
        }

        assertTrue(
                recording.calls().isEmpty()
        );
    }

    @Test
    public void allowsUnusedClientValues()
            throws SQLException {

        PreparedSql preparedSql =
                new PreparedSql(
                        "select ?",
                        Collections.singletonList(
                                SqlParameterBinding
                                        .jdbcPositional(
                                                1,
                                                1
                                        )
                        )
                );

        Map<String, Object> namedValues =
                new HashMap<String, Object>();

        namedValues.put(
                "unused",
                "ignored"
        );

        RecordingStatement recording =
                new RecordingStatement();

        JdbcParameterBinder.bind(
                recording.statement(),
                preparedSql,
                Arrays.<Object>asList(
                        "used",
                        "unused"
                ),
                namedValues
        );

        assertEquals(
                Collections.singletonList(
                        new SetObjectCall(
                                1,
                                "used"
                        )
                ),
                recording.calls()
        );
    }

    @Test
    public void doesNothingWhenBindingsAreEmpty()
            throws SQLException {

        PreparedSql preparedSql =
                new PreparedSql(
                        "select current_timestamp",
                        Collections
                                .<SqlParameterBinding>emptyList()
                );

        RecordingStatement recording =
                new RecordingStatement();

        JdbcParameterBinder.bind(
                recording.statement(),
                preparedSql,
                Collections.emptyList(),
                Collections.<String, Object>emptyMap()
        );

        assertTrue(
                recording.calls().isEmpty()
        );
    }

    @Test(expected = NullPointerException.class)
    public void rejectsNullStatement()
            throws SQLException {

        JdbcParameterBinder.bind(
                null,
                emptyPreparedSql(),
                Collections.emptyList(),
                Collections.<String, Object>emptyMap()
        );
    }

    @Test(expected = NullPointerException.class)
    public void rejectsNullPreparedSql()
            throws SQLException {

        JdbcParameterBinder.bind(
                new RecordingStatement().statement(),
                null,
                Collections.emptyList(),
                Collections.<String, Object>emptyMap()
        );
    }

    @Test(expected = NullPointerException.class)
    public void rejectsNullPositionalValues()
            throws SQLException {

        JdbcParameterBinder.bind(
                new RecordingStatement().statement(),
                emptyPreparedSql(),
                null,
                Collections.<String, Object>emptyMap()
        );
    }

    @Test(expected = NullPointerException.class)
    public void rejectsNullNamedValues()
            throws SQLException {

        JdbcParameterBinder.bind(
                new RecordingStatement().statement(),
                emptyPreparedSql(),
                Collections.emptyList(),
                null
        );
    }

    private static PreparedSql emptyPreparedSql() {
        return new PreparedSql(
                "select current_timestamp",
                Collections
                        .<SqlParameterBinding>emptyList()
        );
    }

    private static final class RecordingStatement
            implements InvocationHandler {

        private final List<SetObjectCall> calls =
                new ArrayList<SetObjectCall>();

        private final PreparedStatement statement =
                (PreparedStatement) Proxy.newProxyInstance(
                        PreparedStatement.class
                                .getClassLoader(),
                        new Class<?>[]{
                                PreparedStatement.class
                        },
                        this
                );

        public PreparedStatement statement() {
            return statement;
        }

        public List<SetObjectCall> calls() {
            return calls;
        }

        @Override
        public Object invoke(
                Object proxy,
                Method method,
                Object[] arguments
        ) throws Throwable {
            if ("setObject".equals(method.getName())
                    && arguments != null
                    && arguments.length == 2) {

                calls.add(
                        new SetObjectCall(
                                (Integer) arguments[0],
                                arguments[1]
                        )
                );

                return null;
            }

            if ("toString".equals(method.getName())) {
                return "RecordingPreparedStatement";
            }

            if ("hashCode".equals(method.getName())) {
                return System.identityHashCode(proxy);
            }

            if ("equals".equals(method.getName())) {
                return proxy == arguments[0];
            }

            throw new UnsupportedOperationException(
                    "Unexpected PreparedStatement method: "
                            + method.getName()
            );
        }
    }

    private static final class SetObjectCall {

        private final int position;
        private final Object value;

        private SetObjectCall(
                int position,
                Object value
        ) {
            this.position = position;
            this.value = value;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }

            if (!(other instanceof SetObjectCall)) {
                return false;
            }

            SetObjectCall that =
                    (SetObjectCall) other;

            if (position != that.position) {
                return false;
            }

            if (value == null) {
                return that.value == null;
            }

            return value.equals(that.value);
        }

        @Override
        public int hashCode() {
            int result = position;
            result = 31 * result
                    + (value != null
                    ? value.hashCode()
                    : 0);
            return result;
        }

        @Override
        public String toString() {
            return "setObject("
                    + position
                    + ", "
                    + value
                    + ")";
        }
    }
}
