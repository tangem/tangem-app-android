package com.tangem.plugin.configuration.configurations.extension

import com.android.build.gradle.LibraryExtension
import com.tangem.plugin.configuration.model.AppConfig
import com.tangem.plugin.configuration.model.BuildType
import com.tangem.plugin.configuration.utils.BuildConfigFieldFactory
import com.tangem.plugin.configuration.utils.resolveGradleProperties
import org.gradle.api.Project

internal fun LibraryExtension.configure(project: Project) {
    configureCompileSdk()
    configureDefaultConfig()
    configureBuildTypes(project)
    configurePackagingOptions()
    configureCompose(project)
    configureCompilerOptions()
}

private fun LibraryExtension.configureDefaultConfig() {
    defaultConfig {
        minSdk = AppConfig.minSdkVersion
        vectorDrawables {
            useSupportLibrary = true
        }
        buildFeatures.buildConfig = true
    }
}

private fun LibraryExtension.configureBuildTypes(project: Project) {
    buildTypes {
        BuildType.values().forEach { buildVariant ->
            maybeCreate(buildVariant.id).apply {
                val fields = buildVariant.configFields.resolveGradleProperties(project)
                BuildConfigFieldFactory(
                    fields = fields,
                    builder = ::buildConfigField,
                ).create()
            }
        }
    }
}

private fun LibraryExtension.configurePackagingOptions() {
    packaging {
        resources {
            excludes += "lib/x86_64/darwin/libscrypt.dylib"
            excludes += "lib/x86_64/freebsd/libscrypt.so"
            excludes += "lib/x86_64/linux/libscrypt.so"
            excludes += "META-INF/gradle/incremental.annotation.processors"
            pickFirsts += "google/protobuf/*"
        }
    }
}