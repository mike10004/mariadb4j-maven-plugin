package com.github.mike10004.mariadb4jmavenplugin;

import ch.vorburger.mariadb4j.DB;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Table;
import org.apache.maven.plugin.testing.MojoRule;
import org.junit.Rule;
import org.junit.Test;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.checkState;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

public class Mariadb4jStartMojoTest {

    @Rule
    public MojoRule mojoRule = new MojoRule();

    private static final String BASIC_TABLE_NAME = "bar";
    private static final String BASIC_DB_NAME = "foo";
    @SuppressWarnings({"SqlDialectInspection", "SqlNoDataSourceInspection"})
    private static final String BASIC_TABLE_INSERT_STMT = "INSERT INTO bar (baz) VALUES (?)";
    private static final String BASIC_TABLE_VALUE_COLUMN = "baz";

    @Test
    public void execute() throws Exception
    {
        File pom = new File(getClass().getResource( "/basic-usage/pom.xml").toURI());
        checkState(pom.isFile(), "not found: %s", pom);
        Mariadb4jStartMojo mojo = (Mariadb4jStartMojo) mojoRule.lookupMojo("start", pom);
        assertNotNull(mojo);
        Map pluginContext = mojo.getPluginContext();
        if (pluginContext == null) {
            mojo.setPluginContext(pluginContext = new HashMap<>());
        }
        mojoRule.configureMojo(mojo, "mariadb4j-maven-plugin", pom);
        String createDatabase = mojo.getCreateDatabase();
        assertEquals("createDatabase(name)", BASIC_DB_NAME, createDatabase);
        mojo.execute();
        DB db = null;
        boolean clean = false;
        try {
            db = (DB) mojo.getPluginContext().get(Mariadb4jStartMojo.CONTEXT_KEY_DB);
            assertNotNull("db from plugin context", db);
            String jdbcUrl = "jdbc:mysql://localhost:" + db.getConfiguration().getPort() + "/" + BASIC_DB_NAME;
            System.out.println("querying database...");
            Map<String, String> vars;
            Table<Integer, String, Object> table;
            try (Connection conn = DriverManager.getConnection(jdbcUrl)) {
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
            clean = true;
        } finally {
            if (db != null) {
                try {
                    System.out.println("stopping database...");
                    db.stop();
                    System.out.println("database stopped");
                } catch (Exception e) {
                    e.printStackTrace(System.err);
                    if (clean) {
                        fail("could not stop database: " + e.toString());
                    }
                }
            }
            checkState(pluginContext == mojo.getPluginContext());
            new Mariadb4jStopMojo().cleanUpTempDirectories(pluginContext);
        }

    }

}