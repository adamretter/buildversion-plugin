// looking for usage of "git.sh", because we specified that as the git command to use

import java.nio.file.Files
import java.nio.file.Paths

def build_log = new String(Files.readAllBytes(Paths.get("target/it/setting-git-cmd/build.log")), "UTF-8")

return build_log.contains("git.sh")
