package com.tangem.plugin.configuration.configurations

import org.gradle.api.Project
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

internal fun Project.configureKotlinCompilerOptions() {
    project.tasks.withType<KotlinCompile> {
        kotlinOptions {
            jvmTarget = "17"
            allWarningsAsErrors = false
            // this is required to produce a unique META-INF/*.kotlin_module files
            moduleName = project.path.removePrefix(":").replace(':', '-')
        }
    }
}