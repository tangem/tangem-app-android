package com.tangem.plugin.configuration.configurations.extension

import com.android.build.gradle.internal.plugins.AppPlugin
import com.android.build.gradle.internal.plugins.LibraryPlugin
import com.tangem.plugin.configuration.model.BuildType
import org.gradle.api.Project

fun Project.kaptForObfuscatingVariants(dependencyNotation: Any) {
    plugins.all {
        when (this) {
            is AppPlugin -> {
                BuildType.values().filter { it.obfuscating }.forEach {
                    dependencies.add(it.createKaptConfiguration(), dependencyNotation)
                }
            }
            is LibraryPlugin -> {
                BuildType.values().filter { it.obfuscating }.forEach {
                    dependencies.add(it.createKaptConfiguration(), dependencyNotation)
                }
            }
        }
    }
}

private fun BuildType.createKaptConfiguration() = "kapt" + name.replaceFirstChar(Char::titlecase)