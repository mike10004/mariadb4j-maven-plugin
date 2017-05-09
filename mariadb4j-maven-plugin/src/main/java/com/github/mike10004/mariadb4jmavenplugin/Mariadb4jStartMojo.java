package com.github.mike10004.mariadb4jmavenplugin;

import ch.vorburger.exec.ManagedProcessException;
import ch.vorburger.mariadb4j.DB;
import ch.vorburger.mariadb4j.DBConfiguration;
import ch.vorburger.mariadb4j.DBConfigurationBuilder;
import com.google.common.base.MoreObjects;
import com.google.common.io.Files;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkState;

@SuppressWarnings("unused")
@Mojo(name = "start", defaultPhase= LifecyclePhase.PRE_INTEGRATION_TEST)
public class Mariadb4jStartMojo extends AbstractMojo {

    static final String CONTEXT_KEY_DB = Mariadb4jStartMojo.class.getName() + ".db";

    private static final int UNSET_PORT_VALUE = -1;
    private static final String UNSET_PORT_VALUE_STR = "-1";
    /**
     * Maven project.
     */
    @Parameter(
            defaultValue = "${project}",
            required = true,
            readonly = true
    )
    private transient MavenProject project;

    @Parameter
    private boolean skip;

    @Parameter
    private String[] args;

    @Parameter
    private String baseDir;

    @Parameter
    private String dataDir;

    @Parameter
    private String databaseVersion;

    @Parameter
    private String libDir;

    @Parameter
    private File tempDir;

    @Parameter
    private String os;

    @Parameter(property=Mariadb4jFindFreePortMojo.PROPNAME_PORT, defaultValue=UNSET_PORT_VALUE_STR)
    private int port;

    @Parameter
    private String socket;

    @Parameter(defaultValue="true")
    private boolean unpackingFromClasspath = true;

    @Parameter
    private File[] scripts;

    @Parameter
    private String scriptCharset;

    @Parameter
    private String createDatabase;

    @SuppressWarnings("unchecked")
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        getLog().debug("executing MariaDB4J start mojo");
        if (skip) {
            getLog().debug("skipping start mariadb");
            return;
        }
        Map pluginContext = getPluginContext();
        DB db = null;
        boolean clean = false;
        try {
            db = buildDb(pluginContext);
            db.start();
            if (createDatabase != null) {
                db.createDB(createDatabase);
            }
            runScripts(db, createDatabase);
            int port = db.getConfiguration().getPort();
            clean = true;
            pluginContext.put(CONTEXT_KEY_DB, db);
        } catch (ManagedProcessException | IOException e) {
            throw new MojoFailureException("failed to start or run scripts", e);
        } finally {
            if (db != null && !clean) {
                try {
                    db.stop();
                } catch (ManagedProcessException e) {
                    getLog().info("failed to stop db after failed start attempt", e);
                }
            }
        }
    }

    private Charset getScriptCharset() {
        return Optional.ofNullable(scriptCharset).map(Charset::forName).orElse(StandardCharsets.UTF_8);
    }

    protected void runScripts(DB db, @Nullable String dbName) throws ManagedProcessException, IOException, MojoExecutionException {
        File[] scripts = this.scripts;
        if (scripts != null) {
            Charset charset = getScriptCharset();
            for (File scriptFile : scripts) {
                String scriptText = Files.toString(scriptFile, charset);
                db.run(scriptText, null, null, dbName);
            }
        }
    }

    protected DB buildDb(Map pluginContext) throws ManagedProcessException, IOException {
        return DB.newEmbeddedDB(buildDbConfiguration(pluginContext));
    }

    private File getDefaultTempParent() {
        return MoreObjects.firstNonNull(tempDir, new File(System.getProperty("java.io.tmpdir")));
    }

    private File getTempParent() {
        if (tempDir == null) {
            tempDir = getDefaultTempParent();
        }
        return tempDir;
    }

    enum SupportDir {
        base, data, lib;

        public String toPrefix() {
            return "mariadb4j-" + name();
        }
    }

    @SuppressWarnings("unchecked")
    private File getOrCreateSupportDirectory(@Nullable String pathname, SupportDir supportDir, Map pluginContext) throws IOException {
        if (pathname == null) {
            File tempParent = getTempParent();
            if (!tempParent.isDirectory()) {
                if (!tempParent.mkdirs()) {
                    getLog().warn("failed to create directory " + tempParent);
                }
            }
            if (!tempParent.isDirectory()) {
                throw new IOException("not directory and could not be created: " + tempParent);
            }
            Path dir = java.nio.file.Files.createTempDirectory(tempParent.toPath(), supportDir.toPrefix());
            pluginContext.put(supportDir, dir);
            return dir.toFile();
        } else {
            return new File(pathname);
        }
    }

    protected DBConfiguration buildDbConfiguration(Map pluginContext) throws IOException {
        DBConfigurationBuilder b = DBConfigurationBuilder.newBuilder();
        if (args != null) {
            for (String arg : args) {
                b.addArg(arg);
            }
        }
        b.setBaseDir(getOrCreateSupportDirectory(baseDir, SupportDir.base, pluginContext).getAbsolutePath());
        b.setDataDir(getOrCreateSupportDirectory(dataDir, SupportDir.data, pluginContext).getAbsolutePath());
        if (databaseVersion != null) {
            b.setDatabaseVersion(databaseVersion);
        }
        b.setLibDir(getOrCreateSupportDirectory(libDir, SupportDir.lib, pluginContext).getAbsolutePath());
        if (os != null) {
            b.setOS(os);
        }
        checkState(port != UNSET_PORT_VALUE, "port must be set to value >= 0; " +
                "this can be done by running the 'port' goal, " +
                "specifying a known value for the 'mariadb4j.port' system property, " +
                "or specifying <port> in the configuration; " +
                "actual port value on this run is %s", port);
        b.setPort(port);
        if (socket != null) {
            b.setSocket(socket);
        }
        b.setUnpackingFromClasspath(unpackingFromClasspath);
        return b.build();
    }

    public String[] getArgs() {
        return args;
    }

    public void setArgs(String[] args) {
        this.args = args;
    }

    public String getBaseDir() {
        return baseDir;
    }

    public void setBaseDir(String baseDir) {
        this.baseDir = baseDir;
    }

    public String getDataDir() {
        return dataDir;
    }

    public void setDataDir(String dataDir) {
        this.dataDir = dataDir;
    }

    public String getDatabaseVersion() {
        return databaseVersion;
    }

    public void setDatabaseVersion(String databaseVersion) {
        this.databaseVersion = databaseVersion;
    }

    public String getLibDir() {
        return libDir;
    }

    public void setLibDir(String libDir) {
        this.libDir = libDir;
    }

    public String getOs() {
        return os;
    }

    public void setOs(String os) {
        this.os = os;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getSocket() {
        return socket;
    }

    public void setSocket(String socket) {
        this.socket = socket;
    }

    public boolean isUnpackingFromClasspath() {
        return unpackingFromClasspath;
    }

    public void setUnpackingFromClasspath(boolean unpackingFromClasspath) {
        this.unpackingFromClasspath = unpackingFromClasspath;
    }

    public File[] getScripts() {
        return scripts;
    }

    public void setScripts(File[] scripts) {
        this.scripts = scripts;
    }

    public void setScriptCharset(String scriptCharset) {
        this.scriptCharset = scriptCharset;
    }

    public String getCreateDatabase() {
        return createDatabase;
    }

    public void setCreateDatabase(String createDatabase) {
        this.createDatabase = createDatabase;
    }

    static {
        org.apache.commons.io.ByteOrderMark.class.getName();
    }

    public MavenProject getProject() {
        return project;
    }

    public void setProject(MavenProject project) {
        this.project = project;
    }

    public File getTempDir() {
        return tempDir;
    }

    public void setTempDir(File tempDir) {
        this.tempDir = tempDir;
    }

    public boolean isSkip() {
        return skip;
    }

    public void setSkip(boolean skip) {
        this.skip = skip;
    }
}
