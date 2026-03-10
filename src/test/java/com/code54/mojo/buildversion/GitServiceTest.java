package com.code54.mojo.buildversion;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class GitServiceTest {

    private static final String MAVEN_TARGET_DIR = System.getProperty("buildversion.maven-target-dir", System.getProperty("java.io.tmpdir", "/tmp"));
    private static final String MAVEN_BASH_SOURCE_DIR = System.getProperty("buildversion.maven-bash-source-dir", "src/test/bash");

    private static Path sampleProjectDir;

    private static final Logger LOGGER = LoggerFactory.getLogger(GitServiceTest.class);

    @BeforeAll
    static void setUpSampleProject() throws IOException, InterruptedException {
        LOGGER.info("buildversion tests. Using maven-target-dir: " + MAVEN_TARGET_DIR);
        LOGGER.info("buildversion tests. Using maven-bash-source-dir: " + MAVEN_BASH_SOURCE_DIR);

        sampleProjectDir = Paths.get(MAVEN_TARGET_DIR).resolve("sample_project");

        if (Files.isDirectory(sampleProjectDir.resolve(".git"))) {
            LOGGER.info("Example GIT project dir found... re-using it");
        } else {
            LOGGER.info("Building example GIT project for testing...");
            final Path script = Paths.get(MAVEN_BASH_SOURCE_DIR).resolve("create-sample-git-project.sh");
            final ProcessBuilder pb = new ProcessBuilder(script.toAbsolutePath().toString(), sampleProjectDir.toAbsolutePath().toString());
            pb.redirectErrorStream(true);
            final Process process = pb.start();
            final int exitCode = process.waitFor();
            assertEquals(0, exitCode, "create-sample-git-project.sh failed");
        }
    }

    @Test
    void testRunGit() throws IOException {
        final GitService gitService = new GitService(sampleProjectDir, "git", new SLF4JLogAdapter());
        final List<String> output = gitService.runGit("--version");
        assertEquals(1, output.size());
        assertTrue(output.get(0).matches("(?s).*git version \\d+\\..*"), "Expected git version output but got: " + output);
    }

    @Test
    void testRunGitInvalidPath() {
        final GitService invalidGitService = new GitService(sampleProjectDir, "/invalid/path/to/git", new SLF4JLogAdapter());
        final IOException ex = assertThrows(IOException.class, () ->
            invalidGitService.runGit("--version")
        );
        assertTrue(ex.getMessage().contains("/invalid/path/to/git"), "Expected path in message but got: " + ex.getMessage());
    }

    @Test
    void testRunGitInvalidArguments() {
        final GitService gitService = new GitService(sampleProjectDir, "git", new SLF4JLogAdapter());
        final IOException ex = assertThrows(IOException.class, () ->
            gitService.runGit("--invalid-argument-here")
        );
        assertTrue(ex.getMessage().contains("Git command (git --invalid-argument-here) failed"), "Expected 'Git command failed' in message but got: " + ex.getMessage());
    }

    @Test
    void testGitDescribeLogLines() {
        // Single line, tag at delta 0
        assertDescribeLogLines("v9.9.9", 0,
            "aa44944 (HEAD, tag: v9.9.9, origin/master, master) ...");

        assertDescribeLogLines("v1.11.0", 0,
            "c3bc9ff (tag: v1.11.0) TMS: Add...");

        assertDescribeLogLines("v1.10.0-dev", 0,
            "c3bc9fx (tag: v1.10.0-dev) Blah blah...");

        // Tag found after 2 commits (delta=2)
        assertDescribeLogLines("v1.10.0-dev", 2,
                "aabbccd Dummy commit",
                "bbccddee Dummy commmit2",
                "c3bc9fx (tag: v1.10.0-dev) Blah blah...");

        // Non-versioning tags are skipped; versioning tag at delta 5
        assertDescribeLogLines("v1.10.0-dev", 5,
                "aabbccd Dummy commit",
                "bbccddee Dummy commmit2",
                "bbccddee Dummy commmit2",
                "bbbdddxx (tag: jenkins-myproyect_release-11) Blah blah...",
                "bbccddee Dummy commmit2",
                "c3bc9fx (tag: v1.10.0-dev) Blah blah...");

        // No tags: delta = index of last line
        assertDescribeLogLines(null, 2,
                "aabbccd Dummy commit1",
                "bbccdde Dummy commit2",
                "ccddeef Dymmy commit3");

        // Unexpected output: should not throw
        GitService.describeLogLines(Arrays.asList("unexpected", "output", "from", "git"));
    }

    private void assertDescribeLogLines(final String expectedTag, final int expectedDelta, final String... lines) {
        final List<String> lineList = Arrays.asList(lines);
        final GitService.TagDelta result = GitService.describeLogLines(lineList);
        assertEquals(expectedTag, result.tag, "tag mismatch for lines: " + lineList);
        assertEquals(expectedDelta, result.delta, "delta mismatch for lines: " + lineList);
    }

    @Test
    void testInferProjectVersions() throws IOException {
        assertForCommit("First tagged commit",
                "build-version", "^1.0.0-SNAPSHOT$",
                "build-tag", "^1.0.0-SNAPSHOT$",
                "build-tag-delta", "0",
                "build-tstamp", "\\d+",
                "build-commit", "[a-f\\d]+");

        assertForCommit("dev commit 5",
                "build-version", "1.1.0-SNAPSHOT-3.*",
                "build-tag", "^1.1.0-SNAPSHOT$",
                "build-tag-delta", "3",
                "build-tstamp", "\\d+",
                "build-commit", "[a-f\\d]+");

        assertForCommit("dev commit 1",
                "build-version", "^1.0.0-SNAPSHOT-1.*",
                "build-tag", "^1.0.0-SNAPSHOT",
                "build-tag-delta", "1",
                "build-tstamp", "\\d+",
                "build-commit", "[a-f\\d]+");

        assertForCommit("Initial commit. Before any tag",
                "build-version", "N/A",
                "build-tag", "N/A",
                "build-tag-delta", "0",
                "build-tstamp", "\\d+",
                "build-commit", "[a-f\\d]+");
    }

    @Test
    void testTstampFormatOption() throws IOException {
        final GitService gitService = new GitService(sampleProjectDir, "git", new SLF4JLogAdapter());
        gitService.setTimestampFormat("SSS");

        final Map<String, String> props = gitService.inferProjectVersion();
        final String buildTimestamp = props.get("build-tstamp");
        assertTrue(buildTimestamp.contains("000"), "Expected build-tstamp to contain '000' (git has second precision) but got: " + buildTimestamp);
    }

    private void assertForCommit(final String commitMsg, final String... keyPatterns) throws IOException {
        final GitService gitService = new GitService(sampleProjectDir, "git", new SLF4JLogAdapter());
        final List<String> commitHash = gitService.runGit("log", "--all", "--format=format:%H", "--grep=^" + commitMsg + "$");
        assertFalse(commitHash.isEmpty(), "Couldn't find hash for commit: " + commitMsg);

        gitService.runGit("checkout", commitHash.get(0));

        gitService.setTimestampFormat("yyyyMMddHHmmss");
        final Map<String, String> props = gitService.inferProjectVersion();

        for (int i = 0; i < keyPatterns.length; i += 2) {
            final String key = keyPatterns[i];
            final String pattern = keyPatterns[i + 1];
            final String value = props.get(key);
            assertNotNull(value, "Property '" + key + "' not found for commit '" + commitMsg + "'");
            assertTrue(Pattern.compile(pattern).matcher(value).matches(), "Testing " + key + " for commit '" + commitMsg + "': expected pattern '" + pattern + "' but got '" + value + "'");
        }
    }
}
