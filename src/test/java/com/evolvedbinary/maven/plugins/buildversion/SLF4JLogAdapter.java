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

import com.evolvedbinary.maven.plugins.buildversion.util.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.Util;

public class SLF4JLogAdapter implements Logger {

    private final org.slf4j.Logger logger;

    public SLF4JLogAdapter() {
        this.logger = LoggerFactory.getLogger(Util.getCallingClass());
    }

    @Override
    public void debug(final String message) {
        this.logger.debug(message);
    }

    @Override
    public void debug(final String message, final Throwable error) {
        this.logger.debug(message, error);
    }
}
