package ru.inversion.util.parser.lexer;

import ru.inversion.util.parser.text.SourceText;
import ru.inversion.util.parser.text.TextRange;

public final class Token<K> {

    private final TokenKind kind;
    private final TextRange range;

    public Token(TokenKind kind, TextRange range) {
        if (kind == null) {
            throw new IllegalArgumentException("kind is null");
        }

        if (range == null) {
            throw new IllegalArgumentException("range is null");
        }

        this.kind = kind;
        this.range = range;
    }

    public TokenKind kind() {
        return kind;
    }

    public TextRange range() {
        return range;
    }

    public String text(SourceText source) {
        return source.substring(range.start(), range.end());
    }

    @Override
    public String toString() {
        return kind + " " + range;
    }
}