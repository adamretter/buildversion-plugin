package com.code54.mojo.buildversion;

import org.apache.maven.plugin.logging.Log;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

public class GitServiceTest {

    private static final Log NOOP_LOG = new Log() {
        public boolean isDebugEnabled() { return false; }
        public void debug(CharSequence content) {}
        public void debug(CharSequence content, Throwable error) {}
        public void debug(Throwable error) {}
        public boolean isInfoEnabled() { return true; }
        public void info(CharSequence content) { System.out.println("[INFO] " + content); }
        public void info(CharSequence content, Throwable error) { System.out.println("[INFO] " + content); }
        public void info(Throwable error) { System.out.println("[INFO] " + error); }
        public boolean isWarnEnabled() { return true; }
        public void warn(CharSequence content) { System.err.println("[WARN] " + content); }
        public void warn(CharSequence content, Throwable error) { System.err.println("[WARN] " + content); }
        public void warn(Throwable error) { System.err.println("[WARN] " + error); }
        public boolean isErrorEnabled() { return true; }
        public void error(CharSequence content) { System.err.println("[ERROR] " + content); }
        public void error(CharSequence content, Throwable error) { System.err.println("[ERROR] " + content); }
        public void error(Throwable error) { System.err.println("[ERROR] " + error); }
    };

    private static final String mavenTargetDir =
            System.getProperty("buildversion.maven-target-dir", "/tmp");
    private static final String mavenBashSourceDir =
            System.getProperty("buildversion.maven-bash-source-dir", "src/test/bash");

    private static File sampleProjectDir;
    private static GitService gitService;

    @BeforeAll
    static void setUpSampleProject() throws Exception {
        System.out.println("buildversion tests. Using maven-target-dir: " + mavenTargetDir);
        System.out.println("buildversion tests. Using maven-bash-source-dir: " + mavenBashSourceDir);

        sampleProjectDir = new File(mavenTargetDir, "sample_project");

        if (new File(sampleProjectDir, ".git").isDirectory()) {
            System.out.println("Example GIT project dir found... re-using it");
        } else {
            System.out.println("Building example GIT project for testing...");
            File script = new File(mavenBashSourceDir, "create-sample-git-project.sh");
            ProcessBuilder pb = new ProcessBuilder(script.getCanonicalPath(),
                    sampleProjectDir.getAbsolutePath());
            pb.redirectErrorStream(true);
            Process process = pb.start();
            int exitCode = process.waitFor();
            Assertions.assertEquals(0, exitCode, "create-sample-git-project.sh failed");
        }

        gitService = new GitService(sampleProjectDir, "git", NOOP_LOG);
    }

    @Test
    void testRunGit() throws Exception {
        String output = gitService.runGit("--version");
        assertTrue(output.matches("(?s).*git version \\d+\\..*"),
                "Expected git version output but got: " + output);
    }

    @Test
    void testRunGitInvalidPath() throws Exception {
        GitService invalidGitService = new GitService(sampleProjectDir, "/invalid/path/to/git", NOOP_LOG);
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> invalidGitService.runGit("--version"));
        assertTrue(ex.getMessage().contains("/invalid/path/to/git"),
                "Expected path in message but got: " + ex.getMessage());
    }

    @Test
    void testRunGitInvalidArguments() throws Exception {
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> gitService.runGit("--invalid-argument-here"));
        assertTrue(ex.getMessage().contains("Git command failed"),
                "Expected 'Git command failed' in message but got: " + ex.getMessage());
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

    private void assertDescribeLogLines(String expectedTag, int expectedDelta, String... lines) {
        List<String> lineList = Arrays.asList(lines);
        GitService.TagDelta result = GitService.describeLogLines(lineList);
        assertEquals(expectedTag, result.tag,
                "tag mismatch for lines: " + lineList);
        assertEquals(expectedDelta, result.delta,
                "delta mismatch for lines: " + lineList);
    }

    @Test
    void testInferProjectVersions() throws Exception {
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
    void testTstampFormatOption() throws Exception {
        Map<String, String> props = gitService.inferProjectVersion("SSS");
        String tstamp = props.get("build-tstamp");
        assertTrue(tstamp.contains("000"),
                "Expected build-tstamp to contain '000' (git has second precision) but got: " + tstamp);
    }

    private void assertForCommit(String commitMsg, String... keyPatterns) throws Exception {
        String commitHash = gitService.runGit(
                "log", "--all", "--format=format:%H", "--grep=^" + commitMsg + "$");
        assertFalse(commitHash.isEmpty(), "Couldn't find hash for commit: " + commitMsg);

        gitService.runGit("checkout", commitHash);

        Map<String, String> props = gitService.inferProjectVersion("yyyyMMddHHmmss");

        for (int i = 0; i < keyPatterns.length; i += 2) {
            String key = keyPatterns[i];
            String pattern = keyPatterns[i + 1];
            String value = props.get(key);
            assertNotNull(value, "Property '" + key + "' not found for commit '" + commitMsg + "'");
            assertTrue(Pattern.compile(pattern).matcher(value).matches(),
                    "Testing " + key + " for commit '" + commitMsg
                            + "': expected pattern '" + pattern + "' but got '" + value + "'");
        }
    }
}
