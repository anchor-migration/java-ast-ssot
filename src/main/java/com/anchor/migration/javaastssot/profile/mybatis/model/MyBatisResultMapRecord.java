package com.anchor.migration.javaastssot.profile.mybatis.model;

public record MyBatisResultMapRecord(
        String sourceFile,
        String resultMapId,
        String typeStableId,
        String mappingRole,
        String tableName,
        String statementId) {}
