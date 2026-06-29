package com.anchor.migration.javaastssot.profile.jpa;

import com.anchor.migration.javaastssot.profile.ProfileSnapshot;
import com.anchor.migration.javaastssot.profile.jpa.model.JpaEntityRecord;
import com.anchor.migration.javaastssot.profile.jpa.model.JpaFieldRecord;

import java.util.ArrayList;
import java.util.List;

public final class JpaSnapshot implements ProfileSnapshot {
    public final List<JpaEntityRecord> entities = new ArrayList<>();
    public final List<JpaFieldRecord> fields = new ArrayList<>();
}
