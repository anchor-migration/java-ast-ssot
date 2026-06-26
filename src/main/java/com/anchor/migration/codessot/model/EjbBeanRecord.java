package com.anchor.migration.codessot.model;

public record EjbBeanRecord(
        String descriptorFile,
        String ejbName,
        String ejbClass,
        String beanType,
        String sessionType,
        String persistenceType,
        String tableName) {}
