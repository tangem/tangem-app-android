package com.tangem.plugin.configuration.configurations

import org.gradle.api.Project
import java.io.File

/** Packaged, not compiled — a file under these may legitimately end in `.kt` (`res/raw` takes any name). */
private val NON_SOURCE_DIRECTORIES = setOf("res", "assets", "build")

private const val IGNORED_VARIANTS_PROPERTY = "dependency.analysis.android.ignored.variants"

/**
 * Fails a trimmed dependency-analysis run when a module keeps sources in a variant that run skips.
 *
 * Such a source set compiles in the real build but not in the analysis, so a dependency declared on the
 * shared `implementation`/`api` configuration and used solely from there counts as unused: the plugin
 * advises deleting it, the check turns red, and the build that does compile the code
 * (assembleGoogleInternal, assembleGoogleExternal, bundleGoogleRelease) breaks later, far from the
 * change. The nightly run does not catch it either — there the variant is analysed and the advice
 * degrades into "move it to `internalImplementation`", which `onIncorrectConfiguration` ignores.
 *
 * The directories to check are derived from the variant list the run itself was given, so there is no
 * second copy to keep in step, and ordinary builds — local ones, IDE sync, tests, publishing — never
 * see this check at all: it only speaks up where the wrong advice would be produced.
 *
 * Blind spot: sources attached through `sourceSets.named(...) { srcDir(...) }` rather than living under
 * `src/<variant>` are invisible here. That covers `src/prodDi`, which is attached to every build type
 * except `mocked` and is therefore compiled by an analysed variant anyway.
 */
internal fun Project.verifyNoSourcesInUnanalysedVariants() {
    val unanalysed = providers.gradleProperty(IGNORED_VARIANTS_PROPERTY)
        .getOrElse("")
        .split(",")
        .map(String::trim)
        .filter(String::isNotEmpty)
    if (unanalysed.isEmpty()) return

    val offenders = unanalysed
        .map { layout.projectDirectory.dir("src/$it").asFile }
        .filter(File::isDirectory)
        .flatMap { dir ->
            dir.walkTopDown()
                .onEnter { it == dir || it.name !in NON_SOURCE_DIRECTORIES }
                .filter { it.isFile && (it.extension == "kt" || it.extension == "java") }
                .toList()
        }
    if (offenders.isEmpty()) return

    error(
        "$path has sources in a variant this dependency-analysis run skips: " +
            offenders.joinToString { it.relativeTo(projectDir).invariantSeparatorsPath } +
            ". Any dependency they alone use will be reported as unused. Either move them to src/main " +
            "and vary behaviour through a BuildConfig field or a DI binding, or declare what they need " +
            "on the matching variant configuration — internalImplementation(...) instead of " +
            "implementation(...) — which keeps the declaration out of the analysis too. Resources in " +
            "these source sets are fine and never reported.",
    )
}