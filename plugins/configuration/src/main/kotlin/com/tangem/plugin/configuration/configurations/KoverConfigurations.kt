package com.tangem.plugin.configuration.configurations

import com.tangem.plugin.configuration.utils.findPlugin
import org.gradle.api.Project

internal fun Project.configureKover() {
    plugins.apply(findPlugin(alias = "kover").pluginId)
}