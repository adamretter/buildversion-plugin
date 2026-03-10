package com.code54.mojo.buildversion;

import org.apache.maven.plugin.logging.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GitService {

    private final File workDir;
    private final String gitCmd;
    private final Log log;

    public GitService(File workDir, String gitCmd, Log log) {
        this.workDir = workDir;
        this.gitCmd = gitCmd;
        this.log = log;
    }

    String runGit(String... args) throws IOException, InterruptedException {
        List<String> cmd = new ArrayList<>();
        cmd.add(gitCmd);
        Collections.addAll(cmd, args);

        log.debug("Running cmd: " + String.join(" ", cmd));

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.directory(workDir);

        Process process;
        try {
            process = pb.start();
        } catch (IOException e) {
            throw new RuntimeException("Failed to start git command: " + gitCmd + ": " + e.getMessage(), e);
        }

        StringBuilder stderrSb = new StringBuilder();
        Thread stderrThread = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    stderrSb.append(line).append("\n");
                }
            } catch (IOException ignored) {
            }
        });
        stderrThread.setDaemon(true);
        stderrThread.start();

        StringBuilder stdoutSb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (stdoutSb.length() > 0) stdoutSb.append("\n");
                stdoutSb.append(line);
            }
        }

        int exitCode = process.waitFor();
        stderrThread.join();

        if (exitCode != 0) {
            throw new RuntimeException("Git command failed: " + stderrSb.toString().trim());
        }

        return stdoutSb.toString().trim();
    }

    static class TagDelta {
        final String tag;
        final int delta;

        TagDelta(String tag, int delta) {
            this.tag = tag;
            this.delta = delta;
        }
    }

    static TagDelta describeLogLines(List<String> lines) {
        Pattern pattern = Pattern.compile("^(\\w+) .*tag: (v[^),]+).*");

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            Matcher m = pattern.matcher(line);
            if (m.find()) {
                return new TagDelta(m.group(2), i);
            }
            if (i == lines.size() - 1) {
                return new TagDelta(null, i);
            }
        }
        return new TagDelta(null, 0);
    }

    private TagDelta describeFirstParent() throws IOException, InterruptedException {
        String output = runGit("log", "--oneline", "--decorate=short", "--first-parent");
        List<String> lines = Arrays.asList(output.split("\n"));
        return describeLogLines(lines);
    }

    public Map<String, String> inferProjectVersion(String tstampFormat) throws IOException, InterruptedException {
        String ctStr = runGit("log", "-n", "1", "--format=%ct");
        long epochMillis = Long.parseLong(ctStr) * 1000L;
        Date commitDate = new Date(epochMillis);
        String formatStr = (tstampFormat != null && !tstampFormat.isEmpty()) ? tstampFormat : "yyyyMMddHHmmss";
        String commitTstampStr = new SimpleDateFormat(formatStr).format(commitDate);

        String hashLine = runGit("log", "-n", "1", "--format=%h %H");
        String[] parts = hashLine.split("\\s+");
        String shortHash = parts[0];
        String longHash = parts[1];

        Map<String, String> props = new LinkedHashMap<>();
        props.put("build-tag", "N/A");
        props.put("build-version", "N/A");
        props.put("build-tag-delta", "0");
        props.put("build-tstamp", commitTstampStr);
        props.put("build-commit", longHash);
        props.put("build-commit-abbrev", shortHash);

        TagDelta td = describeFirstParent();

        if (td.tag != null) {
            String version = td.tag.replaceFirst("^v", "");
            props.put("build-tag", version);
            props.put("build-tag-delta", String.valueOf(td.delta));
            if (td.delta == 0) {
                props.put("build-version", version);
            } else {
                props.put("build-version", version + "-" + td.delta + "-" + shortHash);
            }
        }

        return props;
    }
}
