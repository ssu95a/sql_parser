package ru.inversion.util.parser.sql.transform;

import org.junit.Test;
import ru.inversion.util.parser.text.TextRange;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class SelectQueryMapTest {

    @Test
    public void representsExistingWhere() {
        SelectQueryMap map =
                new SelectQueryMap(
                        new SqlAnchor(7),
                        new TextRange(20, 25),
                        null,
                        null,
                        new SqlAnchor(40),
                        noParameters()
                );

        assertTrue(
                map.hasWhere()
        );

        assertEquals(
                new TextRange(20, 25),
                map.wherePredicateRange()
        );
    }

    @Test
    public void representsMissingWhere() {
        SelectQueryMap map =
                new SelectQueryMap(
                        new SqlAnchor(7),
                        null,
                        new SqlAnchor(30),
                        null,
                        new SqlAnchor(40),
                        noParameters()
                );

        assertFalse(
                map.hasWhere()
        );

        assertEquals(
                new SqlAnchor(30),
                map.whereInsertion()
        );
    }

    @Test
    public void representsExistingOrderBy() {
        SelectQueryMap map =
                new SelectQueryMap(
                        new SqlAnchor(7),
                        null,
                        new SqlAnchor(20),
                        new TextRange(30, 40),
                        null,
                        noParameters()
                );

        assertTrue(
                map.hasOrderBy()
        );

        assertEquals(
                new TextRange(30, 40),
                map.orderByClauseRange()
        );
    }

    @Test
    public void representsMissingOrderBy() {
        SelectQueryMap map =
                new SelectQueryMap(
                        new SqlAnchor(7),
                        null,
                        new SqlAnchor(20),
                        null,
                        new SqlAnchor(40),
                        noParameters()
                );

        assertFalse(
                map.hasOrderBy()
        );

        assertEquals(
                new SqlAnchor(40),
                map.orderByInsertion()
        );
    }

    @Test
    public void rejectsBothWhereRepresentations() {
        try {
            new SelectQueryMap(
                    new SqlAnchor(7),
                    new TextRange(20, 25),
                    new SqlAnchor(30),
                    null,
                    new SqlAnchor(40),
                    noParameters()
            );

            fail(
                    "Expected invalid WHERE state"
            );
        } catch (IllegalArgumentException expected) {
            assertEquals(
                    "Exactly one of wherePredicateRange "
                            + "and whereInsertion must be specified",
                    expected.getMessage()
            );
        }
    }

    @Test
    public void rejectsMissingWhereRepresentations() {
        try {
            new SelectQueryMap(
                    new SqlAnchor(7),
                    null,
                    null,
                    null,
                    new SqlAnchor(40),
                    noParameters()
            );

            fail(
                    "Expected invalid WHERE state"
            );
        } catch (IllegalArgumentException expected) {
            assertEquals(
                    "Exactly one of wherePredicateRange "
                            + "and whereInsertion must be specified",
                    expected.getMessage()
            );
        }
    }

    @Test
    public void rejectsBothOrderByRepresentations() {
        try {
            new SelectQueryMap(
                    new SqlAnchor(7),
                    null,
                    new SqlAnchor(20),
                    new TextRange(30, 40),
                    new SqlAnchor(50),
                    noParameters()
            );

            fail(
                    "Expected invalid ORDER BY state"
            );
        } catch (IllegalArgumentException expected) {
            assertEquals(
                    "Exactly one of orderByClauseRange "
                            + "and orderByInsertion must be specified",
                    expected.getMessage()
            );
        }
    }

    @Test
    public void rejectsMissingOrderByRepresentations() {
        try {
            new SelectQueryMap(
                    new SqlAnchor(7),
                    null,
                    new SqlAnchor(20),
                    null,
                    null,
                    noParameters()
            );

            fail(
                    "Expected invalid ORDER BY state"
            );
        } catch (IllegalArgumentException expected) {
            assertEquals(
                    "Exactly one of orderByClauseRange "
                            + "and orderByInsertion must be specified",
                    expected.getMessage()
            );
        }
    }

    @Test(expected = IllegalStateException.class)
    public void rejectsWhereRangeRequestWhenWhereIsAbsent() {
        SelectQueryMap map =
                new SelectQueryMap(
                        new SqlAnchor(7),
                        null,
                        new SqlAnchor(30),
                        null,
                        new SqlAnchor(40),
                        noParameters()
                );

        map.wherePredicateRange();
    }

    @Test(expected = IllegalStateException.class)
    public void rejectsWhereInsertionRequestWhenWhereIsPresent() {
        SelectQueryMap map =
                new SelectQueryMap(
                        new SqlAnchor(7),
                        new TextRange(20, 25),
                        null,
                        null,
                        new SqlAnchor(40),
                        noParameters()
                );

        map.whereInsertion();
    }

    @Test(expected = IllegalStateException.class)
    public void rejectsOrderByRangeRequestWhenOrderByIsAbsent() {
        SelectQueryMap map =
                new SelectQueryMap(
                        new SqlAnchor(7),
                        null,
                        new SqlAnchor(20),
                        null,
                        new SqlAnchor(40),
                        noParameters()
                );

        map.orderByClauseRange();
    }

    @Test(expected = IllegalStateException.class)
    public void rejectsOrderByInsertionRequestWhenOrderByIsPresent() {
        SelectQueryMap map =
                new SelectQueryMap(
                        new SqlAnchor(7),
                        null,
                        new SqlAnchor(20),
                        new TextRange(30, 40),
                        null,
                        noParameters()
                );

        map.orderByInsertion();
    }

    private static java.util.List<SqlParameterOccurrence>
    noParameters() {
        return Collections.emptyList();
    }
}