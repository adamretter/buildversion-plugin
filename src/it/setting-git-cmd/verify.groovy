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
// looking for usage of "git.sh", because we specified that as the git command to use

import java.nio.file.Files
import java.nio.file.Paths

def build_log = new String(Files.readAllBytes(Paths.get("target/it/setting-git-cmd/build.log")), "UTF-8")

return build_log.contains("git.sh")
