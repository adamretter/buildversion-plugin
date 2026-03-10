package com.code54.mojo.buildversion;

import com.code54.mojo.buildversion.util.Logger;
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
