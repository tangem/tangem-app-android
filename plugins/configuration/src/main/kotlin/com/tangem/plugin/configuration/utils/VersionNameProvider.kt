package com.tangem.plugin.configuration.utils

import org.gradle.api.Project
import org.gradle.api.provider.Provider

/**
 * Provides version name based on current git branch
 */
internal class VersionNameProvider(
    private val project: Project,
) {

    /**
     * Get version name for current branch.
     * If -PversionName is provided, uses that value.
     * Otherwise, derives version from git branch name.
     */
    fun getVersionName(): String {
        // Check if versionName is provided as gradle property
        if (project.hasProperty("versionName")) {
            return project.property("versionName") as String
        }

        // Get current branch name using Provider API (configuration cache compatible)
        val currentBranch = getCurrentBranchProvider().get()

        // Try to extract version from branch name (releases/X.Y)
        val versionFromBranch = extractVersionFromBranch(currentBranch)
        if (versionFromBranch != null) {
            return versionFromBranch
        }

        // For other branches (develop, feature/*), find latest release branch and increment minor
        val latestReleaseBranch = findLatestReleaseBranch()
        if (latestReleaseBranch != null) {
            val versionFromLatest = extractVersionFromBranch(latestReleaseBranch)
            if (versionFromLatest != null) {
                return incrementMinorVersion(versionFromLatest)
            }
        }

        // Fallback to default if nothing works
        return "1.0.0-SNAPSHOT"
    }

    private fun getCurrentBranchProvider(): Provider<String> {
        return project.providers.exec {
            commandLine("git", "rev-parse", "--abbrev-ref", "HEAD")
        }.standardOutput.asText.map { it.trim() }
    }

    private fun findLatestReleaseBranch(): String? {
        val scriptPath = project.rootProject.file("tangem-android-tools/CI/shell_scripts/find-latest-release-branch.sh")
        if (!scriptPath.exists()) {
            project.logger.warn("Script not found: $scriptPath")
            return null
        }

        val currentBranch = getCurrentBranchProvider().get()
        val outputFile = project.rootProject.file("find-latest-release-branch.output")

        return try {
            project.providers.exec {
                commandLine("sh", scriptPath.absolutePath, currentBranch)
            }.standardOutput.asText.get()

            if (!outputFile.exists()) {
                project.logger.warn("Script output file not found")
                return null
            }

            outputFile.readText().trim().also { result ->
                project.logger.lifecycle("Found latest release branch: $result")
                outputFile.delete()
            }
        } catch (e: Exception) {
            project.logger.warn("Failed to execute script: ${e.message}")
            outputFile.delete()
            null
        }
    }

    private fun extractVersionFromBranch(branch: String): String? {
        val regex = Regex("""^releases/(\d+)\.(\d+)(?:\.(\d+))?$""")
        val matchResult = regex.find(branch) ?: return null

        val major = matchResult.groupValues[1]
        val minor = matchResult.groupValues[2]
        val patch = matchResult.groupValues[3]

        return if (patch.isEmpty()) "$major.$minor" else "$major.$minor.$patch"
    }

    private fun incrementMinorVersion(version: String): String {
        val parts = version.split(".")
        if (parts.size < 2) return version

        val major = parts[0]
        val minor = parts[1].toIntOrNull() ?: return version
        val newMinor = minor + 1

        return "$major.$newMinor"
    }

}