package ru.inversion.util.parser.sql.lexer;

import org.junit.Test;
import ru.inversion.util.parser.lexer.LexerResult;
import ru.inversion.util.parser.lexer.Token;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class OperatorRecognizerTest {

    @Test
    public void singleCharacterOperatorsMustBeRecognized() {
        assertOperator("+");
        assertOperator("-");
        assertOperator("*");
        assertOperator("/");
        assertOperator("%");
        assertOperator("=");
        assertOperator("<");
        assertOperator(">");
        assertOperator("&");
        assertOperator("|");
        assertOperator("^");
        assertOperator("~");
    }

    @Test
    public void comparisonOperatorsMustBeRecognized() {
        assertOperator("<=");
        assertOperator(">=");
        assertOperator("<>");
        assertOperator("!=");
        assertOperator("!<");
        assertOperator("!>");
    }

    @Test
    public void compoundOperatorsMustBeRecognized() {
        assertOperator("||");
        assertOperator("&&");
        assertOperator("<<");
        assertOperator(">>");
        assertOperator("::");
        assertOperator(":=");
        assertOperator("->");
        assertOperator("->>");
        assertOperator("#>");
        assertOperator("#>>");
        assertOperator("#-");
        assertOperator("@>");
        assertOperator("<@");
        assertOperator("~*");
        assertOperator("!~");
        assertOperator("!~*");
        assertOperator("@@");
        assertOperator("@?");
    }

    @Test
    public void longestOperatorMustWin() {
        LexerResult<SqlTokenKind> result =
                new SqlLexer().tokenize("a->>b");

        assertEquals(4, result.tokens().size());

        assertToken(
                result,
                0,
                SqlTokenKind.WORD,
                "a"
        );

        assertToken(
                result,
                1,
                SqlTokenKind.OPERATOR,
                "->>"
        );

        assertToken(
                result,
                2,
                SqlTokenKind.WORD,
                "b"
        );

        assertEquals(
                SqlTokenKind.END_OF_FILE,
                result.tokens().get(3).kind()
        );
    }

    @Test
    public void expressionOperatorsMustRemainSeparate() {
        LexerResult<SqlTokenKind> result =
                new SqlLexer().tokenize("1+2*3");

        assertEquals(6, result.tokens().size());

        assertToken(
                result,
                0,
                SqlTokenKind.INTEGER_LITERAL,
                "1"
        );

        assertToken(
                result,
                1,
                SqlTokenKind.OPERATOR,
                "+"
        );

        assertToken(
                result,
                2,
                SqlTokenKind.INTEGER_LITERAL,
                "2"
        );

        assertToken(
                result,
                3,
                SqlTokenKind.OPERATOR,
                "*"
        );

        assertToken(
                result,
                4,
                SqlTokenKind.INTEGER_LITERAL,
                "3"
        );
    }

    @Test
    public void castOperatorMustBeSingleToken() {
        LexerResult<SqlTokenKind> result =
                new SqlLexer().tokenize("value::text");

        assertEquals(4, result.tokens().size());

        assertToken(
                result,
                0,
                SqlTokenKind.WORD,
                "value"
        );

        assertToken(
                result,
                1,
                SqlTokenKind.OPERATOR,
                "::"
        );

        assertToken(
                result,
                2,
                SqlTokenKind.WORD,
                "text"
        );
    }

    @Test
    public void lineCommentMustWinOverMinusOperator() {
        String sql = "-- comment";

        LexerResult<SqlTokenKind> result =
                new SqlLexer().tokenize(sql);

        assertEquals(
                SqlTokenKind.LINE_COMMENT,
                result.tokens().get(0).kind()
        );

        assertEquals(
                sql,
                result.text(result.tokens().get(0))
        );
    }

    @Test
    public void blockCommentMustWinOverDivisionOperator() {
        String sql = "/* comment */";

        LexerResult<SqlTokenKind> result =
                new SqlLexer().tokenize(sql);

        assertEquals(
                SqlTokenKind.BLOCK_COMMENT,
                result.tokens().get(0).kind()
        );

        assertEquals(
                sql,
                result.text(result.tokens().get(0))
        );
    }

    private static void assertOperator(String operator) {
        LexerResult<SqlTokenKind> result =
                new SqlLexer().tokenize(operator);

        assertEquals(
                "Unexpected token count for operator " + operator,
                2,
                result.tokens().size()
        );

        assertEquals(
                operator,
                SqlTokenKind.OPERATOR,
                result.tokens().get(0).kind()
        );

        assertEquals(
                operator,
                result.text(result.tokens().get(0))
        );

        assertEquals(
                SqlTokenKind.END_OF_FILE,
                result.tokens().get(1).kind()
        );
    }

    private static void assertToken(
            LexerResult<SqlTokenKind> result,
            int index,
            SqlTokenKind expectedKind,
            String expectedText
    ) {
        List<Token<SqlTokenKind>> tokens =
                result.tokens();

        Token<SqlTokenKind> token =
                tokens.get(index);

        assertEquals(expectedKind, token.kind());
        assertEquals(expectedText, result.text(token));
    }
}