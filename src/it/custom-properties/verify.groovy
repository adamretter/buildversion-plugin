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
// looking for timestamp with custom format yyyy-MM-dd

import java.nio.file.Files
import java.nio.file.Paths

def build_log = new String(Files.readAllBytes(Paths.get("target/it/custom-properties/build.log")), "UTF-8")

boolean tstamp = (build_log =~ /build-tstamp: \d\d\d\d-\d\d-\d\d/)

// expect an all-uppercased git commit hash 
boolean customProperty = ( build_log =~ /build-commit-uppercase: [0-9A-F]{40}/ )

return tstamp && customProperty
