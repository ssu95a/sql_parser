package ru.inversion.util.parser.sql.parser;


import ru.inversion.util.parser.lexer.LexerResult;
import ru.inversion.util.parser.lexer.Token;
import ru.inversion.util.parser.sql.lexer.SqlTokenKind;
import ru.inversion.util.parser.text.SourceText;
import ru.inversion.util.parser.text.TextRange;

import java.util.List;
import java.util.Objects;

/**
 * Курсор по значимым SQL-токенам.
 * <p>
 * Исходный LexerResult остаётся неизменным и содержит полный поток,
 * включая пробелы и комментарии.
 * <p>
 * Методы current(), peek() и consume() автоматически пропускают
 * токены, для которых SqlTokenKind.isTrivia() возвращает true.
 */
public final class SqlTokenCursor {

    private final LexerResult<SqlTokenKind> result;
    private final List<Token<SqlTokenKind>> tokens;

    /**
     * Индекс текущего значимого токена в полном списке LexerResult.
     */
    private int index;

    public SqlTokenCursor(
            LexerResult<SqlTokenKind> result
    ) {
        this.result = Objects.requireNonNull(result, "result");
        this.tokens = result.tokens();

        if (tokens.isEmpty()) {
            throw new IllegalArgumentException(
                    "LexerResult contains no tokens"
            );
        }

        Token<SqlTokenKind> last =
                tokens.get(tokens.size() - 1);

        if (last.kind() != SqlTokenKind.END_OF_FILE) {
            throw new IllegalArgumentException(
                    "LexerResult does not end with END_OF_FILE"
            );
        }

        this.index = skipTrivia(0);
    }

    /**
     * Возвращает текущий значимый токен.
     *
     * После окончания входного текста всегда возвращает END_OF_FILE.
     */
    public Token<SqlTokenKind> current() {
        return tokens.get(index);
    }

    /**
     * Возвращает значимый токен на указанном расстоянии.
     *
     * peek(0) — текущий токен;
     * peek(1) — следующий значимый токен.
     *
     * Если указанная позиция находится после конца потока,
     * возвращается END_OF_FILE.
     */
    public Token<SqlTokenKind> peek(int distance) {
        if (distance < 0) {
            throw new IllegalArgumentException(
                    "distance < 0: " + distance
            );
        }

        int candidateIndex = index;

        for (int currentDistance = 0;
             currentDistance < distance;
             currentDistance++) {

            if (tokens.get(candidateIndex).kind()
                    == SqlTokenKind.END_OF_FILE) {
                return tokens.get(candidateIndex);
            }

            candidateIndex = skipTrivia(candidateIndex + 1);
        }

        return tokens.get(candidateIndex);
    }

    /**
     * Возвращает текущий значимый токен и перемещает курсор
     * к следующему значимому токену.
     *
     * END_OF_FILE не потребляется: повторные вызовы после конца
     * возвращают тот же END_OF_FILE.
     */
    public Token<SqlTokenKind> consume() {
        Token<SqlTokenKind> consumed = current();

        if (consumed.kind() != SqlTokenKind.END_OF_FILE) {
            index = skipTrivia(index + 1);
        }

        return consumed;
    }

    public boolean consumeIf(SqlTokenKind kind) {
        Objects.requireNonNull(kind, "kind");

        if (!is(kind)) {
            return false;
        }

        consume();
        return true;
    }

    public boolean consumeWordIf(String expected) {

        Objects.requireNonNull(expected, "expected");

        if (!isWord(expected)) {
            return false;
        }
        consume();
        return true;
    }

    /** */
    public boolean is(SqlTokenKind kind) {
        Objects.requireNonNull(kind, "kind");
        return current().kind() == kind;
    }

    /**
     * Проверяет, является ли текущий токен словом с указанным текстом.
     *
     * Сравнение выполняется без учёта регистра.
     */
    public boolean isWord(String expected) {
        Objects.requireNonNull(expected, "expected");

        Token<SqlTokenKind> token = current();

        return token.kind() == SqlTokenKind.WORD
                && regionMatchesIgnoreCase(
                result.source(),
                token.range(),
                expected
        );
    }

    public boolean isEnd() {
        return is(SqlTokenKind.END_OF_FILE);
    }

    public LexerResult<SqlTokenKind> result() {
        return result;
    }

    /**
     * Индекс текущего токена в полном потоке, включая trivia.
     *
     * Может понадобиться parser-у для фиксации границ узлов.
     */
    public int tokenIndex() {
        return index;
    }

    private int skipTrivia(int startIndex) {
        int candidateIndex = startIndex;

        while (candidateIndex < tokens.size()) {
            Token<SqlTokenKind> token =
                    tokens.get(candidateIndex);

            if (!token.kind().isTrivia()) {
                return candidateIndex;
            }

            candidateIndex++;
        }

        /*
         * Конструктор проверяет наличие END_OF_FILE,
         * поэтому эта ситуация означает повреждённый поток.
         */
        throw new IllegalStateException(
                "No END_OF_FILE token after index " + startIndex
        );
    }

    private static boolean regionMatchesIgnoreCase(
            SourceText source,
            TextRange range,
            String expected
    ) {
        if (range.length() != expected.length()) {
            return false;
        }

        for (int index = 0; index < expected.length(); index++) {
            char actualCharacter =
                    source.charAt(range.start() + index);

            char expectedCharacter =
                    expected.charAt(index);

            if (!equalsIgnoreCase(
                    actualCharacter,
                    expectedCharacter
            )) {
                return false;
            }
        }

        return true;
    }

    /**
     * Логика соответствует общему поведению сравнения символов
     * без учёта регистра в Java.
     */
    private static boolean equalsIgnoreCase(
            char first,
            char second
    ) {
        if (first == second) {
            return true;
        }

        char firstUpper = Character.toUpperCase(first);
        char secondUpper = Character.toUpperCase(second);

        if (firstUpper == secondUpper) {
            return true;
        }

        return Character.toLowerCase(firstUpper)
                == Character.toLowerCase(secondUpper);
    }


}