package com.anchor.migration.javaastssot.profile.mybatis;

public final class MyBatisIds {
    public static final String PROFILE_ID = "mybatis";

    private MyBatisIds() {}

    public static String sqlStatement(String mapperPath, String statementId) {
        return "sql:" + mapperPath + "#" + statementId;
    }
}
