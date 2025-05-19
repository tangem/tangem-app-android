package com.tangem.plugin.configuration

import com.android.build.gradle.AppExtension
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.internal.plugins.AppPlugin
import com.android.build.gradle.internal.plugins.LibraryPlugin
import com.tangem.plugin.configuration.configurations.configure
import com.tangem.plugin.configuration.configurations.extension.configure
import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class ConfigurationPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.configure()
        project.plugins.all(startPluginConfigurationAction(project))
    }

    private fun startPluginConfigurationAction(project: Project) = Action<Plugin<*>> {
        when (this) {
            is AppPlugin -> {
                project.configure<AppExtension> { configure(project) }
            }
            is LibraryPlugin -> {
                project.configure<LibraryExtension> { configure(project) }
            }
        }
    }
}