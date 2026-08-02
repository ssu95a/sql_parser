package ru.inversion.util.parser.sql.parser;

/**
 * Стабильные коды диагностик SQL parser-а.
 */
public final class SqlDiagnosticCodes {

    public static final String EXPECTED_TOKEN =
            "SQL001";

    public static final String EXPECTED_WORD =
            "SQL002";

    public static final String UNEXPECTED_TOKEN =
            "SQL003";

    public static final String EXPECTED_EXPRESSION = "SQL004";

    public static final String EXPECTED_RIGHT_PARENTHESIS = "SQL005";

    private SqlDiagnosticCodes() {
    }
}