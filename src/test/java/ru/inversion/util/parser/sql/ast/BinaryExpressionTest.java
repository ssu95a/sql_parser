package ru.inversion.util.parser.sql.ast;

import org.junit.Test;
import ru.inversion.util.parser.lexer.LexerResult;
import ru.inversion.util.parser.lexer.Token;
import ru.inversion.util.parser.sql.lexer.SqlLexer;
import ru.inversion.util.parser.sql.lexer.SqlTokenKind;
import ru.inversion.util.parser.text.TextRange;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

public class BinaryExpressionTest {

    @Test
    public void expressionMustContainOperandsAndOperator() {

        LexerResult<SqlTokenKind> lexerResult = new SqlLexer().tokenize("left + right");

        NameExpression left = new NameExpression( lexerResult.tokens().get(0) );

        Token<SqlTokenKind> operator =
                lexerResult.tokens().get(2);

        NameExpression right =
                new NameExpression(
                        lexerResult.tokens().get(4)
                );

        BinaryExpression expression =
                new BinaryExpression(
                        left,
                        operator,
                        right
                );

        assertSame(left, expression.left());
        assertSame(operator, expression.operator());
        assertSame(right, expression.right());

        assertEquals(
                new TextRange(0, 12),
                expression.range()
        );
    }

    @Test
    public void wordOperatorMustBeAccepted() {
        LexerResult<SqlTokenKind> lexerResult =
                new SqlLexer().tokenize("left AND right");

        BinaryExpression expression =
                new BinaryExpression(
                        new NameExpression(
                                lexerResult.tokens().get(0)
                        ),
                        lexerResult.tokens().get(2),
                        new NameExpression(
                                lexerResult.tokens().get(4)
                        )
                );

        assertEquals(
                SqlTokenKind.WORD,
                expression.operator().kind()
        );

        assertEquals(
                new TextRange(0, 14),
                expression.range()
        );
    }

    @Test(expected = NullPointerException.class)
    public void leftOperandMustNotBeNull() {
        LexerResult<SqlTokenKind> lexerResult =
                new SqlLexer().tokenize("+ right");

        new BinaryExpression(
                null,
                lexerResult.tokens().get(0),
                new NameExpression(
                        lexerResult.tokens().get(2)
                )
        );
    }

    @Test(expected = NullPointerException.class)
    public void operatorMustNotBeNull() {
        LexerResult<SqlTokenKind> lexerResult =
                new SqlLexer().tokenize("left right");

        new BinaryExpression(
                new NameExpression(
                        lexerResult.tokens().get(0)
                ),
                null,
                new NameExpression(
                        lexerResult.tokens().get(2)
                )
        );
    }

    @Test(expected = NullPointerException.class)
    public void rightOperandMustNotBeNull() {
        LexerResult<SqlTokenKind> lexerResult =
                new SqlLexer().tokenize("left +");

        new BinaryExpression(
                new NameExpression(
                        lexerResult.tokens().get(0)
                ),
                lexerResult.tokens().get(2),
                null
        );
    }

    @Test(expected = IllegalArgumentException.class)
    public void literalMustNotBeUsedAsOperator() {
        LexerResult<SqlTokenKind> lexerResult =
                new SqlLexer().tokenize("left 123 right");

        new BinaryExpression(
                new NameExpression(
                        lexerResult.tokens().get(0)
                ),
                lexerResult.tokens().get(2),
                new NameExpression(
                        lexerResult.tokens().get(4)
                )
        );
    }

    @Test(expected = IllegalArgumentException.class)
    public void operatorMustFollowLeftOperand() {
        LexerResult<SqlTokenKind> lexerResult =
                new SqlLexer().tokenize("+ left right");

        new BinaryExpression(
                new NameExpression(
                        lexerResult.tokens().get(2)
                ),
                lexerResult.tokens().get(0),
                new NameExpression(
                        lexerResult.tokens().get(4)
                )
        );
    }

    @Test(expected = IllegalArgumentException.class)
    public void rightOperandMustFollowOperator() {
        LexerResult<SqlTokenKind> lexerResult =
                new SqlLexer().tokenize("right + left");

        new BinaryExpression(
                new NameExpression(
                        lexerResult.tokens().get(4)
                ),
                lexerResult.tokens().get(2),
                new NameExpression(
                        lexerResult.tokens().get(0)
                )
        );
    }
}