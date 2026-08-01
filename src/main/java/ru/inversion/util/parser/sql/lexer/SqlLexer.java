package ru.inversion.util.parser.sql.lexer;


import ru.inversion.util.parser.lexer.LexerEngine;
import ru.inversion.util.parser.lexer.LexerResult;
import ru.inversion.util.parser.lexer.TokenRecognizer;
import ru.inversion.util.parser.lexer.recognizer.SingleCharacterRecognizer;
import ru.inversion.util.parser.lexer.recognizer.WhitespaceRecognizer;

import java.util.Arrays;
import java.util.List;

/**
 * Публичная точка входа SQL lexer-а.
 */
public final class SqlLexer {

    private final LexerEngine<SqlTokenKind> engine;

    public SqlLexer()
    {
        List<TokenRecognizer<SqlTokenKind>> recognizers = Arrays.asList( new WhitespaceRecognizer() );
        this.engine = new LexerEngine<>( recognizers, new SingleCharacterRecognizer<>(SqlTokenKind.UNKNOWN), SqlTokenKind.END_OF_FILE );
    }

    /** */
    public LexerResult<SqlTokenKind> tokenize( CharSequence source )
    {
        return engine.tokenize(source);
    }
}