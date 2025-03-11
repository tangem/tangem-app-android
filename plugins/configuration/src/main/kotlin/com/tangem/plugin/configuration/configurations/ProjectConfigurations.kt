package com.tangem.plugin.configuration.configurations

import org.gradle.api.Project

internal fun Project.configure() {
    configureKotlinCompilerOptions()
    configureDetektRules()
}