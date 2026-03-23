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
package com.code54.mojo.buildversion;

import com.code54.mojo.buildversion.util.Logger;

import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class GitService {

    public static final String DEFAULT_TIMESTAMP_FORMAT = "yyyyMMddHHmmss";
    private static final Pattern DESCRIBE_LOG_LINES_PATTERN = Pattern.compile("^(\\w+) .*tag: (v[^),]+).*");
    private static final String EOL = System.getProperty("line.separator");

    private final Path folder;
    private final String gitCommand;
    private final Logger logger;
    @Nullable private String timestampFormat;

    public GitService(final Path folder, final String gitCommand, final Logger logger) {
        this.folder = folder;
        this.gitCommand = gitCommand;
        this.logger = logger;
    }

    public void setTimestampFormat(@Nullable final String timestampFormat) {
        if (timestampFormat == null || timestampFormat.trim().isEmpty()) {
            this.timestampFormat = null;
        } else {
            this.timestampFormat = timestampFormat;
        }
    }

    public String getTimestampFormat() {
        if (timestampFormat != null) {
            return timestampFormat;
        }

        return DEFAULT_TIMESTAMP_FORMAT;
    }

    List<String> runGit(final String... args) throws IOException {
        final ExecutorService executorService = Executors.newFixedThreadPool(2, r -> {
            final Thread t = new Thread(r);
            t.setDaemon(true);
            return t;
        });

        final List<String> command = new ArrayList<>();
        command.add(gitCommand);
        Collections.addAll(command, args);

        logger.debug("Running cmd: " + String.join(" ", command));

        final ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(folder.toFile());

        final Process process;
        try {
            process = pb.start();
        } catch (final IOException e) {
            throw new IOException("Failed to start git command: " + toCommandString(command) + ". " + e.getMessage(), e);
        }

        try (final InputStream isStdError = process.getErrorStream();
             final InputStream isStdOut = process.getInputStream()) {

            final InputStreamHandler stdErrHandler = new InputStreamHandler(isStdError);
            final InputStreamHandler stdOutHandler = new InputStreamHandler(isStdOut);

            final Future<List<String>> stdErrFuture = executorService.submit(stdErrHandler);
            final Future<List<String>> stdOutFuture = executorService.submit(stdOutHandler);

            final int exitCode = process.waitFor();
            executorService.shutdownNow();

            final boolean terminatedOkay = executorService.awaitTermination(5, TimeUnit.SECONDS);
            if (!terminatedOkay) {
                throw new IOException("Git command (" + toCommandString(command) + ") failed stream termination with exitCode: " + exitCode);
            }

            try {
                if (exitCode != 0) {
                    final String stdErr = stdErrFuture.get().stream().collect(Collectors.joining(EOL));
                    throw new IOException("Git command (" + toCommandString(command) + ") failed with exit code: " + exitCode + ". " + stdErr);
                }

                return stdOutFuture.get();

            } catch (final ExecutionException e) {
                throw new IOException("Git command (" + toCommandString(command) + ") failed with exit code: " + exitCode + ". " + e.getMessage(), e);
            }

        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();  // restore interrupted flag
            throw new IOException("Git command (" + toCommandString(command) + ") interrupted when running git: " + e.getMessage(), e);

        }
    }

    static TagDelta describeLogLines(final List<String> lines) {
        @Nullable Matcher matcher = null;
        for (int i = 0; i < lines.size(); i++) {
            final String line = lines.get(i);

            if (matcher == null) {
                matcher = DESCRIBE_LOG_LINES_PATTERN.matcher(line);
            } else {
                matcher.reset(line);
            }

            if (matcher.find()) {
                return new TagDelta(matcher.group(2), i);
            }

            if (i == lines.size() - 1) {
                return new TagDelta(null, i);
            }
        }

        return new TagDelta(null, 0);
    }

    private TagDelta describeFirstParent() throws IOException {
        final List<String> outputLines = runGit("log", "--oneline", "--decorate=short", "--first-parent");
        return describeLogLines(outputLines);
    }

    public Map<String, String> inferProjectVersion() throws IOException {
        List<String> outputLines = runGit("log", "-n", "1", "--format=%ct");
        final String ctStr = outputLines.get(0);
        final long epochMillis = Long.parseLong(ctStr) * 1000L;
        final Date commitDate = new Date(epochMillis);
        final String commitTimestamp = new SimpleDateFormat(getTimestampFormat()).format(commitDate);

        outputLines = runGit("log", "-n", "1", "--format=%h %H");
        final String hashLine = outputLines.get(0);
        final String[] hashLineParts = hashLine.split("\\s+");
        final String shortHash = hashLineParts[0];
        final String longHash = hashLineParts[1];

        final Map<String, String> gitProperties = new HashMap<>();
        gitProperties.put("build-tstamp", commitTimestamp);
        gitProperties.put("build-commit", longHash);
        gitProperties.put("build-commit-abbrev", shortHash);

        final TagDelta td = describeFirstParent();
        if (td.tag != null) {
            final String version = td.tag.replaceFirst("^v", "");
            gitProperties.put("build-tag", version);
            gitProperties.put("build-tag-delta", String.valueOf(td.delta));

            if (td.delta == 0) {
                gitProperties.put("build-version", version);
            } else {
                gitProperties.put("build-version", version + "-" + td.delta + "-" + shortHash);
            }

        } else {
            gitProperties.put("build-tag", "N/A");
            gitProperties.put("build-tag-delta", "0");
            gitProperties.put("build-version", "N/A");
        }

        return gitProperties;
    }

    private static String toCommandString(final List<String> command) {
        final StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < command.size(); i++) {
            if (i > 0) {
                stringBuilder.append(' ');
            }
            stringBuilder.append(command.get(i));
        }
        return stringBuilder.toString();
    }

    private static class InputStreamHandler implements Callable<List<String>> {
        private final InputStream inputStream;
        private final AtomicReference<IOException> readStreamException = new AtomicReference<>();

        public InputStreamHandler(final InputStream inputStream) {
            this.inputStream = inputStream;
        }

        @Override
        public List<String> call() {
            final List<String> lines = new ArrayList<>();

            try (final BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    lines.add(line);
                }
            } catch (final IOException e) {
                this.readStreamException.set(e);
            }

            return lines;
        }
    }

    static class TagDelta {
        @Nullable final String tag;
        final int delta;

        public TagDelta(@Nullable final String tag, final int delta) {
            this.tag = tag;
            this.delta = delta;
        }
    }
}
