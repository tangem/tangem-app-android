package com.tangem.plugin.configuration.configurations

import com.tangem.plugin.configuration.utils.findLibrary
import com.tangem.plugin.configuration.utils.findPlugin
import io.gitlab.arturbosch.detekt.CONFIGURATION_DETEKT
import io.gitlab.arturbosch.detekt.CONFIGURATION_DETEKT_PLUGINS
import io.gitlab.arturbosch.detekt.Detekt
import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType

internal fun Project.configureDetektRules() {
    plugins.apply(findPlugin(alias = CONFIGURATION_DETEKT).pluginId)
    extensions.configure<DetektExtension> { configure(project) }
    configureDetektPlugins()
    configureDetektTask()
}

private fun DetektExtension.configure(project: Project) {
    parallel = true
    ignoreFailures = false
    autoCorrect = true
    buildUponDefaultConfig = true
    config.setFrom(project.rootProject.files("tangem-android-tools/detekt-config.yml"))
}

private fun Project.configureDetektPlugins() {
    listOf(
        findLibrary(alias = "detekt-formatting"),
        findLibrary(alias = "detekt-compose"),
    ).forEach {
        dependencies.add(CONFIGURATION_DETEKT_PLUGINS, it)
    }
}

private fun Project.configureDetektTask() {
    tasks.withType<Detekt> {
        include("**/*.kt")
        exclude("**/resources/**", "**/build/**")
        reports {
            sarif {
                required.set(false)
            }
            txt {
                required.set(true)
            }
        }

        jvmTarget = "17"
    }
}
