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

import java.nio.file.Files
import java.nio.file.Paths

def targetString = "build-version: 1.2.0-SNAPSHOT"

println "*** Checking build log output. Looking for build-version${targetString}"

def build_log = new String(Files.readAllBytes(Paths.get("target/it/simple-project/build.log")), "UTF-8")

return build_log.contains(targetString)
