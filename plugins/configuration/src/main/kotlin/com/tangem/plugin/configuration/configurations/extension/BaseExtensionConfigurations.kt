package com.tangem.plugin.configuration.configurations.extension

import com.android.build.gradle.BaseExtension
import com.tangem.plugin.configuration.model.AppConfig
import com.tangem.plugin.configuration.utils.findVersion
import org.gradle.api.JavaVersion
import org.gradle.api.Project

internal fun BaseExtension.configureCompileSdk() {
    compileSdkVersion(AppConfig.compileSdkVersion)
}

internal fun BaseExtension.configureCompilerOptions() {
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

internal fun BaseExtension.configureCompose(project: Project) {
    val useCompose = with(project.path) {
        contains(":ui") ||
            contains(":onboarding") || // TODO: divide on api/impl after migrating all onboarding to module
            contains(":presentation") ||
            contains(":app") || // TODO: https://tangem.atlassian.net/browse/AND-3190
            contains(":impl")
    }
    buildFeatures.compose = useCompose
    if (useCompose) {
        composeOptions {
            kotlinCompilerExtensionVersion = project.findVersion(alias = "compose-compiler").requiredVersion
        }
    }
}
