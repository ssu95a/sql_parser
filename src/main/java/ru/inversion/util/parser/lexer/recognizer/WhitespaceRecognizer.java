package ru.inversion.util.parser.lexer.recognizer;

import ru.inversion.util.parser.lexer.TokenMatch;
import ru.inversion.util.parser.lexer.TokenRecognizer;
import ru.inversion.util.parser.sql.lexer.SqlTokenKind;
import ru.inversion.util.parser.text.SourceText;

/**
 * Распознаёт непрерывную последовательность пробельных символов.
 */
public final class WhitespaceRecognizer implements TokenRecognizer<SqlTokenKind> {

    @Override
    public TokenMatch<SqlTokenKind> match( SourceText source, int offset )
    {
        int character = source.get(offset);

        if( !isWhitespace(character) )
            return null;

        int end = offset + 1;

        while( isWhitespace(source.get(end)) )
        {
            end++;
        }

        return new TokenMatch<SqlTokenKind>( SqlTokenKind.WHITESPACE, end );
    }

    /** */
    private static boolean isWhitespace( int character )
    {
        if( character == SourceText.EOF)
            return false;

        char value = (char) character;

        return Character.isWhitespace(value) || Character.isSpaceChar(value);
    }
}