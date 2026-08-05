package ru.inversion.util.parser.sql.jdbc;

import org.junit.Test;
import ru.inversion.util.parser.sql.transform.PreparedSql;
import ru.inversion.util.parser.sql.transform.SqlParameterCompiler;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.math.BigInteger;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static ru.inversion.util.parser.sql.dialect
        .TestSqlSyntaxDialects.NONE;

public class SqlParameterEndToEndTest {

    @Test
    public void compilesAndBindsMixedParameters()
            throws SQLException {

        String sql =
                "select *\n"
                        + "from customer\n"
                        + "where department_id = ?\n"
                        + "  and manager_id = :userId\n"
                        + "  and owner_id = :userId\n"
                        + "  and category_id = $2\n"
                        + "  and status = 'ACTIVE'\n"
                        + "  and priority = 10";

        SqlParameterCompiler compiler =
                new SqlParameterCompiler(NONE);

        PreparedSql preparedSql =
                compiler.compile(sql);

        assertEquals(
                "select *\n"
                        + "from customer\n"
                        + "where department_id = ?\n"
                        + "  and manager_id = ?\n"
                        + "  and owner_id = ?\n"
                        + "  and category_id = ?\n"
                        + "  and status = ?\n"
                        + "  and priority = ?",
                preparedSql.sql()
        );

        assertEquals(
                6,
                preparedSql.bindings().size()
        );

        assertEquals(
                Arrays.asList(2, 3),
                preparedSql.positionsOf("userId")
        );

        Map<String, Object> namedValues =
                new HashMap<String, Object>();

        namedValues.put(
                "userId",
                200L
        );

        RecordingStatement recording =
                new RecordingStatement();

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

        private PreparedStatement statement() {
            return statement;
        }

        private List<SetObjectCall> calls() {
            return calls;
        }

        @Override
        public Object invoke(
                Object proxy,
                Method method,
                Object[] arguments
        ) {
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
