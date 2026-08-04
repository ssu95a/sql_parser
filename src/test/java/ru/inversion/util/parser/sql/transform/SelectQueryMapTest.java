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
                        Collections
                                .<SqlParameterOccurrence>emptyList()
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
                        Collections
                                .<SqlParameterOccurrence>emptyList()
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
    public void rejectsBothWhereRepresentations() {
        try {
            new SelectQueryMap(
                    new SqlAnchor(7),
                    new TextRange(20, 25),
                    new SqlAnchor(30),
                    Collections
                            .<SqlParameterOccurrence>emptyList()
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
                    Collections
                            .<SqlParameterOccurrence>emptyList()
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

    @Test(expected = IllegalStateException.class)
    public void rejectsWhereRangeRequestWhenWhereIsAbsent() {
        SelectQueryMap map =
                new SelectQueryMap(
                        new SqlAnchor(7),
                        null,
                        new SqlAnchor(30),
                        Collections
                                .<SqlParameterOccurrence>emptyList()
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
                        Collections
                                .<SqlParameterOccurrence>emptyList()
                );

        map.whereInsertion();
    }
}