/*
 * (c) 2017 Novetta
 *
 * Created by mike
 */
package com.github.mike10004.mariadb4jmavenplugin;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

@Mojo(name = "start", defaultPhase= LifecyclePhase.PRE_INTEGRATION_TEST)
public class Mariadb4jStartMojo extends AbstractMojo {

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        getPluginContext().put("widget", new Widget("blue"));
    }

    static class Widget {

        public final String color;

        private Widget(String color) {
            this.color = color;
        }
    }
}
