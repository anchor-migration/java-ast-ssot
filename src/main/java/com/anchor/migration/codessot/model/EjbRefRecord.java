package com.anchor.migration.codessot.model;

public record EjbRefRecord(
        String sourceEjbName, String refName, String refType, String ejbLink, String refKind) {}
