package com.code54.mojo.buildversion.util;

import org.apache.maven.plugin.logging.Log;

public interface LoggerFactory {

    static Logger fromMavenLog(final Log log) {
        return new MavenLogAdapter(log);
    }
}
