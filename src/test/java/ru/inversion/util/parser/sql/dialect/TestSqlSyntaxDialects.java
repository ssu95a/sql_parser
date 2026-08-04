package ru.inversion.util.parser.sql.dialect;

public final class TestSqlSyntaxDialects {

    public static final SqlSyntaxDialect NONE =
            new SqlSyntaxDialect() {
                @Override
                public boolean supports(
                        SqlSyntaxFeature feature
                ) {
                    return false;
                }
            };

    public static final SqlSyntaxDialect POSTGRES =
            new SqlSyntaxDialect() {
                @Override
                public boolean supports(
                        SqlSyntaxFeature feature
                ) {
                    return feature
                            == SqlSyntaxFeature
                            .POSTGRES_DOLLAR_QUOTED_STRING;
                }
            };

    public static final SqlSyntaxDialect ORACLE =
            new SqlSyntaxDialect() {
                @Override
                public boolean supports(
                        SqlSyntaxFeature feature
                ) {
                    return feature
                            == SqlSyntaxFeature
                            .ORACLE_Q_QUOTED_STRING;
                }
            };

    private TestSqlSyntaxDialects() {
    }
}