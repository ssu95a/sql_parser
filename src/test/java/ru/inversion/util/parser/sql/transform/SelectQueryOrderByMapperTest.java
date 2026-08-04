package ru.inversion.util.parser.sql.transform;

import org.junit.Test;
import ru.inversion.util.parser.text.TextRange;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SelectQueryOrderByMapperTest {

    private final SelectQueryMapper mapper =
            new SelectQueryMapper();

    @Test
    public void mapsExistingOrderByItems() {
        assertOrderByItems(
                "select * from t order by c1",
                "c1"
        );
    }

    @Test
    public void mapsMultipleOrderByItems() {
        assertOrderByItems(
                "select * from t "
                        + "order by c1 desc, lower(c2), c3 nulls last",
                "c1 desc, lower(c2), c3 nulls last"
        );
    }

    @Test
    public void recognizesOrderByCaseInsensitively() {
        assertOrderByItems(
                "SELECT * FROM t ORDER BY c1 DESC",
                "c1 DESC"
        );
    }

    @Test
    public void allowsTriviaBetweenOrderAndBy() {
        assertOrderByItems(
                "select * from t "
                        + "order /* sorting */ by c1 desc",
                "c1 desc"
        );
    }

    @Test
    public void excludesOrderByKeywordsFromRange() {
        String sql =
                "select * from t order by c1 desc";

        SelectQueryMap map =
                mapper.map(sql);

        assertEquals(
                sql.indexOf("c1 desc"),
                map.orderByItemsRange().start()
        );
    }

    @Test
    public void excludesTrailingTriviaBeforeLimit() {
        String sql =
                "select * from t\n"
                        + "order by c1 desc   \n"
                        + "limit 10";

        SelectQueryMap map =
                mapper.map(sql);

        TextRange range =
                map.orderByItemsRange();

        assertEquals(
                "c1 desc",
                sql.substring(
                        range.start(),
                        range.end()
                )
        );
    }

    @Test
    public void mapsOrderByItemsBeforeOffset() {
        assertOrderByItems(
                "select * from t "
                        + "order by c1 "
                        + "offset 10",
                "c1"
        );
    }

    @Test
    public void mapsOrderByItemsBeforeFetch() {
        assertOrderByItems(
                "select * from t "
                        + "order by c1 "
                        + "fetch first 10 rows only",
                "c1"
        );
    }

    @Test
    public void mapsOrderByItemsBeforeFor() {
        assertOrderByItems(
                "select * from t "
                        + "order by c1 "
                        + "for update",
                "c1"
        );
    }

    @Test
    public void mapsOrderByItemsBeforeSemicolon() {
        assertOrderByItems(
                "select * from t order by c1;",
                "c1"
        );
    }

    @Test
    public void ignoresOrderByInsideSubquery() {
        String sql =
                "select "
                        + "(select value "
                        + " from nested_table "
                        + " order by nested_value "
                        + " limit 1) as value "
                        + "from outer_table "
                        + "order by outer_value desc "
                        + "limit 10";

        SelectQueryMap map =
                mapper.map(sql);

        assertTrue(
                map.hasOrderBy()
        );

        TextRange range =
                map.orderByItemsRange();

        assertEquals(
                "outer_value desc",
                sql.substring(
                        range.start(),
                        range.end()
                )
        );
    }

    @Test
    public void nestedOrderByDoesNotCountAsOuterOrderBy() {
        String sql =
                "select "
                        + "(select value "
                        + " from nested_table "
                        + " order by nested_value "
                        + " limit 1) as value "
                        + "from outer_table "
                        + "limit 10";

        SelectQueryMap map =
                mapper.map(sql);

        assertFalse(
                map.hasOrderBy()
        );

        assertEquals(
                sql.lastIndexOf("limit 10"),
                map.orderByInsertion().offset()
        );
    }

    @Test
    public void ignoresOrderByInsideFunctionParentheses() {
        String sql =
                "select array_agg(value order by value) "
                        + "from t";

        SelectQueryMap map =
                mapper.map(sql);

        assertFalse(
                map.hasOrderBy()
        );

        assertEquals(
                sql.length(),
                map.orderByInsertion().offset()
        );
    }

    @Test
    public void ignoresOrderByInsideStringAndComment() {
        String sql =
                "select 'order by fake' as value "
                        + "from t "
                        + "/* order by fake */ "
                        + "limit 10";

        SelectQueryMap map =
                mapper.map(sql);

        assertFalse(
                map.hasOrderBy()
        );

        assertEquals(
                sql.indexOf("limit 10"),
                map.orderByInsertion().offset()
        );
    }

    @Test
    public void doesNotTreatOrderWordWithoutByAsClause() {
        String sql =
                "select order_value "
                        + "from t "
                        + "where order_value = 1";

        SelectQueryMap map =
                mapper.map(sql);

        assertFalse(
                map.hasOrderBy()
        );

        assertEquals(
                sql.length(),
                map.orderByInsertion().offset()
        );
    }

    @Test
    public void mapsEmptyOrderByRangeBeforeSemicolon() {
        String sql =
                "select * from t order by;";

        SelectQueryMap map =
                mapper.map(sql);

        assertTrue(
                map.hasOrderBy()
        );

        TextRange range =
                map.orderByItemsRange();

        assertTrue(
                range.isEmpty()
        );

        assertEquals(
                sql.indexOf(';'),
                range.start()
        );
    }

    @Test
    public void mapsOrderByInsertionBeforeLimit() {
        assertOrderByInsertion(
                "select * from t limit 10",
                "limit 10"
        );
    }

    @Test
    public void mapsOrderByInsertionBeforeOffset() {
        assertOrderByInsertion(
                "select * from t offset 10",
                "offset 10"
        );
    }

    @Test
    public void mapsOrderByInsertionBeforeFetch() {
        assertOrderByInsertion(
                "select * from t fetch first 10 rows only",
                "fetch first"
        );
    }

    @Test
    public void mapsOrderByInsertionBeforeFor() {
        assertOrderByInsertion(
                "select * from t for update",
                "for update"
        );
    }

    @Test
    public void mapsOrderByInsertionBeforeSemicolon() {
        assertOrderByInsertion(
                "select * from t;",
                ";"
        );
    }

    @Test
    public void mapsOrderByInsertionAtEndOfInput() {
        String sql =
                "select * from t";

        SelectQueryMap map =
                mapper.map(sql);

        assertFalse(
                map.hasOrderBy()
        );

        assertEquals(
                sql.length(),
                map.orderByInsertion().offset()
        );
    }

    @Test
    public void mapsWhereAndOrderByIndependently() {
        String sql =
                "select * from t "
                        + "where active = :active "
                        + "order by created_at desc "
                        + "limit 10";

        SelectQueryMap map =
                mapper.map(sql);

        assertTrue(
                map.hasWhere()
        );

        assertTrue(
                map.hasOrderBy()
        );

        assertEquals(
                "active = :active",
                text(
                        sql,
                        map.wherePredicateRange()
                )
        );

        assertEquals(
                "created_at desc",
                text(
                        sql,
                        map.orderByItemsRange()
                )
        );

        assertEquals(
                1,
                map.parameters().size()
        );
    }

    private void assertOrderByItems(
            String sql,
            String expectedItems
    ) {
        SelectQueryMap map =
                mapper.map(sql);

        assertTrue(
                map.hasOrderBy()
        );

        assertEquals(
                expectedItems,
                text(
                        sql,
                        map.orderByItemsRange()
                )
        );
    }

    private void assertOrderByInsertion(
            String sql,
            String boundary
    ) {
        SelectQueryMap map =
                mapper.map(sql);

        assertFalse(
                map.hasOrderBy()
        );

        assertEquals(
                sql.indexOf(boundary),
                map.orderByInsertion().offset()
        );
    }

    private static String text(
            String sql,
            TextRange range
    ) {
        return sql.substring(
                range.start(),
                range.end()
        );
    }
}