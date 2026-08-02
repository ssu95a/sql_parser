package ru.inversion.util.parser.sql.ast;

import org.junit.Test;
import ru.inversion.util.parser.lexer.LexerResult;
import ru.inversion.util.parser.lexer.Token;
import ru.inversion.util.parser.sql.lexer.SqlLexer;
import ru.inversion.util.parser.sql.lexer.SqlTokenKind;
import ru.inversion.util.parser.text.TextRange;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class QualifiedNameExpressionTest {

    @Test
    public void twoPartNameMustBeAccepted() {
        LexerResult<SqlTokenKind> result =
                lex("schema.table");

        NameExpression schema =
                name(result, 0);

        Token<SqlTokenKind> dot =
                result.tokens().get(1);

        NameExpression table =
                name(result, 2);

        QualifiedNameExpression expression =
                new QualifiedNameExpression(
                        Arrays.asList(schema, table),
                        Collections.singletonList(dot)
                );

        assertEquals(2, expression.partCount());
        assertSame(schema, expression.firstPart());
        assertSame(table, expression.lastPart());

        assertTrue(expression.isComplete());
        assertFalse(expression.hasTrailingDot());

        assertEquals(
                new TextRange(0, 12),
                expression.range()
        );
    }

    @Test
    public void threePartNameMustBeAccepted() {
        LexerResult<SqlTokenKind> result =
                lex("catalog.schema.table");

        List<NameExpression> parts =
                Arrays.asList(
                        name(result, 0),
                        name(result, 2),
                        name(result, 4)
                );

        List<Token<SqlTokenKind>> dots =
                Arrays.asList(
                        result.tokens().get(1),
                        result.tokens().get(3)
                );

        QualifiedNameExpression expression =
                new QualifiedNameExpression(
                        parts,
                        dots
                );

        assertEquals(3, expression.partCount());
        assertEquals(2, expression.dots().size());

        assertTrue(expression.isComplete());

        assertEquals(
                new TextRange(0, 20),
                expression.range()
        );
    }

    @Test
    public void quotedPartsMustBeAccepted() {
        LexerResult<SqlTokenKind> result =
                lex("\"Some Schema\".\"Some Table\"");

        QualifiedNameExpression expression =
                new QualifiedNameExpression(
                        Arrays.asList(
                                name(result, 0),
                                name(result, 2)
                        ),
                        Collections.singletonList(
                                result.tokens().get(1)
                        )
                );

        assertTrue(
                expression.parts()
                        .get(0)
                        .isQuoted()
        );

        assertTrue(
                expression.parts()
                        .get(1)
                        .isQuoted()
        );

        assertEquals(
                new TextRange(0, 26),
                expression.range()
        );
    }

    @Test
    public void trailingDotMustBeAcceptedForPartialAst() {
        LexerResult<SqlTokenKind> result =
                lex("schema.");

        NameExpression schema =
                name(result, 0);

        Token<SqlTokenKind> dot =
                result.tokens().get(1);

        QualifiedNameExpression expression =
                new QualifiedNameExpression(
                        Collections.singletonList(schema),
                        Collections.singletonList(dot)
                );

        assertTrue(expression.hasTrailingDot());
        assertFalse(expression.isComplete());

        assertEquals(
                new TextRange(0, 7),
                expression.range()
        );
    }

    @Test(expected = UnsupportedOperationException.class)
    public void partsMustBeImmutable() {
        QualifiedNameExpression expression =
                expression("schema.table");

        expression.parts().clear();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void dotsMustBeImmutable() {
        QualifiedNameExpression expression =
                expression("schema.table");

        expression.dots().clear();
    }

    @Test(expected = IllegalArgumentException.class)
    public void emptyPartsMustBeRejected() {
        LexerResult<SqlTokenKind> result =
                lex(".");

        new QualifiedNameExpression(
                Collections.<NameExpression>emptyList(),
                Collections.singletonList(
                        result.tokens().get(0)
                )
        );
    }

    @Test(expected = IllegalArgumentException.class)
    public void nameWithoutDotMustBeRejected() {
        LexerResult<SqlTokenKind> result =
                lex("schema");

        new QualifiedNameExpression(
                Collections.singletonList(
                        name(result, 0)
                ),
                Collections.<Token<SqlTokenKind>>emptyList()
        );
    }

    @Test(expected = IllegalArgumentException.class)
    public void invalidPartsAndDotsCountMustBeRejected() {
        LexerResult<SqlTokenKind> result =
                lex("a.b");

        new QualifiedNameExpression(
                Collections.singletonList(
                        name(result, 0)
                ),
                Arrays.asList(
                        result.tokens().get(1),
                        result.tokens().get(1)
                )
        );
    }

    @Test(expected = IllegalArgumentException.class)
    public void nonDotTokenMustBeRejected() {
        LexerResult<SqlTokenKind> result =
                lex("schema table");

        new QualifiedNameExpression(
                Arrays.asList(
                        name(result, 0),
                        name(result, 2)
                ),
                Collections.singletonList(
                        result.tokens().get(2)
                )
        );
    }

    private static QualifiedNameExpression expression(
            String sql
    ) {
        LexerResult<SqlTokenKind> result =
                lex(sql);

        return new QualifiedNameExpression(
                Arrays.asList(
                        name(result, 0),
                        name(result, 2)
                ),
                Collections.singletonList(
                        result.tokens().get(1)
                )
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