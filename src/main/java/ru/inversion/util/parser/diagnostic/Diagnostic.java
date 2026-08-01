package ru.inversion.util.parser.diagnostic;

import ru.inversion.util.parser.text.TextRange;

import java.util.Objects;

/**
 * Сообщение об ошибке или предупреждении,
 * привязанное к диапазону исходного текста.
 */
public final class Diagnostic {

    private final String code;
    private final DiagnosticSeverity severity;
    private final String message;
    private final TextRange range;

    public Diagnostic(
            String code,
            DiagnosticSeverity severity,
            String message,
            TextRange range
    ) {
        this.code = requireNotEmpty(code, "code");
        this.severity =
                Objects.requireNonNull(severity, "severity");
        this.message = requireNotEmpty(message, "message");
        this.range = Objects.requireNonNull(range, "range");
    }

    public String code() {
        return code;
    }

    public DiagnosticSeverity severity() {
        return severity;
    }

    public String message() {
        return message;
    }

    public TextRange range() {
        return range;
    }

    public boolean isError() {
        return severity == DiagnosticSeverity.ERROR;
    }

    @Override
    public String toString() {
        return severity
                + " "
                + code
                + " "
                + range
                + ": "
                + message;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof Diagnostic)) {
            return false;
        }

        Diagnostic other = (Diagnostic) object;

        return code.equals(other.code)
                && severity == other.severity
                && message.equals(other.message)
                && range.equals(other.range);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                code,
                severity,
                message,
                range
        );
    }

    private static String requireNotEmpty(
            String value,
            String name
    ) {
        Objects.requireNonNull(value, name);

        if (value.isEmpty()) {
            throw new IllegalArgumentException(
                    name + " is empty"
            );
        }

        return value;
    }
}