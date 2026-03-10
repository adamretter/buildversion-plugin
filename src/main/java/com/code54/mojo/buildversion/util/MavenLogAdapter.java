package com.code54.mojo.buildversion.util;

import org.apache.maven.plugin.logging.Log;

public class MavenLogAdapter implements Logger {

    private final Log mavenLog;

    MavenLogAdapter(final Log mavenLog) {
        this.mavenLog = mavenLog;
    }

    @Override
    public void debug(final String message) {
        mavenLog.debug(message);
    }

    @Override
    public void debug(final String message, final Throwable error) {
        mavenLog.debug(message, error);
    }
}
