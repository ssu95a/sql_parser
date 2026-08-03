package ru.inversion.util.parser.sql.transform;

import java.util.List;

public final class SelectQueryMap {

    private final List<SqlParameterOccurrence> parameters;

    public List<SqlParameterOccurrence> parameters() {
        return parameters;
    }
}