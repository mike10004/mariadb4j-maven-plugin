package com.github.mike10004.mariadb4jmavenplugin;

import ch.vorburger.mariadb4j.DB;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Table;
import org.apache.maven.plugin.testing.MojoRule;
import org.junit.Rule;
import org.junit.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class Mariadb4jStartMojoTest {

    private Map pluginContext;

    @Rule
    public MojoRule mojoRule = new MojoRule() {
        @Override
        protected void after() {
            if (pluginContext != null) {
                DB db = (DB) pluginContext.get(Mariadb4jStartMojo.CONTEXT_KEY_DB);
                if (db != null) {
                    try {
                        System.out.println("stopping database...");
                        db.stop();
                        System.out.println("database stopped");
                    } catch (Exception e) {
                        System.err.println("could not stop database");
                        e.printStackTrace(System.err);
                    }
                }
                new Mariadb4jStopMojo().cleanUpTempDirectories(pluginContext);
            }
        }
    };

    private static final String BASIC_TABLE_NAME = "bar";
    private static final String BASIC_DB_NAME = "foo";
    @SuppressWarnings({"SqlDialectInspection", "SqlNoDataSourceInspection"})
    private static final String BASIC_TABLE_INSERT_STMT = "INSERT INTO bar (baz) VALUES (?)";
    private static final String BASIC_TABLE_VALUE_COLUMN = "baz";

    @Test(timeout = 500)
    public void skip() throws Exception {
        File pom = new File(getClass().getResource( "/skip/pom.xml").toURI());
        Mariadb4jStartMojo mojo = (Mariadb4jStartMojo) mojoRule.lookupMojo("start", pom);
        assertNotNull(mojo);
        mojoRule.configureMojo(mojo, "mariadb4j-maven-plugin", pom);
        mojo.execute();
        // without config and plugin context, combined with timeout, this will most certainly fail if not skipped
        assertTrue(mojo.getPluginContext() == null || mojo.getPluginContext().get(Mariadb4jStartMojo.CONTEXT_KEY_DB) == null);
    }

    @Test
    public void basicUsage() throws Exception {
        File pom = new File(getClass().getResource( "/basic-usage/pom.xml").toURI());
        checkState(pom.isFile(), "not found: %s", pom);
        Mariadb4jStartMojo mojo = (Mariadb4jStartMojo) mojoRule.lookupMojo("start", pom);
        assertNotNull(mojo);
        pluginContext = mojo.getPluginContext();
        if (pluginContext == null) {
            mojo.setPluginContext(pluginContext = new HashMap<>());
        }
        mojoRule.configureMojo(mojo, "mariadb4j-maven-plugin", pom);
        String createDatabase = mojo.getCreateDatabase();
        assertEquals("createDatabase(name)", BASIC_DB_NAME, createDatabase);
        mojo.execute();
        DB db = getDbFromPluginContext();
        assertNotNull("db from plugin context", db);
        System.out.println("querying database...");
        Map<String, String> vars;
        Table<Integer, String, Object> table;
        try (Connection conn = openConnection(db, BASIC_DB_NAME)) {
            vars = DbUtils.showVariables(conn, "version");
            System.out.println(vars);
            table = DbUtils.selectAll(conn, BASIC_TABLE_NAME);
            System.out.format("table pre-insertion: %s%n", table);
            try (PreparedStatement stmt = conn.prepareStatement(BASIC_TABLE_INSERT_STMT)) {
                stmt.setString(1, "a");
                stmt.execute();
                stmt.setString(1, "b");
                stmt.execute();
            }
            table = DbUtils.selectAll(conn, BASIC_TABLE_NAME);
            System.out.format("table post-insertion: %s%n", table);
        }
        assertEquals("vars like version", 1, vars.size());
        Set<Object> valueSet = ImmutableSet.copyOf(table.column(BASIC_TABLE_VALUE_COLUMN).values());
        assertEquals("table values", ImmutableSet.of("a", "b"), valueSet);
    }

    private DB getDbFromPluginContext() {
        return (DB) pluginContext.get(Mariadb4jStartMojo.CONTEXT_KEY_DB);
    }

    private Connection openConnection(DB db, String databaseName) throws SQLException {
        checkNotNull(db, "db");
        String jdbcUrl = "jdbc:mysql://localhost:" + db.getConfiguration().getPort() + "/" + databaseName;
        return DriverManager.getConnection(jdbcUrl);
    }

    private static final String UTF8MB4_TEST_DB_NAME = "charset_test";

    @Test
    public void utf8mb4() throws Exception {
        File pom = new File(getClass().getResource( "/utf8mb4/pom.xml").toURI());
        checkState(pom.isFile(), "not found: %s", pom);
        Mariadb4jStartMojo mojo = (Mariadb4jStartMojo) mojoRule.lookupMojo("start", pom);
        assertNotNull(mojo);
        pluginContext = mojo.getPluginContext();
        if (pluginContext == null) {
            mojo.setPluginContext(pluginContext = new HashMap<>());
        }
        mojoRule.configureMojo(mojo, "mariadb4j-maven-plugin", pom);
        mojo.execute();
        byte[] pokerHandBytes = {
                (byte) 0xf0, (byte) 0x9f, (byte) 0x82, (byte) 0xa1,
                (byte) 0xf0, (byte) 0x9f, (byte) 0x82, (byte) 0xa8,
                (byte) 0xf0,(byte) 0x9f, (byte) 0x83, (byte) 0x91,
                (byte) 0xf0, (byte) 0x9f, (byte) 0x83, (byte) 0x98,
                (byte) 0xf0, (byte) 0x9f, (byte) 0x83, (byte) 0x93,
        };
        String complexString = new String(pokerHandBytes, StandardCharsets.UTF_8);
        try (Connection conn = openConnection(getDbFromPluginContext(), UTF8MB4_TEST_DB_NAME);
            PreparedStatement stmt = conn.prepareStatement("INSERT INTO supertext (content) VALUES (?)")) {
            stmt.setString(1, complexString);
            stmt.execute();
        }
        String retrievedValue;
        try (Connection conn = openConnection(getDbFromPluginContext(), UTF8MB4_TEST_DB_NAME);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT content FROM supertext WHERE 1")) {
            checkState(rs.next());
            retrievedValue = rs.getString(1);
        }
        assertEquals("retrieved", complexString, retrievedValue);
    }
}