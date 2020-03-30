#!/usr/bin/env kscript
/**
 * Copyright (C) 2013-2020 Lars Erik Röjerås
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
import java.io.File
import java.nio.file.DirectoryIteratorException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.system.exitProcess
import java.time.LocalDateTime

//INCLUDE ./LeoLib.kts

// -------------------------------------------------------------------------------------------
// Main program
// -------------------------------------------------------------------------------------------
/**
 * todo:
 * To push a docker image to nogui registry:
 * 1. Mandatory paramters; git repo and git semver tag on the form vM.m.p
 * 2. Check out the branch and and tag and verify that there are no more commits after the git tag
 * 3. git clean
 * 4. Build
 * 5. Add the following as docker tag: branch-semver-commit hash
 */
Largument.initialise(
    """
    This script builds a hippo frontend in a Docker image
    It must be run from the base dir in the git project
    """.trimIndent()
)

Largument("clean", "Do a gradle clean before the build", false)
//Largument("environment", "Specify 'qa' | 'prod'", true, "environment")
Largument("nogradle", "Do NOT run gradle before the docker build", false)
Largument("push", "Push image to NoGui docker registry", false)
Largument("run", "Run the docker image", false)
Largument("help", "Show this help information", false)

Largument.parse(args)

if (Largument.isSet("help")) Largument.showUsageAndExit("")

// -------------------------------------------------------------------------------------------
val gitBranch = lExec("git rev-parse --abbrev-ref HEAD")
val gitHash =
    lExec("git rev-parse --short HEAD") // Short version. Long can be reconstructed with the rev-parse command.

val imageBaseTag = "tpinfo-kvfrontend:$gitBranch-$gitHash"
val localImageTag = "rojeras/$imageBaseTag"
val noguiImageTag = "docker-registry.centrera.se:443/sll-tpinfo/$imageBaseTag"

val buildDirName = "build/libs"
val buildName = "showcase-1.0.0-SNAPSHOT"
val zipDirName = "$buildDirName/$buildName"
val buildZipFile = "$buildDirName/$buildName.zip"
val indexHtmlFile = "$zipDirName/index.html"

val currentDir = lPwd()

val statusMsg: String = lExec("git status -s") as String
val isCommitted = statusMsg.isEmpty()

val dateTime = LocalDateTime.now()

val versionInfo = """
    <!--
    Build information
    -----------------
    Build time: $dateTime
    Git branch: $gitBranch
    Git commit: $gitHash 
    -->
""".trimIndent()


// -------------------------------------------------------------------------------------------
if (Largument.isSet("clean")) lExec("./gradlew clean")

if (!Largument.isSet("nogradle")) lExec("./gradlew zip")
File(zipDirName).walkBottomUp().forEach {
    lExec("rm $it", quiet = false)
}
lExec("unzip -d $zipDirName $buildZipFile")

File("versionInfo.txt").writeText(versionInfo)
File(indexHtmlFile).appendText(versionInfo)

lExec("docker build --rm -t $localImageTag .", quiet = true)

// Do not tag and push if there are uncomitted changes - use "git status -s" and check no output

if (Largument.isSet("push")) {
    if (isCommitted) {
        lExec("docker tag $localImageTag $noguiImageTag")
        lExec("docker push $noguiImageTag")
    } else println("Branch '$gitBranch' is not committed - the image will NOT be uploaded")
}

if (Largument.isSet("run")) {
    lExec("docker run -d -p 8888:80 $localImageTag")
    println("The image is running and listen to port 8888")
}

exitProcess(0)