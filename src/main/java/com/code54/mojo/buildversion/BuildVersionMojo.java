package com.code54.mojo.buildversion;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

@Mojo(name = "set-properties", defaultPhase = LifecyclePhase.INITIALIZE)
public class BuildVersionMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    private MavenProject project;

    @Parameter(defaultValue = "${basedir}", required = true, readonly = true)
    private File baseDir;

    @Parameter(property = "tstampFormat", defaultValue = "yyyyMMddHHmmss")
    private String tstampFormat;

    @Parameter(property = "customProperties")
    private String customProperties;

    @Parameter(property = "gitCmd", defaultValue = "git")
    private String gitCmd;

    @Override
    public void execute() throws MojoExecutionException {
        try {
            GitService gitService = new GitService(baseDir, gitCmd, getLog());
            Map<String, String> props = gitService.inferProjectVersion(tstampFormat);

            Map<String, String> finalProps = props;
            if (customProperties != null && !customProperties.trim().isEmpty()) {
                finalProps = applyCustomProperties(props, customProperties);
            }

            Properties mavenProps = project.getProperties();
            for (Map.Entry<String, String> entry : finalProps.entrySet()) {
                getLog().debug("[buildversion-plugin] " + entry.getKey() + ": " + entry.getValue());
                mavenProps.setProperty(entry.getKey(), entry.getValue());
            }
        } catch (IOException | InterruptedException e) {
            throw new MojoExecutionException("Error running git: " + e.getMessage(), e);
        }
    }

    private Map<String, String> applyCustomProperties(Map<String, String> props, String script)
            throws MojoExecutionException {
        ScriptEngine engine = new ScriptEngineManager().getEngineByName("groovy");
        if (engine == null) {
            throw new MojoExecutionException(
                    "Groovy script engine not found. Ensure groovy-jsr223 is on the classpath.");
        }

        for (Map.Entry<String, String> entry : props.entrySet()) {
            engine.put(toCamelCase(entry.getKey()), entry.getValue());
        }

        try {
            Object result = engine.eval(script);
            @SuppressWarnings("unchecked")
            Map<String, Object> resultMap = (Map<String, Object>) result;
            Map<String, String> merged = new LinkedHashMap<>(props);
            for (Map.Entry<String, Object> entry : resultMap.entrySet()) {
                merged.put(entry.getKey(), String.valueOf(entry.getValue()));
            }
            return merged;
        } catch (ScriptException e) {
            throw new MojoExecutionException("Error evaluating customProperties script: " + e.getMessage(), e);
        }
    }

    private static String toCamelCase(String s) {
        String[] parts = s.split("-");
        StringBuilder sb = new StringBuilder(parts[0]);
        for (int i = 1; i < parts.length; i++) {
            String part = parts[i];
            if (!part.isEmpty()) {
                sb.append(Character.toUpperCase(part.charAt(0)));
                sb.append(part.substring(1));
            }
        }
        return sb.toString();
    }
}
