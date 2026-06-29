package com.anchor.migration.javaastssot.profile.mybatis.model;

public record MyBatisStatementRecord(
        String sourceFile,
        String statementId,
        String statementType,
        String resultMapId,
        String resultTypeStableId,
        String sqlText,
        boolean joinQuery,
        String mappingRole) {}
