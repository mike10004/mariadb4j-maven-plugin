package com.github.mike10004.mariadb4jmavenplugin;

import ch.vorburger.exec.ManagedProcessException;
import ch.vorburger.mariadb4j.DB;
import com.github.mike10004.mariadb4jmavenplugin.Mariadb4jStartMojo.SupportDir;
import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

import java.nio.file.Path;
import java.util.Map;

@Mojo(name="stop", defaultPhase= LifecyclePhase.POST_INTEGRATION_TEST)
public class Mariadb4jStopMojo extends AbstractMojo {

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        Map pluginContext = getPluginContext();
        DB db = (DB) pluginContext.get(Mariadb4jStartMojo.CONTEXT_KEY_DB);
        if (db != null) {
            getLog().debug("stopping database");
            try {
                db.stop();
            } catch (ManagedProcessException e) {
                getLog().warn("failed to stop database cleanly; a shutdown hook will probably stop it, though", e);
            }
        }
        cleanUpTempDirectories(pluginContext);
    }

    protected void cleanUpTempDirectories(Map pluginContext) {
        pluginContext.keySet().stream().filter(key -> key instanceof SupportDir).forEach(supportDir -> {
            Path supportDirPath = (Path) pluginContext.get(supportDir);
            FileUtils.deleteQuietly(supportDirPath.toFile());
        });
    }
}
