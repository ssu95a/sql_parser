package ru.inversion.util.parser.text;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class TextChangeApplierTest {

    @Test
    public void returnsSourceWhenChangesAreEmpty() {
        String result =
                TextChangeApplier.apply(
                        "select 1",
                        Collections.<TextChange>emptyList()
                );

        assertEquals(
                "select 1",
                result
        );
    }

    @Test
    public void appliesReplacement() {
        String result =
                TextChangeApplier.apply(
                        "select 10",
                        Collections.singletonList(
                                change(7, 9, "?")
                        )
                );

        assertEquals(
                "select ?",
                result
        );
    }

    @Test
    public void appliesDeletion() {
        String result =
                TextChangeApplier.apply(
                        "select distinct c1 from t",
                        Collections.singletonList(
                                change(7, 16, "")
                        )
                );

        assertEquals(
                "select c1 from t",
                result
        );
    }

    @Test
    public void appliesInsertionAtBeginning() {
        String result =
                TextChangeApplier.apply(
                        "select c1",
                        Collections.singletonList(
                                change(0, 0, "/* query */ ")
                        )
                );

        assertEquals(
                "/* query */ select c1",
                result
        );
    }

    @Test
    public void appliesInsertionInMiddle() {
        String result =
                TextChangeApplier.apply(
                        "select c1 from t",
                        Collections.singletonList(
                                change(7, 7, "mark, ")
                        )
                );

        assertEquals(
                "select mark, c1 from t",
                result
        );
    }

    @Test
    public void appliesInsertionAtEnd() {
        String result =
                TextChangeApplier.apply(
                        "select c1 from t",
                        Collections.singletonList(
                                change(16, 16, " limit 10")
                        )
                );

        assertEquals(
                "select c1 from t limit 10",
                result
        );
    }

    @Test
    public void appliesChangesUsingOriginalCoordinates() {
        List<TextChange> changes =
                Arrays.asList(
                        change(11, 13, "?"),
                        change(7, 9, "?")
                );

        String result =
                TextChangeApplier.apply(
                        "select 10, 20 from t;",
                        changes
                );

        assertEquals(
                "select ?, ? from t;",
                result
        );
    }

    @Test
    public void acceptsChangesInArbitraryOrder() {
        List<TextChange> changes =
                Arrays.asList(
                        change(18, 18, " order by c1"),
                        change(7, 7, "mark, "),
                        change(15, 18, "table_name")
                );

        String result =
                TextChangeApplier.apply(
                        "select c1 from tab",
                        changes
                );

        assertEquals(
                "select mark, c1 from table_name order by c1",
                result
        );
    }
    @Test
    public void preservesOrderOfInsertionsAtSameOffset() {
        List<TextChange> changes =
                Arrays.asList(
                        change(7, 7, "mark, "),
                        change(7, 7, "audit, "),
                        change(7, 7, "version, ")
                );

        String result =
                TextChangeApplier.apply(
                        "select c1 from t",
                        changes
                );

        assertEquals(
                "select mark, audit, version, c1 from t",
                result
        );
    }

    @Test
    public void allowsAdjacentReplacements() {
        List<TextChange> changes =
                Arrays.asList(
                        change(1, 3, "X"),
                        change(3, 5, "Y")
                );

        String result =
                TextChangeApplier.apply(
                        "abcdef",
                        changes
                );

        assertEquals(
                "aXYf",
                result
        );
    }

    @Test
    public void allowsInsertionAtLeftReplacementBoundary() {
        List<TextChange> changes =
                Arrays.asList(
                        change(1, 5, "X"),
                        change(1, 1, "<")
                );

        String result =
                TextChangeApplier.apply(
                        "abcdef",
                        changes
                );

        assertEquals(
                "a<Xf",
                result
        );
    }

    @Test
    public void allowsInsertionAtRightReplacementBoundary() {
        List<TextChange> changes =
                Arrays.asList(
                        change(1, 5, "X"),
                        change(5, 5, ">")
                );

        String result =
                TextChangeApplier.apply(
                        "abcdef",
                        changes
                );

        assertEquals(
                "aX>f",
                result
        );
    }

    @Test
    public void allowsInsertionsAtBothReplacementBoundaries() {
        List<TextChange> changes =
                Arrays.asList(
                        change(1, 5, "X"),
                        change(1, 1, "<"),
                        change(5, 5, ">")
                );

        String result =
                TextChangeApplier.apply(
                        "abcdef",
                        changes
                );

        assertEquals(
                "a<X>f",
                result
        );
    }

    @Test
    public void ignoresEmptyChange() {
        String result =
                TextChangeApplier.apply(
                        "select c1",
                        Collections.singletonList(
                                change(4, 4, "")
                        )
                );

        assertEquals(
                "select c1",
                result
        );
    }

    @Test
    public void insertsIntoEmptySource() {
        String result =
                TextChangeApplier.apply(
                        "",
                        Collections.singletonList(
                                change(0, 0, "select 1")
                        )
                );

        assertEquals(
                "select 1",
                result
        );
    }

    @Test
    public void acceptsGeneralCharSequence() {
        StringBuilder source =
                new StringBuilder("select 10");

        String result =
                TextChangeApplier.apply(
                        source,
                        Collections.singletonList(
                                change(7, 9, "?")
                        )
                );

        assertEquals(
                "select ?",
                result
        );

        assertEquals(
                "select 10",
                source.toString()
        );
    }

    @Test
    public void rejectsPartiallyOverlappingRanges() {
        assertConflict(
                "abcdef",
                change(1, 4, "X"),
                change(3, 5, "Y")
        );
    }

    @Test
    public void rejectsContainedRange() {
        assertConflict(
                "abcdef",
                change(1, 5, "X"),
                change(2, 4, "Y")
        );
    }

    @Test
    public void rejectsEqualNonEmptyRanges() {
        assertConflict(
                "abcdef",
                change(1, 5, "X"),
                change(1, 5, "Y")
        );
    }

    @Test
    public void rejectsInsertionStrictlyInsideReplacement() {
        assertConflict(
                "abcdef",
                change(1, 5, "X"),
                change(3, 3, "Y")
        );
    }

    @Test
    public void rejectsDeletionContainingInsertion() {
        assertConflict(
                "abcdef",
                change(1, 5, ""),
                change(3, 3, "Y")
        );
    }

    @Test
    public void rejectsRangePastSourceEnd() {
        try {
            TextChangeApplier.apply(
                    "abc",
                    Collections.singletonList(
                            change(2, 4, "X")
                    )
            );

            fail(
                    "Expected range outside source "
                            + "to be rejected"
            );
        } catch (IllegalArgumentException expected) {
            assertEquals(
                    "Text change range [2, 4) "
                            + "exceeds source length 3",
                    expected.getMessage()
            );
        }
    }

    @Test
    public void rejectsInsertionPastSourceEnd() {
        try {
            TextChangeApplier.apply(
                    "abc",
                    Collections.singletonList(
                            change(4, 4, "X")
                    )
            );

            fail(
                    "Expected insertion outside source "
                            + "to be rejected"
            );
        } catch (IllegalArgumentException expected) {
            assertEquals(
                    "Text change range [4, 4) "
                            + "exceeds source length 3",
                    expected.getMessage()
            );
        }
    }

    @Test(expected = NullPointerException.class)
    public void rejectsNullSource() {
        TextChangeApplier.apply(
                null,
                Collections.<TextChange>emptyList()
        );
    }

    @Test(expected = NullPointerException.class)
    public void rejectsNullChanges() {
        TextChangeApplier.apply(
                "abc",
                null
        );
    }

    @Test
    public void rejectsNullChange() {
        try {
            TextChangeApplier.apply(
                    "abc",
                    Arrays.asList(
                            change(0, 1, "X"),
                            null
                    )
            );

            fail(
                    "Expected null change "
                            + "to be rejected"
            );
        } catch (NullPointerException expected) {
            assertEquals(
                    "changes[1]",
                    expected.getMessage()
            );
        }
    }

    private static TextChange change(
            int start,
            int end,
            String newText
    ) {
        return new TextChange(
                new TextRange(start, end),
                newText
        );
    }

    private static void assertConflict(
            String source,
            TextChange first,
            TextChange second
    ) {
        try {
            TextChangeApplier.apply(
                    source,
                    Arrays.asList(
                            first,
                            second
                    )
            );

            fail(
                    "Expected conflicting changes "
                            + "to be rejected"
            );
        } catch (IllegalArgumentException expected) {
            assertEquals(
                    "Conflicting text changes: "
                            + first.range()
                            + " and "
                            + second.range(),
                    expected.getMessage()
            );
        }
    }
}