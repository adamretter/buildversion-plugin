// make this test project a "git" repository by copying the .git directory
// from the sample project used on unit tests

import java.nio.file.Files
import java.nio.file.Paths

if (!Files.isDirectory(Paths.get("target/sample_project/.git"))) {
    throw new FileNotFoundException("Couldn't find .git directory at target/sample_project/.git")
}

if (!Files.isDirectory(Paths.get("target/it/simple-project"))) {
    throw new FileNotFoundException("Couldn't find target directory at target/sample_project")
}

println "*** Copying .git dir into target/it/simple-project"
def p = "cp -r target/sample_project/.git/ target/it/simple-project".execute()
println p.text

println "*** Checking out 'develop' branch"
p = "git --git-dir=target/it/simple-project/.git --work-tree=target/it/simple-project/ checkout -f develop".execute()
println p.text
p.waitFor()

return true
