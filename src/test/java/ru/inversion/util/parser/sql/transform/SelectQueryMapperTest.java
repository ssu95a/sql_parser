package ru.inversion.util.parser.sql.transform;

import org.junit.Test;
import ru.inversion.util.parser.text.TextChangeApplier;
import ru.inversion.util.parser.text.TextRange;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class SelectQueryMapperTest {

    private final SelectQueryMapper mapper =
            new SelectQueryMapper();

    @Test
    public void returnsEmptyParameterListWhenParametersAreAbsent() {
        SelectQueryMap map =
                mapper.map(
                        "select c1 from t"
                );

        assertTrue(
                map.parameters().isEmpty()
        );
    }

    @Test
    public void mapsJdbcParameter() {
        String sql =
                "select * from t where id = ?";

        SelectQueryMap map =
                mapper.map(sql);

        List<SqlParameterOccurrence> parameters =
                map.parameters();

        assertEquals(
                1,
                parameters.size()
        );

        SqlParameterOccurrence parameter =
                parameters.get(0);

        int offset =
                sql.indexOf('?');

        assertEquals(
                SqlParameterKind.POSITIONAL,
                parameter.kind()
        );

        assertEquals(
                new TextRange(
                        offset,
                        offset + 1
                ),
                parameter.range()
        );

        assertNull(
                parameter.name()
        );

        assertTrue(
                parameter.isPositional()
        );

        assertFalse(
                parameter.isNamed()
        );
    }

    @Test
    public void mapsNamedParameter() {
        String sql =
                "select * from t where id = :customerId";

        SelectQueryMap map =
                mapper.map(sql);

        List<SqlParameterOccurrence> parameters =
                map.parameters();

        assertEquals(
                1,
                parameters.size()
        );

        SqlParameterOccurrence parameter =
                parameters.get(0);

        int start =
                sql.indexOf(":customerId");

        assertEquals(
                SqlParameterKind.NAMED,
                parameter.kind()
        );

        assertEquals(
                new TextRange(
                        start,
                        start + ":customerId".length()
                ),
                parameter.range()
        );

        assertEquals(
                "customerId",
                parameter.name()
        );

        assertFalse(
                parameter.isPositional()
        );

        assertTrue(
                parameter.isNamed()
        );
    }

    @Test
    public void preservesMixedParameterOrder() {
        String sql =
                "select * from t "
                        + "where a = ? "
                        + "and b = :name "
                        + "and c = ?";

        SelectQueryMap map =
                mapper.map(sql);

        List<SqlParameterOccurrence> parameters =
                map.parameters();

        assertEquals(
                3,
                parameters.size()
        );

        assertEquals(
                SqlParameterKind.POSITIONAL,
                parameters.get(0).kind()
        );

        assertEquals(
                SqlParameterKind.NAMED,
                parameters.get(1).kind()
        );

        assertEquals(
                "name",
                parameters.get(1).name()
        );

        assertEquals(
                SqlParameterKind.POSITIONAL,
                parameters.get(2).kind()
        );

        assertTrue(
                parameters.get(0).range().start()
                        < parameters.get(1).range().start()
        );

        assertTrue(
                parameters.get(1).range().start()
                        < parameters.get(2).range().start()
        );
    }

    @Test
    public void preservesRepeatedNamedParameterOccurrences() {
        String sql =
                "select * from t "
                        + "where parent_id = :id "
                        + "or child_id = :id";

        SelectQueryMap map =
                mapper.map(sql);

        List<SqlParameterOccurrence> parameters =
                map.parameters();

        assertEquals(
                2,
                parameters.size()
        );

        SqlParameterOccurrence first =
                parameters.get(0);

        SqlParameterOccurrence second =
                parameters.get(1);

        assertEquals(
                "id",
                first.name()
        );

        assertEquals(
                "id",
                second.name()
        );

        assertNotSame(
                first,
                second
        );

        int firstStart =
                sql.indexOf(":id");

        int secondStart =
                sql.indexOf(
                        ":id",
                        firstStart + 1
                );

        assertEquals(
                new TextRange(
                        firstStart,
                        firstStart + 3
                ),
                first.range()
        );

        assertEquals(
                new TextRange(
                        secondStart,
                        secondStart + 3
                ),
                second.range()
        );
    }

    @Test
    public void mapsParametersInsideNestedQuery() {
        String sql =
                "select * "
                        + "from t "
                        + "where id in ("
                        + "select parent_id "
                        + "from nested "
                        + "where value = :nestedValue"
                        + ") "
                        + "and status = ?";

        SelectQueryMap map =
                mapper.map(sql);

        List<SqlParameterOccurrence> parameters =
                map.parameters();

        assertEquals(
                2,
                parameters.size()
        );

        assertEquals(
                SqlParameterKind.NAMED,
                parameters.get(0).kind()
        );

        assertEquals(
                "nestedValue",
                parameters.get(0).name()
        );

        assertEquals(
                SqlParameterKind.POSITIONAL,
                parameters.get(1).kind()
        );
    }

    @Test
    public void ignoresParameterTextInsideStringLiteral() {
        String sql =
                "select ':notParameter', ? from t";

        SelectQueryMap map =
                mapper.map(sql);

        List<SqlParameterOccurrence> parameters =
                map.parameters();

        assertEquals(
                1,
                parameters.size()
        );

        assertTrue(
                parameters.get(0).isPositional()
        );
    }

    @Test
    public void ignoresParameterTextInsideQuotedIdentifier() {
        String sql =
                "select \":notParameter\", :realParameter "
                        + "from t";

        SelectQueryMap map =
                mapper.map(sql);

        List<SqlParameterOccurrence> parameters =
                map.parameters();

        assertEquals(
                1,
                parameters.size()
        );

        assertEquals(
                "realParameter",
                parameters.get(0).name()
        );
    }

    @Test
    public void ignoresParameterTextInsideComments() {
        String sql =
                "select ? "
                        + "from t "
                        + "-- :lineComment\n"
                        + "where id = :id "
                        + "/* ? :blockComment */";

        SelectQueryMap map =
                mapper.map(sql);

        List<SqlParameterOccurrence> parameters =
                map.parameters();

        assertEquals(
                2,
                parameters.size()
        );

        assertTrue(
                parameters.get(0).isPositional()
        );

        assertEquals(
                "id",
                parameters.get(1).name()
        );
    }

    @Test
    public void ignoresPostgresPositionalParameter() {
        String sql =
                "select $1, ?, :name from t";

        SelectQueryMap map =
                mapper.map(sql);

        List<SqlParameterOccurrence> parameters =
                map.parameters();

        assertEquals(
                2,
                parameters.size()
        );

        assertTrue(
                parameters.get(0).isPositional()
        );

        assertEquals(
                "name",
                parameters.get(1).name()
        );
    }

    @Test(expected = NullPointerException.class)
    public void rejectsNullSql() {
        mapper.map(null);
    }

    @Test
    public void mapsSelectItemInsertionForSimpleSelect() {
        String sql =
                "select c1 from t";

        SelectQueryMap map =
                mapper.map(sql);

        assertEquals(
                sql.indexOf("c1"),
                map.selectItemInsertion().offset()
        );
    }

    @Test
    public void mapsSelectItemInsertionAfterDistinct() {
        String sql =
                "select distinct c1 from t";

        SelectQueryMap map =
                mapper.map(sql);

        assertEquals(
                sql.indexOf("c1"),
                map.selectItemInsertion().offset()
        );
    }

    @Test
    public void mapsSelectItemInsertionAfterAll() {
        String sql =
                "select all c1 from t";

        SelectQueryMap map =
                mapper.map(sql);

        assertEquals(
                sql.indexOf("c1"),
                map.selectItemInsertion().offset()
        );
    }

    @Test
    public void mapsSelectItemInsertionCaseInsensitively() {
        String sql =
                "SELECT DISTINCT c1 FROM t";

        SelectQueryMap map =
                mapper.map(sql);

        assertEquals(
                sql.indexOf("c1"),
                map.selectItemInsertion().offset()
        );
    }

    @Test
    public void skipsWhitespaceAndCommentsBeforeFirstSelectItem() {
        String sql =
                "select distinct\n"
                        + "    /* returned value */\n"
                        + "    c1\n"
                        + "from t";

        SelectQueryMap map =
                mapper.map(sql);

        assertEquals(
                sql.indexOf("c1"),
                map.selectItemInsertion().offset()
        );
    }

    @Test
    public void mapsInsertionBeforeParenthesizedSelectItem() {
        String sql =
                "select "
                        + "(select max(x) from nested_table) "
                        + "as value "
                        + "from t";

        SelectQueryMap map =
                mapper.map(sql);

        assertEquals(
                sql.indexOf('('),
                map.selectItemInsertion().offset()
        );
    }

    @Test
    public void ignoresNestedSelectBeforeOuterSelect() {
        String sql =
                "with nested as ("
                        + "select ? as nested_value"
                        + ") "
                        + "select distinct c1 "
                        + "from nested";

        SelectQueryMap map =
                mapper.map(sql);

        assertEquals(
                sql.indexOf("c1"),
                map.selectItemInsertion().offset()
        );

        assertEquals(
                1,
                map.parameters().size()
        );

        assertTrue(
                map.parameters()
                        .get(0)
                        .isPositional()
        );
    }

    @Test
    public void ignoresSelectTextInsideStringLiteral() {
        String sql =
                "select 'select distinct fake' as value "
                        + "from t";

        SelectQueryMap map =
                mapper.map(sql);

        assertEquals(
                sql.indexOf("'select distinct fake'"),
                map.selectItemInsertion().offset()
        );
    }

    @Test
    public void ignoresSelectTextInsideComment() {
        String sql =
                "/* select fake from fake_table */\n"
                        + "select c1 from t";

        SelectQueryMap map =
                mapper.map(sql);

        assertEquals(
                sql.indexOf("c1"),
                map.selectItemInsertion().offset()
        );
    }

    @Test
    public void allowsIncompleteSelectAtEndOfInput() {
        String sql =
                "select distinct";

        SelectQueryMap map =
                mapper.map(sql);

        assertEquals(
                sql.length(),
                map.selectItemInsertion().offset()
        );
    }

    @Test
    public void placesAnchorBeforeSemicolonForEmptySelectList() {
        String sql =
                "select;";

        SelectQueryMap map =
                mapper.map(sql);

        assertEquals(
                sql.indexOf(';'),
                map.selectItemInsertion().offset()
        );
    }

    @Test
    public void rejectsSqlWithoutTopLevelSelect() {
        try {
            mapper.map(
                    "update t set value = :value"
            );

            fail(
                    "Expected missing top-level SELECT "
                            + "to be rejected"
            );
        } catch (IllegalArgumentException expected) {
            assertEquals(
                    "Top-level SELECT not found",
                    expected.getMessage()
            );
        }
    }

    @Test
    public void rejectsSelectWrappedInParenthesesForNow() {
        try {
            mapper.map(
                    "(select c1 from t)"
            );

            fail(
                    "Expected wrapped SELECT "
                            + "to be rejected"
            );
        } catch (IllegalArgumentException expected) {
            assertEquals(
                    "Top-level SELECT not found",
                    expected.getMessage()
            );
        }
    }

    @Test
    public void anchorCanPrependSelectItem() {
        String sql =
                "select distinct c1 from t";

        SelectQueryMap map =
                mapper.map(sql);

        String result =
                TextChangeApplier.apply(
                        sql,
                        Collections.singletonList(
                                map.selectItemInsertion()
                                        .insert("mark, ")
                        )
                );

        assertEquals(
                "select distinct mark, c1 from t",
                result
        );
    }

    @Test
    public void mapsExistingWherePredicate() {
        String sql =
                "select * from t "
                        + "where a = 1 or b = 2";

        SelectQueryMap map =
                mapper.map(sql);

        assertTrue(
                map.hasWhere()
        );

        TextRange range =
                map.wherePredicateRange();

        assertEquals(
                "a = 1 or b = 2",
                sql.substring(
                        range.start(),
                        range.end()
                )
        );
    }

    @Test
    public void excludesWhereKeywordFromPredicateRange() {
        String sql =
                "select * from t where value = 10";

        SelectQueryMap map =
                mapper.map(sql);

        TextRange range =
                map.wherePredicateRange();

        assertEquals(
                sql.indexOf("value"),
                range.start()
        );
    }

    @Test
    public void excludesTrailingWhitespaceFromWherePredicate() {
        String sql =
                "select * from t\n"
                        + "where value = 10   \n"
                        + "order by value";

        SelectQueryMap map =
                mapper.map(sql);

        TextRange range =
                map.wherePredicateRange();

        assertEquals(
                "value = 10",
                sql.substring(
                        range.start(),
                        range.end()
                )
        );
    }

    @Test
    public void mapsWherePredicateBeforeGroupBy() {
        assertWherePredicate(
                "select c1, count(*) "
                        + "from t "
                        + "where active = 1 "
                        + "group by c1",
                "active = 1"
        );
    }

    @Test
    public void mapsWherePredicateBeforeHaving() {
        assertWherePredicate(
                "select count(*) "
                        + "from t "
                        + "where active = 1 "
                        + "having count(*) > 1",
                "active = 1"
        );
    }

    @Test
    public void mapsWherePredicateBeforeOrderBy() {
        assertWherePredicate(
                "select * "
                        + "from t "
                        + "where active = 1 "
                        + "order by id",
                "active = 1"
        );
    }

    @Test
    public void mapsWherePredicateBeforeLimit() {
        assertWherePredicate(
                "select * "
                        + "from t "
                        + "where active = 1 "
                        + "limit 10",
                "active = 1"
        );
    }

    @Test
    public void mapsWherePredicateBeforeSemicolon() {
        assertWherePredicate(
                "select * from t where active = 1;",
                "active = 1"
        );
    }

    @Test
    public void ignoresWhereInsideSubquery() {
        String sql =
                "select "
                        + "(select max(value) "
                        + " from nested "
                        + " where nested.id = :id) as value "
                        + "from outer_table "
                        + "where outer_table.active = ? "
                        + "order by value";

        SelectQueryMap map =
                mapper.map(sql);

        assertTrue(
                map.hasWhere()
        );

        TextRange range =
                map.wherePredicateRange();

        assertEquals(
                "outer_table.active = ?",
                sql.substring(
                        range.start(),
                        range.end()
                )
        );

        assertEquals(
                2,
                map.parameters().size()
        );
    }

    @Test
    public void nestedWhereDoesNotCountAsOuterWhere() {
        String sql =
                "select "
                        + "(select value "
                        + " from nested "
                        + " where nested.id = :id) "
                        + "from outer_table "
                        + "order by value";

        SelectQueryMap map =
                mapper.map(sql);

        assertFalse(
                map.hasWhere()
        );

        assertEquals(
                sql.indexOf("order by"),
                map.whereInsertion().offset()
        );
    }

    @Test
    public void mapsWhereInsertionBeforeGroupBy() {
        assertWhereInsertion(
                "select c1, count(*) "
                        + "from t "
                        + "group by c1",
                "group by"
        );
    }

    @Test
    public void mapsWhereInsertionBeforeHaving() {
        assertWhereInsertion(
                "select count(*) "
                        + "from t "
                        + "having count(*) > 1",
                "having"
        );
    }

    @Test
    public void mapsWhereInsertionBeforeOrderBy() {
        assertWhereInsertion(
                "select * from t order by id",
                "order by"
        );
    }

    @Test
    public void mapsWhereInsertionBeforeLimit() {
        assertWhereInsertion(
                "select * from t limit 10",
                "limit"
        );
    }

    @Test
    public void mapsWhereInsertionBeforeOffset() {
        assertWhereInsertion(
                "select * from t offset 10",
                "offset"
        );
    }

    @Test
    public void mapsWhereInsertionBeforeFetch() {
        assertWhereInsertion(
                "select * from t fetch first 10 rows only",
                "fetch"
        );
    }

    @Test
    public void mapsWhereInsertionBeforeFor() {
        assertWhereInsertion(
                "select * from t for update",
                "for"
        );
    }

    @Test
    public void mapsWhereInsertionBeforeSemicolon() {
        assertWhereInsertion(
                "select * from t;",
                ";"
        );
    }

    @Test
    public void mapsWhereInsertionAtEndOfInput() {
        String sql =
                "select * from t";

        SelectQueryMap map =
                mapper.map(sql);

        assertFalse(
                map.hasWhere()
        );

        assertEquals(
                sql.length(),
                map.whereInsertion().offset()
        );
    }

    @Test
    public void ignoresWhereTextInsideStringAndComment() {
        String sql =
                "select 'where fake' as value "
                        + "from t "
                        + "/* where fake = 1 */ "
                        + "order by value";

        SelectQueryMap map =
                mapper.map(sql);

        assertFalse(
                map.hasWhere()
        );

        assertEquals(
                sql.indexOf("order by"),
                map.whereInsertion().offset()
        );
    }

    @Test
    public void recognizesWhereCaseInsensitively() {
        String sql =
                "SELECT * FROM t WHERE value = 10 ORDER BY value";

        SelectQueryMap map =
                mapper.map(sql);

        assertTrue(
                map.hasWhere()
        );

        TextRange range =
                map.wherePredicateRange();

        assertEquals(
                "value = 10",
                sql.substring(
                        range.start(),
                        range.end()
                )
        );
    }

    private void assertWherePredicate(
            String sql,
            String expectedPredicate
    ) {
        SelectQueryMap map =
                mapper.map(sql);

        assertTrue(
                map.hasWhere()
        );

        TextRange range =
                map.wherePredicateRange();

        assertEquals(
                expectedPredicate,
                sql.substring(
                        range.start(),
                        range.end()
                )
        );
    }

    private void assertWhereInsertion(
            String sql,
            String boundary
    ) {
        SelectQueryMap map =
                mapper.map(sql);

        assertFalse(
                map.hasWhere()
        );

        assertEquals(
                sql.indexOf(boundary),
                map.whereInsertion().offset()
        );
    }
}