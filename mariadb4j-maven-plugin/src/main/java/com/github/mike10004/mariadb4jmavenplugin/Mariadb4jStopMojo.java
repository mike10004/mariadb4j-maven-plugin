/*
 * (c) 2017 Novetta
 *
 * Created by mike
 */
package com.github.mike10004.mariadb4jmavenplugin;

import com.github.mike10004.mariadb4jmavenplugin.Mariadb4jStartMojo.Widget;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

@Mojo(name="stop", defaultPhase= LifecyclePhase.POST_INTEGRATION_TEST)
public class Mariadb4jStopMojo extends AbstractMojo {
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        Object widget = getPluginContext().get("widget");
        if (widget == null) {
            throw new MojoFailureException("widget not present in plugin context");
        }
        getLog().info("widget is " + ((Widget)widget).color);
    }
}
