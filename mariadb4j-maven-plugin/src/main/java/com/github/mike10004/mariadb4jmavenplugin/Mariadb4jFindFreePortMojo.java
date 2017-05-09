package com.github.mike10004.mariadb4jmavenplugin;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.IOException;
import java.net.ServerSocket;

@Mojo(name = "port", defaultPhase = LifecyclePhase.VALIDATE)
public class Mariadb4jFindFreePortMojo extends AbstractMojo {

    public static final String PROPNAME_PORT = "mariadb4j.port";

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

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (skip) {
            getLog().debug("skipping finding free port");
            return;
        }
        int port;
        try {
            port = findFreePort();
        } catch (IOException e) {
            throw new MojoExecutionException("failed to open socket to find free port", e);
        }
        getLog().info("found free port " + port);
        project.getProperties().setProperty(PROPNAME_PORT, String.valueOf(port));
    }

    protected static int findFreePort() throws IOException {
        ServerSocket ss = new ServerSocket(0);
        int port = ss.getLocalPort();
        ss.setReuseAddress(true);
        ss.close();
        return port;
    }

    public MavenProject getProject() {
        return project;
    }

    public void setProject(MavenProject project) {
        this.project = project;
    }

    public boolean isSkip() {
        return skip;
    }

    public void setSkip(boolean skip) {
        this.skip = skip;
    }
}
