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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

internal fun BaseExtension.configureCompose(project: Project) {
    val useCompose = with(project.path) {
        contains(":ui") ||
            contains(":onboarding") ||
            contains(":presentation") ||
            contains(":app") || // TODO: [REDACTED_JIRA]
            contains(":tester:impl") // TODO: Rename module
    }
    buildFeatures.compose = useCompose
    if (useCompose) {
        composeOptions {
            kotlinCompilerExtensionVersion = project.findVersion(alias = "compose-compiler").requiredVersion
        }
    }
}