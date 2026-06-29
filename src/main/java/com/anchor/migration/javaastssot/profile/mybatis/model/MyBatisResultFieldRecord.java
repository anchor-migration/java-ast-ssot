package com.anchor.migration.javaastssot.profile.mybatis.model;

public record MyBatisResultFieldRecord(
        String sourceFile,
        String resultMapId,
        String propertyName,
        String columnName,
        String tableName) {}
