package ru.inversion.util.parser.diagnostic;

import org.junit.Test;
import ru.inversion.util.parser.text.TextRange;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DiagnosticBagTest {

    @Test
    public void emptyBagMustHaveNoErrors() {
        DiagnosticBag bag = new DiagnosticBag();

        assertTrue(bag.isEmpty());
        assertFalse(bag.hasErrors());
        assertEquals(0, bag.size());
    }

    @Test
    public void errorMustBeAdded() {
        DiagnosticBag bag = new DiagnosticBag();

        bag.error(
                "SQL001",
                new TextRange(5, 5),
                "Expected token"
        );

        assertEquals(1, bag.size());
        assertFalse(bag.isEmpty());
        assertTrue(bag.hasErrors());

        Diagnostic diagnostic =
                bag.diagnostics().get(0);

        assertEquals("SQL001", diagnostic.code());
        assertEquals(
                DiagnosticSeverity.ERROR,
                diagnostic.severity()
        );
        assertEquals(
                new TextRange(5, 5),
                diagnostic.range()
        );
    }

    @Test
    public void warningMustNotMakeBagErroneous() {
        DiagnosticBag bag = new DiagnosticBag();

        bag.warning(
                "SQL100",
                new TextRange(0, 1),
                "Warning"
        );

        assertEquals(1, bag.size());
        assertFalse(bag.hasErrors());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void diagnosticSnapshotMustBeImmutable() {
        DiagnosticBag bag = new DiagnosticBag();

        bag.error(
                "SQL001",
                new TextRange(0, 0),
                "Error"
        );

        List<Diagnostic> snapshot =
                bag.diagnostics();

        snapshot.clear();
    }

    @Test
    public void snapshotMustNotChangeAfterNewReports() {
        DiagnosticBag bag = new DiagnosticBag();

        List<Diagnostic> snapshot =
                bag.diagnostics();

        bag.error(
                "SQL001",
                new TextRange(0, 0),
                "Error"
        );

        assertTrue(snapshot.isEmpty());
        assertEquals(1, bag.size());
    }
}