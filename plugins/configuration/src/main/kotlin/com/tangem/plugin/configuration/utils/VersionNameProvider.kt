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

        // Resolve the current branch (git, with a CI env fallback for detached checkouts)
        val currentBranch = resolveCurrentBranch()

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

    /**
     * Resolves the current branch name.
     *
     * Prefers git, but CI (e.g. the GitHub Actions Docker build used for UI tests) checks out a
     * detached HEAD, so `git rev-parse --abbrev-ref HEAD` yields "HEAD" (or nothing when git can't
     * read the mounted workspace). In that case fall back to the branch the CI runner already knows:
     * `GITHUB_HEAD_REF` for pull requests, `GITHUB_REF_NAME` for branch/tag builds.
     */
    private fun resolveCurrentBranch(): String {
        val gitBranch = runCatching { gitBranchProvider().get() }
            .getOrDefault("")
            .trim()

        if (gitBranch.isNotEmpty() && gitBranch != DETACHED_HEAD) {
            return gitBranch
        }

        return ciBranchName().ifEmpty { gitBranch }
    }

    private fun gitBranchProvider(): Provider<String> {
        return project.providers.exec {
            commandLine("git", "rev-parse", "--abbrev-ref", "HEAD")
            // Never let a git failure (detached HEAD, dubious ownership in a container, missing
            // binary) abort configuration — fall through to the CI env fallback instead.
            isIgnoreExitValue = true
        }.standardOutput.asText.map { it.trim() }
    }

    private fun ciBranchName(): String {
        val providers = project.providers
        val headRef = providers.environmentVariable("GITHUB_HEAD_REF").orNull?.trim().orEmpty()
        if (headRef.isNotEmpty()) return headRef

        return providers.environmentVariable("GITHUB_REF_NAME").orNull?.trim().orEmpty()
    }

    private fun findLatestReleaseBranch(): String? {
        val scriptPath = project.rootProject.file("tangem-android-tools/CI/shell_scripts/find-latest-release-branch.sh")
        if (!scriptPath.exists()) {
            project.logger.warn("Script not found: $scriptPath")
            return null
        }

        val outputFile = project.rootProject.file("find-latest-release-branch.output")

        return try {
            project.providers.exec {
                // HEAD always resolves to the current commit, even in CI's detached checkout, so the
                // script can compute which release branch this build descends from without relying on
                // a local branch ref existing.
                commandLine("sh", scriptPath.absolutePath, "HEAD")
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

    private companion object {
        const val DETACHED_HEAD = "HEAD"
    }
}