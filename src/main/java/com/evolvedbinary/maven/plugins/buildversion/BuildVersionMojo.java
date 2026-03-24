/*
 * Build Version Maven Plugin - This is a maven plugin that extracts current build information from git
    projects, including: the latest commit hash, timestamp, most recent tag,
    number of commits since most recent tag. It also implements a "follow first
    parent" flavor of git describe.
 * Copyright © 2012 Fernando Dobladez
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package com.evolvedbinary.maven.plugins.buildversion;

import com.evolvedbinary.maven.plugins.buildversion.util.LoggerFactory;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import javax.annotation.Nullable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static com.evolvedbinary.maven.plugins.buildversion.GitService.DEFAULT_TIMESTAMP_FORMAT;

@Mojo(name = "set-properties", defaultPhase = LifecyclePhase.INITIALIZE, threadSafe = true)
public class BuildVersionMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    private MavenProject project;

    @Parameter(defaultValue = "${basedir}", required = true, readonly = true)
    private Path baseDir;

    @Parameter(property = "tstampFormat", defaultValue = DEFAULT_TIMESTAMP_FORMAT)
    private String tstampFormat;

    @Parameter(property = "customProperties")
    private String customProperties;

    @Parameter(property = "gitCmd", defaultValue = "git")
    private String gitCmd;

    @Override
    public void execute() throws MojoExecutionException {
        final GitService gitService = new GitService(baseDir, gitCmd, LoggerFactory.fromMavenLog(getLog()));
        gitService.setTimestampFormat(tstampFormat);

        Map<String, String> gitProperties;
        try {
            gitProperties = gitService.inferProjectVersion();
        } catch (final IOException e) {
            throw new MojoExecutionException("I/O error when running git: " + e.getMessage(), e);
        }

        if (customProperties != null && !customProperties.trim().isEmpty()) {
            gitProperties = applyCustomProperties(customProperties, gitProperties);
        }

        final Properties mavenProps = project.getProperties();
        for (final Map.Entry<String, String> entry : gitProperties.entrySet()) {
            getLog().debug("[buildversion-plugin] Setting property: " + entry.getKey() + "=" + entry.getValue());
            mavenProps.setProperty(entry.getKey(), entry.getValue());
        }
    }

    private static Map<String, String> applyCustomProperties(final String customProperties, final Map<String, String> gitProperties) throws MojoExecutionException {
        @Nullable final ScriptEngine engine = new ScriptEngineManager().getEngineByName("groovy");
        if (engine == null) {
            throw new MojoExecutionException("Groovy script engine not found. Ensure groovy-jsr223 is on the classpath.");
        }

        for (final Map.Entry<String, String> entry : gitProperties.entrySet()) {
            final String keyCamelCase = toCamelCase(entry.getKey());
            final String value = entry.getValue();
            engine.put(keyCamelCase, value);
        }

        @Nullable final Object groovyScriptResult;
        try {
            groovyScriptResult = engine.eval(customProperties);
        } catch (final ScriptException e) {
            throw new MojoExecutionException("Error evaluating customProperties groovy script: " + e.getMessage(), e);
        }

        if (groovyScriptResult == null) {
            throw new MojoExecutionException("Error customProperties groovy script must produce a java.util.Map, but received null");
        }

        if (!(groovyScriptResult instanceof Map)) {
            throw new MojoExecutionException("Error customProperties groovy script must produce a java.util.Map, but received: " + groovyScriptResult.getClass().getName());
        }

        @SuppressWarnings("unchecked")
        final Map<Object, Object> groovyScriptResultMap = (Map<Object, Object>) groovyScriptResult;

        final Map<String, String> merged = new HashMap<>(gitProperties);

        for (final Map.Entry<Object, Object> groovyScriptResultMapEntry : groovyScriptResultMap.entrySet()) {
            @Nullable final Object key = groovyScriptResultMapEntry.getKey();
            @Nullable final Object value = groovyScriptResultMapEntry.getValue();

            final String keyString = String.valueOf(key);
            final String valueString = String.valueOf(value);

            merged.put(keyString, valueString);
        }

        return merged;
    }

    private static String toCamelCase(final String s) {
        final String[] parts = s.split("-|\\.");
        final StringBuilder stringBuilder = new StringBuilder(parts[0]);
        for (int i = 1; i < parts.length; i++) {
            final String part = parts[i];
            if (!part.isEmpty()) {
                final char partFirstChar = Character.toUpperCase(part.charAt(0));
                stringBuilder.append(partFirstChar);
                if (part.length() > 1) {
                    final String partRemainingChars = part.substring(1);
                    stringBuilder.append(partRemainingChars);
                }
            }
        }
        return stringBuilder.toString();
    }
}
