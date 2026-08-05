package ru.inversion.util.parser.sql.transform;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class PreparedSqlTest {

    @Test
    public void storesSqlAndParameters() {
        PreparedSql result =
                new PreparedSql(
                        "select * from t where id = ?",
                        Arrays.<Object>asList(10)
                );

        assertEquals(
                "select * from t where id = ?",
                result.sql()
        );

        assertEquals(
                Arrays.<Object>asList(10),
                result.parameters()
        );
    }

    @Test
    public void copiesParameterList() {
        List<Object> parameters =
                new ArrayList<Object>();

        parameters.add(10);

        PreparedSql result =
                new PreparedSql(
                        "select ?",
                        parameters
                );

        parameters.add(20);

        assertEquals(
                Arrays.<Object>asList(10),
                result.parameters()
        );
    }

    @Test
    public void exposesImmutableParameterList() {
        PreparedSql result =
                new PreparedSql(
                        "select ?",
                        Arrays.<Object>asList(10)
                );

        try {
            result.parameters().add(20);

            fail(
                    "Expected immutable parameter list"
            );
        } catch (UnsupportedOperationException expected) {
            // Expected.
        }
    }

    @Test(expected = NullPointerException.class)
    public void rejectsNullSql() {
        new PreparedSql(
                null,
                Arrays.<Object>asList(10)
        );
    }

    @Test(expected = NullPointerException.class)
    public void rejectsNullParameterList() {
        new PreparedSql(
                "select ?",
                null
        );
    }

    @Test
    public void allowsNullParameterValue() {
        PreparedSql result =
                new PreparedSql(
                        "select ?",
                        Arrays.<Object>asList(
                                (Object) null
                        )
                );

        assertEquals(
                1,
                result.parameters().size()
        );

        assertEquals(
                null,
                result.parameters().get(0)
        );
    }
}