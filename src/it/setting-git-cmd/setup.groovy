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
// make git.sh wrapper  executable.
// Need to do this because maven-invoker-plugin's cloneProjectsTo doesn't seem to preserve
// permissions
println "*** Ensure git.sh is executable"
def p = "chmod +x target/it/setting-git-cmd/git.sh".execute()
p.waitFor()

return true
