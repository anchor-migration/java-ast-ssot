package com.anchor.migration.javaastssot.profile.javaee.ejb2jboss.model;

public record EjbRefRecord(
        String sourceEjbName, String refName, String refType, String ejbLink, String refKind) {}
