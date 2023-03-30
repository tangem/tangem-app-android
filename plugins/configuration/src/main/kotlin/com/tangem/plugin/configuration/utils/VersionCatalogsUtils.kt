package com.tangem.plugin.configuration.utils

import org.gradle.api.Project
import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.artifacts.VersionConstraint
import org.gradle.kotlin.dsl.getByType
import org.gradle.plugin.use.PluginDependency
import kotlin.jvm.optionals.getOrNull

private val Project.depsVersionCatalog: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("deps")

internal fun Project.findVersion(alias: String): VersionConstraint {
    return depsVersionCatalog.findVersion(alias).getOrNull()
        ?: error("Unable to find version $alias")
}

internal fun Project.findPlugin(alias: String): PluginDependency {
    return depsVersionCatalog.findPlugin(alias).getOrNull()?.orNull
        ?: error("Unable to find plugin $alias")
}

internal fun Project.findLibrary(alias: String): MinimalExternalModuleDependency {
    return depsVersionCatalog.findLibrary(alias).getOrNull()?.orNull
        ?: error("Unable to find library $alias")
}