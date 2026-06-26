package com.anchor.migration.javaastssot.profile.javaee.ejb2jboss.model;

public record EjbBeanRecord(
        String descriptorFile,
        String ejbName,
        String ejbClass,
        String beanType,
        String sessionType,
        String persistenceType,
        String tableName) {}
