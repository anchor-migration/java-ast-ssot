package com.anchor.migration.javaastssot.profile.javaee.ejb2jboss;

public final class JavaEeEjb2JbossIds {
    public static final String PROFILE_ID = "javaee-ejb2-jboss";
    public static final String DEFAULT_DB_SCHEMA = "dukesbank";

    private JavaEeEjb2JbossIds() {}

    public static String ejbBean(String ejbName) {
        return "ejb:" + ejbName;
    }

    public static String dbTable(String schema, String table) {
        return schema + "." + table;
    }
}
