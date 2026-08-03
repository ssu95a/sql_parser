package ru.inversion.util.parser.text;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class TextChangeTest {

    @Test
    public void storesRangeAndNewText() {
        TextRange range =
                new TextRange(2, 5);

        TextChange change =
                new TextChange(
                        range,
                        "value"
                );

        assertSame(
                range,
                change.range()
        );

        assertEquals(
                "value",
                change.newText()
        );
    }

    @Test
    public void recognizesInsertion() {
        TextChange change =
                new TextChange(
                        new TextRange(3, 3),
                        "value"
                );

        assertTrue(change.isInsertion());
        assertFalse(change.isDeletion());
        assertFalse(change.isReplacement());
        assertFalse(change.isEmpty());
    }

    @Test
    public void recognizesDeletion() {
        TextChange change =
                new TextChange(
                        new TextRange(3, 7),
                        ""
                );

        assertFalse(change.isInsertion());
        assertTrue(change.isDeletion());
        assertFalse(change.isReplacement());
        assertFalse(change.isEmpty());
    }

    @Test
    public void recognizesReplacement() {
        TextChange change =
                new TextChange(
                        new TextRange(3, 7),
                        "value"
                );

        assertFalse(change.isInsertion());
        assertFalse(change.isDeletion());
        assertTrue(change.isReplacement());
        assertFalse(change.isEmpty());
    }

    @Test
    public void recognizesEmptyChange() {
        TextChange change =
                new TextChange(
                        new TextRange(3, 3),
                        ""
                );

        assertFalse(change.isInsertion());
        assertFalse(change.isDeletion());
        assertFalse(change.isReplacement());
        assertTrue(change.isEmpty());
    }

    @Test
    public void equalChangesAreEqual() {
        TextChange first =
                new TextChange(
                        new TextRange(2, 5),
                        "value"
                );

        TextChange second =
                new TextChange(
                        new TextRange(2, 5),
                        "value"
                );

        assertEquals(first, second);
        assertEquals(
                first.hashCode(),
                second.hashCode()
        );
    }

    @Test
    public void changesWithDifferentRangesAreNotEqual() {
        TextChange first =
                new TextChange(
                        new TextRange(2, 5),
                        "value"
                );

        TextChange second =
                new TextChange(
                        new TextRange(3, 5),
                        "value"
                );

        assertNotEquals(first, second);
    }

    @Test
    public void changesWithDifferentTextAreNotEqual() {
        TextChange first =
                new TextChange(
                        new TextRange(2, 5),
                        "first"
                );

        TextChange second =
                new TextChange(
                        new TextRange(2, 5),
                        "second"
                );

        assertNotEquals(first, second);
    }

    @Test
    public void changeIsEqualToItself() {
        TextChange change =
                new TextChange(
                        new TextRange(2, 5),
                        "value"
                );

        assertEquals(change, change);
    }

    @Test
    public void changeIsNotEqualToNull() {
        TextChange change =
                new TextChange(
                        new TextRange(2, 5),
                        "value"
                );

        assertNotEquals(change, null);
    }

    @Test
    public void changeIsNotEqualToAnotherType() {
        TextChange change =
                new TextChange(
                        new TextRange(2, 5),
                        "value"
                );

        assertNotEquals(
                change,
                "value"
        );
    }

    @Test(expected = NullPointerException.class)
    public void rejectsNullRange() {
        new TextChange(
                null,
                "value"
        );
    }

    @Test(expected = NullPointerException.class)
    public void rejectsNullNewText() {
        new TextChange(
                new TextRange(0, 0),
                null
        );
    }
}