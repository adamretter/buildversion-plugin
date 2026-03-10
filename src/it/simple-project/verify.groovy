import java.nio.file.Files
import java.nio.file.Paths

def targetString = "build-version: 1.2.0-SNAPSHOT"

println "*** Checking build log output. Looking for build-version${targetString}"

def build_log = new String(Files.readAllBytes(Paths.get("target/it/simple-project/build.log")), "UTF-8")

return build_log.contains(targetString)
