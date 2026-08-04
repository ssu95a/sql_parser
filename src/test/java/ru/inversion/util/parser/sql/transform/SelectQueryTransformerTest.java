package ru.inversion.util.parser.sql.transform;

import org.junit.Test;
import ru.inversion.util.parser.text.TextChange;
import ru.inversion.util.parser.text.TextChangeApplier;
import java.util.Collections;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class SelectQueryTransformerTest {

    private final SelectQueryMapper mapper = new SelectQueryMapper();

    @Test
    public void strengthensExistingWhere() {
        String sql =
                "select * "
                        + "from t "
                        + "where value = 10";

        String result =
                transform(
                        sql,
                        "active = 1"
                );

        assertEquals(
                "select * "
                        + "from t "
                        + "where (value = 10) "
                        + "and (active = 1)",
                result
        );
    }

    @Test
    public void preservesMeaningOfExistingOrPredicate() {
        String sql =
                "select * "
                        + "from t "
                        + "where a = 1 or b = 2";

        String result =
                transform(
                        sql,
                        "c = 3"
                );

        assertEquals(
                "select * "
                        + "from t "
                        + "where (a = 1 or b = 2) "
                        + "and (c = 3)",
                result
        );
    }

    @Test
    public void preservesExistingPredicateFormatting() {
        String sql =
                "select *\n"
                        + "from t\n"
                        + "where\n"
                        + "    a = 1\n"
                        + "    or b = 2\n"
                        + "order by a";

        String result =
                transform(
                        sql,
                        "active = 1"
                );

        assertEquals(
                "select *\n"
                        + "from t\n"
                        + "where\n"
                        + "    (a = 1\n"
                        + "    or b = 2) and (active = 1)\n"
                        + "order by a",
                result
        );
    }

    @Test
    public void preservesCommentsInsideExistingPredicate() {
        String sql =
                "select * "
                        + "from t "
                        + "where /* first */ a = 1 "
                        + "or /* second */ b = 2 "
                        + "order by a";

        String result =
                transform(
                        sql,
                        "active = 1"
                );

        assertEquals(
                "select * "
                        + "from t "
                        + "where /* first */ (a = 1 "
                        + "or /* second */ b = 2) "
                        + "and (active = 1) "
                        + "order by a",
                result
        );
    }

    @Test
    public void preservesTrailingTriviaBeforeOrderBy() {
        String sql =
                "select * from t "
                        + "where value = 10   \n"
                        + "order by value";

        String result =
                transform(
                        sql,
                        "active = 1"
                );

        assertEquals(
                "select * from t "
                        + "where (value = 10) "
                        + "and (active = 1)   \n"
                        + "order by value",
                result
        );
    }

    @Test
    public void insertsWhereAtEndOfInput() {
        String sql =
                "select * from t";

        String result =
                transform(
                        sql,
                        "active = 1"
                );

        assertEquals(
                "select * from t where active = 1 ",
                result
        );
    }

    @Test
    public void insertsWhereBeforeOrderBy() {
        String sql =
                "select * from t order by id";

        String result =
                transform(
                        sql,
                        "active = 1"
                );

        assertEquals(
                "select * from t  where active = 1 "
                        + "order by id",
                result
        );
    }

    @Test
    public void insertsWhereBeforeGroupBy() {
        String sql =
                "select category, count(*) "
                        + "from t "
                        + "group by category";

        String result =
                transform(
                        sql,
                        "active = 1"
                );

        assertEquals(
                "select category, count(*) "
                        + "from t  where active = 1 "
                        + "group by category",
                result
        );
    }

    @Test
    public void insertsWhereBeforeLimit() {
        String sql =
                "select * from t limit 10";

        String result =
                transform(
                        sql,
                        "active = 1"
                );

        assertEquals(
                "select * from t  where active = 1 "
                        + "limit 10",
                result
        );
    }

    @Test
    public void insertsWhereBeforeSemicolon() {
        String sql =
                "select * from t;";

        String result =
                transform(
                        sql,
                        "active = 1"
                );

        assertEquals(
                "select * from t"
                        + " where active = 1 ;",
                result
        );
    }

    @Test
    public void doesNotModifyNestedWhere() {
        String sql =
                "select "
                        + "(select max(value) "
                        + " from nested "
                        + " where nested.id = :id) as value "
                        + "from outer_table "
                        + "where outer_table.active = ?";

        String result =
                transform(
                        sql,
                        "outer_table.deleted = 0"
                );

        assertEquals(
                "select "
                        + "(select max(value) "
                        + " from nested "
                        + " where nested.id = :id) as value "
                        + "from outer_table "
                        + "where (outer_table.active = ?) "
                        + "and (outer_table.deleted = 0)",
                result
        );
    }

    @Test
    public void returnsTwoChangesForExistingWhere() {
        String sql =
                "select * from t where value = 10";

        SelectQueryMap map =
                mapper.map(sql);

        List<TextChange> changes =
                SelectQueryTransformer.strengthenWhere(
                        map,
                        "active = 1"
                );

        assertEquals(
                2,
                changes.size()
        );

        assertTrue(
                changes.get(0).isInsertion()
        );

        assertTrue(
                changes.get(1).isInsertion()
        );
    }

    @Test
    public void returnsOneChangeForMissingWhere() {
        String sql =
                "select * from t";

        SelectQueryMap map =
                mapper.map(sql);

        List<TextChange> changes =
                SelectQueryTransformer.strengthenWhere(
                        map,
                        "active = 1"
                );

        assertEquals(
                1,
                changes.size()
        );

        assertTrue(
                changes.get(0).isInsertion()
        );
    }

    @Test
    public void doesNotChangeMapState() {
        String sql =
                "select * from t where value = 10";

        SelectQueryMap map =
                mapper.map(sql);

        SelectQueryTransformer.strengthenWhere(
                map,
                "active = 1"
        );

        assertTrue(
                map.hasWhere()
        );

        assertFalse(
                map.wherePredicateRange().isEmpty()
        );
    }

    @Test(expected = NullPointerException.class)
    public void rejectsNullMap() {
        SelectQueryTransformer.strengthenWhere(
                null,
                "active = 1"
        );
    }

    @Test(expected = NullPointerException.class)
    public void rejectsNullPredicate() {
        SelectQueryMap map =
                mapper.map(
                        "select * from t"
                );

        SelectQueryTransformer.strengthenWhere(
                map,
                null
        );
    }

    @Test
    public void rejectsEmptyPredicate() {
        assertInvalidPredicate("");
    }

    @Test
    public void rejectsWhitespacePredicate() {
        assertInvalidPredicate(
                " \t\r\n "
        );
    }

    private String transform(
            String sql,
            String predicate
    ) {
        SelectQueryMap map =
                mapper.map(sql);

        List<TextChange> changes =
                SelectQueryTransformer.strengthenWhere(
                        map,
                        predicate
                );

        return TextChangeApplier.apply(
                sql,
                changes
        );
    }

    private void assertInvalidPredicate(
            String predicate
    ) {
        SelectQueryMap map =
                mapper.map(
                        "select * from t"
                );

        try {
            SelectQueryTransformer.strengthenWhere(
                    map,
                    predicate
            );

            fail(
                    "Expected empty predicate "
                            + "to be rejected"
            );
        } catch (IllegalArgumentException expected) {
            assertEquals(
                    "predicate is empty",
                    expected.getMessage()
            );
        }
    }

    @Test
    public void prependsSelectItemAfterOptimizerHint() {
        String sql =
                "select /*+ INDEX(t IDX_T) */ "
                        + "c1 from t";

        String result =
                prependSelectItem(
                        sql,
                        "marker"
                );

        assertEquals(
                "select /*+ INDEX(t IDX_T) */ "
                        + "marker, c1 from t",
                result
        );
    }

    @Test
    public void prependsSelectItemAfterOptimizerHintAndDistinct() {
        String sql =
                "select /*+ FIRST_ROWS(10) */ "
                        + "distinct c1 from t";

        String result =
                prependSelectItem(
                        sql,
                        "marker"
                );

        assertEquals(
                "select /*+ FIRST_ROWS(10) */ "
                        + "distinct marker, c1 from t",
                result
        );
    }

    @Test
    public void prependsSelectItemAfterOptimizerHintAndAll() {
        String sql =
                "select /*+ FULL(t) */ "
                        + "all c1 from t";

        String result =
                prependSelectItem(
                        sql,
                        "marker"
                );

        assertEquals(
                "select /*+ FULL(t) */ "
                        + "all marker, c1 from t",
                result
        );
    }

    @Test
    public void preservesMultilineOptimizerHintWhenPrependingSelectItem() {
        String sql =
                "select\n"
                        + "    /*+\n"
                        + "        LEADING(a b)\n"
                        + "        USE_NL(b)\n"
                        + "    */\n"
                        + "    c1\n"
                        + "from t";

        String result =
                prependSelectItem(
                        sql,
                        "marker"
                );

        assertEquals(
                "select\n"
                        + "    /*+\n"
                        + "        LEADING(a b)\n"
                        + "        USE_NL(b)\n"
                        + "    */\n"
                        + "    marker, c1\n"
                        + "from t",
                result
        );
    }

    @Test
    public void prependsOnlyToOuterSelectWithNestedOptimizerHint() {
        String sql =
                "select c1, (\n"
                        + "    select "
                        + "/*+ INDEX(x IDX_X) */ "
                        + "x.value\n"
                        + "    from x\n"
                        + ") as nested_value\n"
                        + "from t";

        String result =
                prependSelectItem(
                        sql,
                        "marker"
                );

        assertEquals(
                "select marker, c1, (\n"
                        + "    select "
                        + "/*+ INDEX(x IDX_X) */ "
                        + "x.value\n"
                        + "    from x\n"
                        + ") as nested_value\n"
                        + "from t",
                result
        );
    }

    @Test
    public void preservesOptimizerHintContentsExactly() {
        String sql =
                "select "
                        + "/*+ CARDINALITY(t 100) "
                        + "SOME_HINT(~ 'text') */ "
                        + "c1 from t";

        String result =
                prependSelectItem(
                        sql,
                        "marker"
                );

        assertEquals(
                "select "
                        + "/*+ CARDINALITY(t 100) "
                        + "SOME_HINT(~ 'text') */ "
                        + "marker, c1 from t",
                result
        );
    }

    @Test
    public void preservesOrdinaryCommentAfterOptimizerHint() {
        String sql =
                "select "
                        + "/*+ INDEX(t IDX_T) */ "
                        + "/* projected value */ "
                        + "c1 from t";

        String result =
                prependSelectItem(
                        sql,
                        "marker"
                );

        assertEquals(
                "select "
                        + "/*+ INDEX(t IDX_T) */ "
                        + "/* projected value */ "
                        + "marker, c1 from t",
                result
        );
    }

    private String prependSelectItem(
            String sql,
            String selectItem
    ) {
        SelectQueryMap map =
                mapper.map(sql);

        TextChange change =
                SelectQueryTransformer.prependSelectItem(
                        map,
                        selectItem
                );

        return TextChangeApplier.apply(
                sql,
                Collections.singletonList(change)
        );
    }
}