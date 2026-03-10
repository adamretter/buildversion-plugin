// looking for timestamp with custom format yyyy-MM-dd

import java.nio.file.Files
import java.nio.file.Paths

def build_log = new String(Files.readAllBytes(Paths.get("target/it/custom-properties/build.log")), "UTF-8")

boolean tstamp = (build_log =~ /build-tstamp: \d\d\d\d-\d\d-\d\d/)

// expect an all-uppercased git commit hash 
boolean customProperty = ( build_log =~ /build-commit-uppercase: [0-9A-F]{40}/ )

return tstamp && customProperty
