package com.tangem.plugin.configuration.configurations

import org.gradle.api.Project
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

internal fun Project.configureKotlinCompilerOptions() {
    kotlinExtension.sourceSets.all {
        // https://github.com/Kotlin/KEEP/blob/explicit-backing-fields-re/proposals/explicit-backing-fields.md
        languageSettings.enableLanguageFeature("ExplicitBackingFields")
    }
    project.tasks.withType<KotlinCompile> {
        kotlinOptions {
            jvmTarget = "17"
            allWarningsAsErrors = false
            // this is required to produce a unique META-INF/*.kotlin_module files
            moduleName = project.path.removePrefix(":").replace(':', '-')
        }
    }
}