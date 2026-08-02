package ru.inversion.util.parser.sql.parser;

import org.junit.Test;
import ru.inversion.util.parser.diagnostic.Diagnostic;
import ru.inversion.util.parser.sql.ast.BinaryExpression;
import ru.inversion.util.parser.sql.ast.LiteralExpression;
import ru.inversion.util.parser.sql.ast.NameExpression;
import ru.inversion.util.parser.sql.ast.ParameterExpression;
import ru.inversion.util.parser.sql.ast.ParenthesizedExpression;
import ru.inversion.util.parser.sql.ast.SqlExpression;
import ru.inversion.util.parser.sql.ast.UnaryExpression;
import ru.inversion.util.parser.sql.lexer.SqlTokenKind;
import ru.inversion.util.parser.text.TextRange;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class SqlParserExpressionTest {

    @Test
    public void integerLiteralMustBeParsed() {
        SqlParseResult<SqlExpression> result = parse("123");

        assertTrue(result.isSuccessful());
        assertTrue(result.root() instanceof LiteralExpression);

        LiteralExpression expression =
                (LiteralExpression) result.root();

        assertEquals(
                SqlTokenKind.INTEGER_LITERAL,
                expression.literalKind()
        );

        assertEquals(
                "123",
                result.lexerResult().text(expression.token())
        );
    }

    @Test
    public void stringLiteralMustBeParsed() {
        SqlParseResult<SqlExpression> result = parse("'text'");

        assertTrue(result.isSuccessful());
        assertTrue(result.root() instanceof LiteralExpression);

        LiteralExpression expression =
                (LiteralExpression) result.root();

        assertEquals(
                SqlTokenKind.STRING_LITERAL,
                expression.literalKind()
        );
    }

    @Test
    public void namedParameterMustBeParsed() {
        SqlParseResult<SqlExpression> result =
                parse(":customerId");

        assertTrue(result.isSuccessful());
        assertTrue(result.root() instanceof ParameterExpression);

        ParameterExpression expression =
                (ParameterExpression) result.root();

        assertEquals(
                SqlTokenKind.NAMED_PARAMETER,
                expression.parameterKind()
        );
    }

    @Test
    public void jdbcParameterMustBeParsed() {
        SqlParseResult<SqlExpression> result = parse("?");

        assertTrue(result.isSuccessful());
        assertTrue(result.root() instanceof ParameterExpression);

        ParameterExpression expression =
                (ParameterExpression) result.root();

        assertEquals(
                SqlTokenKind.JDBC_PARAMETER,
                expression.parameterKind()
        );
    }

    @Test
    public void postgresParameterMustBeParsed() {
        SqlParseResult<SqlExpression> result = parse("$25");

        assertTrue(result.isSuccessful());
        assertTrue(result.root() instanceof ParameterExpression);

        ParameterExpression expression =
                (ParameterExpression) result.root();

        assertEquals(
                SqlTokenKind.POSTGRES_POSITIONAL_PARAMETER,
                expression.parameterKind()
        );
    }

    @Test
    public void wordMustBeParsedAsName() {
        SqlParseResult<SqlExpression> result =
                parse("customer_id");

        assertTrue(result.isSuccessful());
        assertTrue(result.root() instanceof NameExpression);

        NameExpression expression =
                (NameExpression) result.root();

        assertFalse(expression.isQuoted());

        assertEquals(
                SqlTokenKind.WORD,
                expression.nameKind()
        );
    }

    @Test
    public void quotedIdentifierMustBeParsedAsName() {
        SqlParseResult<SqlExpression> result =
                parse("\"Customer Name\"");

        assertTrue(result.isSuccessful());
        assertTrue(result.root() instanceof NameExpression);

        NameExpression expression =
                (NameExpression) result.root();

        assertTrue(expression.isQuoted());

        assertEquals(
                SqlTokenKind.QUOTED_IDENTIFIER,
                expression.nameKind()
        );
    }

    @Test
    public void parserMustIgnoreTrivia() {
        SqlParseResult<SqlExpression> result =
                parse(" /* before */ 123 -- after\n");

        assertTrue(result.isSuccessful());
        assertTrue(result.root() instanceof LiteralExpression);
    }

    @Test
    public void emptySourceMustProduceDiagnostic() {
        SqlParseResult<SqlExpression> result = parse("");

        assertNull(result.root());
        assertFalse(result.hasRoot());
        assertTrue(result.hasErrors());
        assertFalse(result.isSuccessful());
        assertEquals(1, result.diagnostics().size());

        Diagnostic diagnostic =
                result.diagnostics().get(0);

        assertEquals(
                SqlDiagnosticCodes.EXPECTED_EXPRESSION,
                diagnostic.code()
        );

        assertEquals(
                new TextRange(0, 0),
                diagnostic.range()
        );
    }

    @Test
    public void unsupportedPrimaryOperatorMustProduceDiagnostic() {
        SqlParseResult<SqlExpression> result = parse("*");

        assertNull(result.root());
        assertTrue(result.hasErrors());

        Diagnostic diagnostic =
                result.diagnostics().get(0);

        assertEquals(
                SqlDiagnosticCodes.EXPECTED_EXPRESSION,
                diagnostic.code()
        );

        assertEquals(
                new TextRange(0, 1),
                diagnostic.range()
        );
    }

    @Test
    public void trailingTokenMustProduceDiagnostic() {
        SqlParseResult<SqlExpression> result =
                parse("first second");

        assertTrue(result.hasRoot());
        assertTrue(result.root() instanceof NameExpression);
        assertTrue(result.hasErrors());
        assertFalse(result.isSuccessful());
        assertEquals(1, result.diagnostics().size());

        Diagnostic diagnostic =
                result.diagnostics().get(0);

        assertEquals(
                SqlDiagnosticCodes.UNEXPECTED_TOKEN,
                diagnostic.code()
        );

        assertEquals(
                new TextRange(6, 12),
                diagnostic.range()
        );
    }

    @Test
    public void parenthesizedLiteralMustBeParsed() {
        SqlParseResult<SqlExpression> result =
                parse("(123)");

        assertTrue(result.isSuccessful());
        assertTrue(
                result.root()
                        instanceof ParenthesizedExpression
        );

        ParenthesizedExpression expression =
                (ParenthesizedExpression) result.root();

        assertTrue(
                expression.expression()
                        instanceof LiteralExpression
        );

        assertTrue(expression.hasRightParenthesis());

        assertEquals(
                new TextRange(0, 5),
                expression.range()
        );
    }

    @Test
    public void parenthesizedParameterMustBeParsed() {
        SqlParseResult<SqlExpression> result =
                parse("(:id)");

        assertTrue(result.isSuccessful());
        assertTrue(
                result.root()
                        instanceof ParenthesizedExpression
        );

        ParenthesizedExpression expression =
                (ParenthesizedExpression) result.root();

        assertTrue(
                expression.expression()
                        instanceof ParameterExpression
        );
    }

    @Test
    public void nestedParenthesesMustBeParsed() {
        SqlParseResult<SqlExpression> result =
                parse("((value))");

        assertTrue(result.isSuccessful());

        ParenthesizedExpression outer =
                (ParenthesizedExpression) result.root();

        assertTrue(
                outer.expression()
                        instanceof ParenthesizedExpression
        );

        ParenthesizedExpression inner =
                (ParenthesizedExpression) outer.expression();

        assertTrue(
                inner.expression()
                        instanceof NameExpression
        );

        assertEquals(
                new TextRange(0, 9),
                outer.range()
        );

        assertEquals(
                new TextRange(1, 8),
                inner.range()
        );
    }

    @Test
    public void missingRightParenthesisMustProducePartialAst() {
        SqlParseResult<SqlExpression> result =
                parse("(123");

        assertTrue(result.hasRoot());
        assertTrue(result.hasErrors());
        assertFalse(result.isSuccessful());
        assertTrue(
                result.root()
                        instanceof ParenthesizedExpression
        );

        ParenthesizedExpression expression =
                (ParenthesizedExpression) result.root();

        assertFalse(expression.hasRightParenthesis());
        assertNull(expression.rightParenthesis());

        assertEquals(
                new TextRange(0, 4),
                expression.range()
        );

        assertEquals(
                SqlDiagnosticCodes.EXPECTED_RIGHT_PARENTHESIS,
                result.diagnostics().get(0).code()
        );

        assertEquals(
                new TextRange(4, 4),
                result.diagnostics().get(0).range()
        );
    }

    @Test
    public void emptyParenthesesMustProduceDiagnostic() {
        SqlParseResult<SqlExpression> result = parse("()");

        assertNull(result.root());
        assertTrue(result.hasErrors());

        assertEquals(
                SqlDiagnosticCodes.EXPECTED_EXPRESSION,
                result.diagnostics().get(0).code()
        );

        assertEquals(
                new TextRange(1, 2),
                result.diagnostics().get(0).range()
        );
    }

    @Test
    public void triviaInsideParenthesesMustBeIgnored() {
        SqlParseResult<SqlExpression> result =
                parse("( /* before */ :id -- after\n )");

        assertTrue(result.isSuccessful());
        assertTrue(
                result.root()
                        instanceof ParenthesizedExpression
        );

        ParenthesizedExpression expression =
                (ParenthesizedExpression) result.root();

        assertTrue(expression.hasRightParenthesis());
        assertTrue(
                expression.expression()
                        instanceof ParameterExpression
        );
    }

    @Test
    public void negativeLiteralMustBeParsed() {
        SqlParseResult<SqlExpression> result =
                parse("-123");

        assertTrue(result.isSuccessful());
        assertTrue(result.root() instanceof UnaryExpression);

        UnaryExpression unary =
                (UnaryExpression) result.root();

        assertEquals(
                "-",
                result.lexerResult().text(unary.operator())
        );

        assertTrue(
                unary.operand()
                        instanceof LiteralExpression
        );

        assertEquals(
                new TextRange(0, 4),
                unary.range()
        );
    }

    @Test
    public void unaryOperatorMayPrecedeName() {
        SqlParseResult<SqlExpression> result =
                parse("~mask");

        assertTrue(result.isSuccessful());
        assertTrue(result.root() instanceof UnaryExpression);

        UnaryExpression unary =
                (UnaryExpression) result.root();

        assertEquals(
                "~",
                result.lexerResult().text(unary.operator())
        );

        assertTrue(
                unary.operand()
                        instanceof NameExpression
        );
    }

    @Test
    public void unaryOperatorMayPrecedeParentheses() {
        SqlParseResult<SqlExpression> result =
                parse("-(123)");

        assertTrue(result.isSuccessful());
        assertTrue(result.root() instanceof UnaryExpression);

        UnaryExpression unary =
                (UnaryExpression) result.root();

        assertTrue(
                unary.operand()
                        instanceof ParenthesizedExpression
        );

        assertEquals(
                new TextRange(0, 6),
                unary.range()
        );
    }

    @Test
    public void unaryOperatorsMustBeRightAssociative() {
        SqlParseResult<SqlExpression> result =
                parse("+-123");

        assertTrue(result.isSuccessful());
        assertTrue(result.root() instanceof UnaryExpression);

        UnaryExpression outer =
                (UnaryExpression) result.root();

        assertEquals(
                "+",
                result.lexerResult().text(outer.operator())
        );

        assertTrue(
                outer.operand()
                        instanceof UnaryExpression
        );

        UnaryExpression inner =
                (UnaryExpression) outer.operand();

        assertEquals(
                "-",
                result.lexerResult().text(inner.operator())
        );

        assertTrue(
                inner.operand()
                        instanceof LiteralExpression
        );
    }

    @Test
    public void unaryOperatorWithoutOperandMustProduceDiagnostic() {
        SqlParseResult<SqlExpression> result = parse("-");

        assertNull(result.root());
        assertTrue(result.hasErrors());

        assertEquals(
                SqlDiagnosticCodes.EXPECTED_EXPRESSION,
                result.diagnostics().get(0).code()
        );

        assertEquals(
                new TextRange(1, 1),
                result.diagnostics().get(0).range()
        );
    }

    @Test
    public void additionMustBeParsed() {
        SqlParseResult<SqlExpression> result =
                parse("left + right");

        assertTrue(result.isSuccessful());
        assertTrue(result.root() instanceof BinaryExpression);

        BinaryExpression expression =
                (BinaryExpression) result.root();

        assertTrue(
                expression.left()
                        instanceof NameExpression
        );

        assertTrue(
                expression.right()
                        instanceof NameExpression
        );

        assertEquals(
                "+",
                result.lexerResult().text(expression.operator())
        );

        assertEquals(
                new TextRange(0, 12),
                expression.range()
        );
    }

    @Test
    public void multiplicationMustHaveHigherPrecedenceThanAddition() {
        SqlParseResult<SqlExpression> result =
                parse("a + b * c");

        assertTrue(result.isSuccessful());
        assertTrue(result.root() instanceof BinaryExpression);

        BinaryExpression addition =
                (BinaryExpression) result.root();

        assertEquals(
                "+",
                result.lexerResult().text(addition.operator())
        );

        assertTrue(
                addition.left()
                        instanceof NameExpression
        );

        assertTrue(
                addition.right()
                        instanceof BinaryExpression
        );

        BinaryExpression multiplication =
                (BinaryExpression) addition.right();

        assertEquals(
                "*",
                result.lexerResult().text(
                        multiplication.operator()
                )
        );

        assertTrue(
                multiplication.left()
                        instanceof NameExpression
        );

        assertTrue(
                multiplication.right()
                        instanceof NameExpression
        );
    }

    @Test
    public void subtractionMustBeLeftAssociative() {
        SqlParseResult<SqlExpression> result =
                parse("a - b - c");

        assertTrue(result.isSuccessful());
        assertTrue(result.root() instanceof BinaryExpression);

        BinaryExpression outer =
                (BinaryExpression) result.root();

        assertEquals(
                "-",
                result.lexerResult().text(outer.operator())
        );

        assertTrue(
                outer.left()
                        instanceof BinaryExpression
        );

        BinaryExpression inner =
                (BinaryExpression) outer.left();

        assertEquals(
                "-",
                result.lexerResult().text(inner.operator())
        );

        assertTrue(
                inner.left()
                        instanceof NameExpression
        );

        assertTrue(
                inner.right()
                        instanceof NameExpression
        );

        assertTrue(
                outer.right()
                        instanceof NameExpression
        );
    }

    @Test
    public void parenthesesMustOverrideBinaryPrecedence() {
        SqlParseResult<SqlExpression> result =
                parse("(a + b) * c");

        assertTrue(result.isSuccessful());
        assertTrue(result.root() instanceof BinaryExpression);

        BinaryExpression multiplication =
                (BinaryExpression) result.root();

        assertEquals(
                "*",
                result.lexerResult().text(
                        multiplication.operator()
                )
        );

        assertTrue(
                multiplication.left()
                        instanceof ParenthesizedExpression
        );

        ParenthesizedExpression parentheses =
                (ParenthesizedExpression)
                        multiplication.left();

        assertTrue(
                parentheses.expression()
                        instanceof BinaryExpression
        );

        BinaryExpression addition =
                (BinaryExpression)
                        parentheses.expression();

        assertEquals(
                "+",
                result.lexerResult().text(addition.operator())
        );

        assertTrue(
                multiplication.right()
                        instanceof NameExpression
        );
    }

    @Test
    public void unaryOperatorMustHaveHigherPrecedenceThanBinary() {
        SqlParseResult<SqlExpression> result =
                parse("-a * +b");

        assertTrue(result.isSuccessful());
        assertTrue(result.root() instanceof BinaryExpression);

        BinaryExpression multiplication =
                (BinaryExpression) result.root();

        assertEquals(
                "*",
                result.lexerResult().text(
                        multiplication.operator()
                )
        );

        assertTrue(
                multiplication.left()
                        instanceof UnaryExpression
        );

        assertTrue(
                multiplication.right()
                        instanceof UnaryExpression
        );

        UnaryExpression left =
                (UnaryExpression) multiplication.left();

        UnaryExpression right =
                (UnaryExpression) multiplication.right();

        assertEquals(
                "-",
                result.lexerResult().text(left.operator())
        );

        assertEquals(
                "+",
                result.lexerResult().text(right.operator())
        );
    }

    @Test
    public void comparisonMustHaveLowerPrecedenceThanAddition() {
        SqlParseResult<SqlExpression> result =
                parse("a + b >= c");

        assertTrue(result.isSuccessful());
        assertTrue(result.root() instanceof BinaryExpression);

        BinaryExpression comparison =
                (BinaryExpression) result.root();

        assertEquals(
                ">=",
                result.lexerResult().text(
                        comparison.operator()
                )
        );

        assertTrue(
                comparison.left()
                        instanceof BinaryExpression
        );

        BinaryExpression addition =
                (BinaryExpression) comparison.left();

        assertEquals(
                "+",
                result.lexerResult().text(addition.operator())
        );

        assertTrue(
                comparison.right()
                        instanceof NameExpression
        );
    }

    @Test
    public void andMustHaveHigherPrecedenceThanOr() {
        SqlParseResult<SqlExpression> result =
                parse("a = 1 OR b = 2 AND c = 3");

        assertTrue(result.isSuccessful());
        assertTrue(result.root() instanceof BinaryExpression);

        BinaryExpression orExpression =
                (BinaryExpression) result.root();

        assertEquals(
                "OR",
                result.lexerResult().text(
                        orExpression.operator()
                ).toUpperCase()
        );

        assertTrue(
                orExpression.left()
                        instanceof BinaryExpression
        );

        assertTrue(
                orExpression.right()
                        instanceof BinaryExpression
        );

        BinaryExpression andExpression =
                (BinaryExpression)
                        orExpression.right();

        assertEquals(
                "AND",
                result.lexerResult().text(
                        andExpression.operator()
                ).toUpperCase()
        );

        assertTrue(
                andExpression.left()
                        instanceof BinaryExpression
        );

        assertTrue(
                andExpression.right()
                        instanceof BinaryExpression
        );
    }

    @Test
    public void wordOperatorsMustBeCaseInsensitive() {
        SqlParseResult<SqlExpression> result =
                parse("a = 1 or b = 2 and c = 3");

        assertTrue(result.isSuccessful());
        assertTrue(result.root() instanceof BinaryExpression);

        BinaryExpression expression =
                (BinaryExpression) result.root();

        assertEquals(
                "or",
                result.lexerResult().text(
                        expression.operator()
                )
        );

        assertTrue(
                expression.right()
                        instanceof BinaryExpression
        );

        BinaryExpression right =
                (BinaryExpression) expression.right();

        assertEquals(
                "and",
                result.lexerResult().text(
                        right.operator()
                )
        );
    }

    @Test
    public void missingRightOperandMustProducePartialAst() {
        SqlParseResult<SqlExpression> result =
                parse("value +");

        assertTrue(result.hasRoot());
        assertTrue(result.hasErrors());
        assertFalse(result.isSuccessful());

        /*
         * BinaryExpression нельзя создать без правого операнда,
         * поэтому parser сохраняет успешно разобранную левую часть.
         */
        assertTrue(result.root() instanceof NameExpression);

        assertEquals(
                SqlDiagnosticCodes.EXPECTED_EXPRESSION,
                result.diagnostics().get(0).code()
        );

        assertEquals(
                new TextRange(7, 7),
                result.diagnostics().get(0).range()
        );
    }

    private static SqlParseResult<SqlExpression> parse(
            String sql
    ) {
        return new SqlParser(sql)
                .parseExpression();
    }
}
