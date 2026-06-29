package com.anchor.migration.javaastssot.profile.mybatis;

import com.anchor.migration.javaastssot.profile.ProfileSnapshot;
import com.anchor.migration.javaastssot.profile.mybatis.model.MyBatisResultFieldRecord;
import com.anchor.migration.javaastssot.profile.mybatis.model.MyBatisResultMapRecord;
import com.anchor.migration.javaastssot.profile.mybatis.model.MyBatisStatementRecord;
import com.anchor.migration.javaastssot.profile.mybatis.model.MyBatisStatementTableRecord;

import java.util.ArrayList;
import java.util.List;

public final class MyBatisSnapshot implements ProfileSnapshot {
    public final List<MyBatisResultMapRecord> resultMaps = new ArrayList<>();
    public final List<MyBatisResultFieldRecord> resultFields = new ArrayList<>();
    public final List<MyBatisStatementRecord> statements = new ArrayList<>();
    public final List<MyBatisStatementTableRecord> statementTables = new ArrayList<>();
}
