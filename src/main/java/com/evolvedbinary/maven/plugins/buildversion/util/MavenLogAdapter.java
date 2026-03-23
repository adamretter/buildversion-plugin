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
package com.evolvedbinary.maven.plugins.buildversion.util;

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
