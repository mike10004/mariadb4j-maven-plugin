package com.github.mike10004.mariadb4jmavenplugin;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableTable;
import com.google.common.math.IntMath;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;

import static com.google.common.base.Preconditions.checkArgument;

@SuppressWarnings({"SqlDialectInspection", "SqlNoDataSourceInspection"})
class DbUtils {
    private DbUtils() {}

    public static ImmutableMap<String, String> showVariables(Connection conn, String likeness) throws SQLException {
        checkArgument(likeness.matches("[\\w%]*"), "likeness value invalid");
        ImmutableMap.Builder<String, String> b = ImmutableMap.builder();
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SHOW VARIABLES LIKE '" + likeness + "'")) {
            while (rs.next()) {
                b.put(rs.getString(1), rs.getString(2));
            }
        }
        return b.build();
    }

    public static ImmutableTable<Integer, String, Object> selectAll(Connection conn, String table) throws SQLException {
        checkArgument(table.matches("[A-Za-z]\\w+"), "table name invalid");
        ImmutableTable.Builder<Integer, String, Object> b = ImmutableTable.builder();
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM `" + table + "` WHERE 1")) {
            ResultSetMetaData md = rs.getMetaData();
            int row = 0;
            while (rs.next()) {
                for (int column = 1; column <= md.getColumnCount(); column++) {
                    String columnName = md.getColumnName(column);
                    Object value = rs.getObject(column);
                    b.put(row, columnName, value);
                }
                row = IntMath.checkedAdd(row, 1);
            }
        }
        return b.build();
    }
}
