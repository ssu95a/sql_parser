package ru.inversion.util.parser.sql.transform;

import ru.inversion.util.parser.text.TextRange;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Карта значимых участков внешнего SELECT-запроса.
 */
public final class SelectQueryMap {

    private final SqlAnchor selectItemInsertion;

    /*
     * Если WHERE существует, заполнен wherePredicateRange.
     * Если WHERE отсутствует, заполнен whereInsertion.
     */
    private final TextRange wherePredicateRange;
    private final SqlAnchor whereInsertion;

    private final List<SqlParameterOccurrence> parameters;

    public SelectQueryMap(
            SqlAnchor selectItemInsertion,
            TextRange wherePredicateRange,
            SqlAnchor whereInsertion,
            List<SqlParameterOccurrence> parameters
    ) {
        this.selectItemInsertion =
                Objects.requireNonNull(
                        selectItemInsertion,
                        "selectItemInsertion"
                );

        /*
         * Должно быть известно либо существующее WHERE,
         * либо место для вставки нового WHERE.
         */
        if ((wherePredicateRange == null)
                == (whereInsertion == null)) {

            throw new IllegalArgumentException(
                    "Exactly one of wherePredicateRange "
                            + "and whereInsertion must be specified"
            );
        }

        this.wherePredicateRange =
                wherePredicateRange;

        this.whereInsertion =
                whereInsertion;

        Objects.requireNonNull(
                parameters,
                "parameters"
        );

        List<SqlParameterOccurrence> copy =
                new ArrayList<SqlParameterOccurrence>(
                        parameters.size()
                );

        for (int index = 0;
             index < parameters.size();
             index++) {

            copy.add(
                    Objects.requireNonNull(
                            parameters.get(index),
                            "parameters[" + index + "]"
                    )
            );
        }

        this.parameters =
                Collections.unmodifiableList(copy);
    }

    /**
     * Позиция перед первым элементом списка внешнего SELECT.
     */
    public SqlAnchor selectItemInsertion() {
        return selectItemInsertion;
    }

    /**
     * Возвращает true, если внешний SELECT содержит WHERE.
     */
    public boolean hasWhere() {
        return wherePredicateRange != null;
    }

    /**
     * Диапазон предиката существующего внешнего WHERE.
     *
     * <p>Диапазон не включает слово WHERE и trivia
     * непосредственно после него.</p>
     *
     * @throws IllegalStateException если WHERE отсутствует
     */
    public TextRange wherePredicateRange() {
        if (!hasWhere()) {
            throw new IllegalStateException(
                    "WHERE is absent"
            );
        }

        return wherePredicateRange;
    }

    /**
     * Позиция, в которой можно вставить новый WHERE.
     *
     * @throws IllegalStateException если WHERE уже существует
     */
    public SqlAnchor whereInsertion() {
        if (hasWhere()) {
            throw new IllegalStateException(
                    "WHERE is present"
            );
        }

        return whereInsertion;
    }

    /**
     * Все найденные параметры в порядке их появления.
     */
    public List<SqlParameterOccurrence> parameters() {
        return parameters;
    }
}