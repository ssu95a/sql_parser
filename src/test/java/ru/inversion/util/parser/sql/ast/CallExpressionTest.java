package ru.inversion.util.parser.sql.ast;

import org.junit.Test;
import ru.inversion.util.parser.lexer.LexerResult;
import ru.inversion.util.parser.lexer.Token;
import ru.inversion.util.parser.sql.lexer.SqlLexer;
import ru.inversion.util.parser.sql.lexer.SqlTokenKind;
import ru.inversion.util.parser.text.TextRange;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class CallExpressionTest {

    @Test
    public void callWithoutArgumentsMustBeAccepted() {
        LexerResult<SqlTokenKind> result =
                lex("now()");

        NameExpression callee =
                name(result, 0);

        CallExpression expression =
                new CallExpression(
                        callee,
                        result.tokens().get(1),
                        Collections.<SqlExpression>emptyList(),
                        Collections
                                .<Token<SqlTokenKind>>emptyList(),
                        result.tokens().get(2)
                );

        assertSame(callee, expression.callee());
        assertEquals(0, expression.argumentCount());
        assertFalse(expression.hasArguments());
        assertTrue(expression.hasRightParenthesis());
        assertFalse(expression.hasTrailingComma());

        assertEquals(
                new TextRange(0, 5),
                expression.range()
        );
    }

    @Test
    public void callWithOneArgumentMustBeAccepted() {
        LexerResult<SqlTokenKind> result =
                lex("sum(price)");

        NameExpression argument =
                name(result, 2);

        CallExpression expression =
                new CallExpression(
                        name(result, 0),
                        result.tokens().get(1),
                        Collections
                                .<SqlExpression>singletonList(
                                        argument
                                ),
                        Collections
                                .<Token<SqlTokenKind>>emptyList(),
                        result.tokens().get(3)
                );

        assertEquals(1, expression.argumentCount());
        assertSame(
                argument,
                expression.arguments().get(0)
        );

        assertTrue(expression.hasArguments());
        assertTrue(expression.hasRightParenthesis());
        assertFalse(expression.hasTrailingComma());

        assertEquals(
                new TextRange(0, 10),
                expression.range()
        );
    }

    @Test
    public void callWithMultipleArgumentsMustBeAccepted() {
        LexerResult<SqlTokenKind> result =
                lex("coalesce(a,b,0)");

        CallExpression expression =
                new CallExpression(
                        name(result, 0),
                        result.tokens().get(1),
                        Arrays.<SqlExpression>asList(
                                name(result, 2),
                                name(result, 4),
                                new LiteralExpression(
                                        result.tokens().get(6)
                                )
                        ),
                        Arrays.asList(
                                result.tokens().get(3),
                                result.tokens().get(5)
                        ),
                        result.tokens().get(7)
                );

        assertEquals(3, expression.argumentCount());
        assertEquals(2, expression.commas().size());

        assertTrue(expression.hasRightParenthesis());
        assertFalse(expression.hasTrailingComma());

        assertEquals(
                new TextRange(0, 15),
                expression.range()
        );
    }

    @Test
    public void qualifiedNameMayBeCallee() {
        LexerResult<SqlTokenKind> result =
                lex("schema.sum(price)");

        QualifiedNameExpression callee =
                new QualifiedNameExpression(
                        Arrays.asList(
                                name(result, 0),
                                name(result, 2)
                        ),
                        Collections.singletonList(
                                result.tokens().get(1)
                        )
                );

        CallExpression expression =
                new CallExpression(
                        callee,
                        result.tokens().get(3),
                        Collections
                                .<SqlExpression>singletonList(
                                        name(result, 4)
                                ),
                        Collections
                                .<Token<SqlTokenKind>>emptyList(),
                        result.tokens().get(5)
                );

        assertSame(callee, expression.callee());

        assertEquals(
                new TextRange(0, 17),
                expression.range()
        );
    }

    @Test
    public void missingRightParenthesisMustBeAcceptedForPartialAst() {
        LexerResult<SqlTokenKind> result =
                lex("sum(price");

        CallExpression expression =
                new CallExpression(
                        name(result, 0),
                        result.tokens().get(1),
                        Collections
                                .<SqlExpression>singletonList(
                                        name(result, 2)
                                ),
                        Collections
                                .<Token<SqlTokenKind>>emptyList(),
                        null
                );

        assertFalse(expression.hasRightParenthesis());
        assertFalse(expression.hasTrailingComma());

        assertEquals(
                new TextRange(0, 9),
                expression.range()
        );
    }

    @Test
    public void emptyPartialCallMustBeAccepted() {
        LexerResult<SqlTokenKind> result =
                lex("sum(");

        CallExpression expression =
                new CallExpression(
                        name(result, 0),
                        result.tokens().get(1),
                        Collections.<SqlExpression>emptyList(),
                        Collections
                                .<Token<SqlTokenKind>>emptyList(),
                        null
                );

        assertEquals(0, expression.argumentCount());
        assertFalse(expression.hasRightParenthesis());

        assertEquals(
                new TextRange(0, 4),
                expression.range()
        );
    }

    @Test
    public void trailingCommaMustBePreserved() {
        LexerResult<SqlTokenKind> result =
                lex("sum(price,");

        CallExpression expression =
                new CallExpression(
                        name(result, 0),
                        result.tokens().get(1),
                        Collections
                                .<SqlExpression>singletonList(
                                        name(result, 2)
                                ),
                        Collections.singletonList(
                                result.tokens().get(3)
                        ),
                        null
                );

        assertTrue(expression.hasTrailingComma());
        assertFalse(expression.hasRightParenthesis());

        assertEquals(
                new TextRange(0, 10),
                expression.range()
        );
    }

    @Test
    public void trailingCommaBeforeRightParenthesisMustBePreserved() {
        LexerResult<SqlTokenKind> result =
                lex("sum(price,)");

        CallExpression expression =
                new CallExpression(
                        name(result, 0),
                        result.tokens().get(1),
                        Collections
                                .<SqlExpression>singletonList(
                                        name(result, 2)
                                ),
                        Collections.singletonList(
                                result.tokens().get(3)
                        ),
                        result.tokens().get(4)
                );

        assertTrue(expression.hasTrailingComma());
        assertTrue(expression.hasRightParenthesis());

        assertEquals(
                new TextRange(0, 11),
                expression.range()
        );
    }

    @Test(expected = UnsupportedOperationException.class)
    public void argumentsMustBeImmutable() {
        CallExpression expression =
                oneArgumentCall();

        expression.arguments().clear();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void commasMustBeImmutable() {
        CallExpression expression =
                multipleArgumentCall();

        expression.commas().clear();
    }

    @Test(expected = NullPointerException.class)
    public void calleeMustNotBeNull() {
        LexerResult<SqlTokenKind> result =
                lex("sum()");

        new CallExpression(
                null,
                result.tokens().get(1),
                Collections.<SqlExpression>emptyList(),
                Collections
                        .<Token<SqlTokenKind>>emptyList(),
                result.tokens().get(2)
        );
    }

    @Test(expected = IllegalArgumentException.class)
    public void leftParenthesisMustHaveCorrectKind() {
        LexerResult<SqlTokenKind> result =
                lex("sum+price");

        new CallExpression(
                name(result, 0),
                result.tokens().get(1),
                Collections
                        .<SqlExpression>singletonList(
                                name(result, 2)
                        ),
                Collections
                        .<Token<SqlTokenKind>>emptyList(),
                null
        );
    }

    @Test(expected = IllegalArgumentException.class)
    public void commaMustHaveCorrectKind() {
        LexerResult<SqlTokenKind> call =
                lex("sum(a,b)");

        LexerResult<SqlTokenKind> operator =
                lex("+");

        new CallExpression(
                name(call, 0),
                call.tokens().get(1),
                Arrays.<SqlExpression>asList(
                        name(call, 2),
                        name(call, 4)
                ),
                Collections.singletonList(
                        operator.tokens().get(0)
                ),
                call.tokens().get(5)
        );
    }

    @Test(expected = IllegalArgumentException.class)
    public void invalidArgumentAndCommaCountMustBeRejected() {
        LexerResult<SqlTokenKind> result =
                lex("function(a,b)");

        new CallExpression(
                name(result, 0),
                result.tokens().get(1),
                Arrays.<SqlExpression>asList(
                        name(result, 2),
                        name(result, 4)
                ),
                Collections
                        .<Token<SqlTokenKind>>emptyList(),
                result.tokens().get(5)
        );
    }

    private static CallExpression oneArgumentCall() {
        LexerResult<SqlTokenKind> result =
                lex("sum(price)");

        return new CallExpression(
                name(result, 0),
                result.tokens().get(1),
                Collections
                        .<SqlExpression>singletonList(
                                name(result, 2)
                        ),
                Collections
                        .<Token<SqlTokenKind>>emptyList(),
                result.tokens().get(3)
        );
    }

    private static CallExpression multipleArgumentCall() {
        LexerResult<SqlTokenKind> result =
                lex("sum(a,b)");

        return new CallExpression(
                name(result, 0),
                result.tokens().get(1),
                Arrays.<SqlExpression>asList(
                        name(result, 2),
                        name(result, 4)
                ),
                Collections.singletonList(
                        result.tokens().get(3)
                ),
                result.tokens().get(5)
        );
    }

    private static NameExpression name(
            LexerResult<SqlTokenKind> result,
            int tokenIndex
    ) {
        return new NameExpression(
                result.tokens().get(tokenIndex)
        );
    }

    private static LexerResult<SqlTokenKind> lex(
            String sql
    ) {
        return new SqlLexer().tokenize(sql);
    }
}