package ru.inversion.util.parser.diagnostic;

import ru.inversion.util.parser.text.TextRange;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Изменяемый накопитель диагностических сообщений.
 *
 * Используется во время разбора. Наружу возвращаются
 * только неизменяемые снимки списка.
 */
public final class DiagnosticBag {

    private final List<Diagnostic> diagnostics =
            new ArrayList<Diagnostic>();

    public Diagnostic report(
            String code,
            DiagnosticSeverity severity,
            TextRange range,
            String message
    ) {
        Diagnostic diagnostic = new Diagnostic(
                code,
                severity,
                message,
                range
        );

        diagnostics.add(diagnostic);
        return diagnostic;
    }

    public Diagnostic error(
            String code,
            TextRange range,
            String message
    ) {
        return report(
                code,
                DiagnosticSeverity.ERROR,
                range,
                message
        );
    }

    public Diagnostic warning(
            String code,
            TextRange range,
            String message
    ) {
        return report(
                code,
                DiagnosticSeverity.WARNING,
                range,
                message
        );
    }

    public void add(Diagnostic diagnostic) {
        diagnostics.add(
                Objects.requireNonNull(
                        diagnostic,
                        "diagnostic"
                )
        );
    }

    public void addAll(
            Iterable<Diagnostic> source
    ) {
        Objects.requireNonNull(source, "source");

        for (Diagnostic diagnostic : source) {
            add(diagnostic);
        }
    }

    public boolean isEmpty() {
        return diagnostics.isEmpty();
    }

    public int size() {
        return diagnostics.size();
    }

    public boolean hasErrors() {
        for (Diagnostic diagnostic : diagnostics) {
            if (diagnostic.isError()) {
                return true;
            }
        }

        return false;
    }

    /**
     * Возвращает неизменяемый снимок текущего состояния.
     */
    public List<Diagnostic> diagnostics() {
        return Collections.unmodifiableList(
                new ArrayList<Diagnostic>(diagnostics)
        );
    }
}