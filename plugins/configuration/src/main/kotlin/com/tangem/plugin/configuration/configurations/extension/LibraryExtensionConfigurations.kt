package com.tangem.plugin.configuration.configurations.extension

import com.android.build.gradle.LibraryExtension
import com.tangem.plugin.configuration.model.AppConfig
import com.tangem.plugin.configuration.model.BuildType
import com.tangem.plugin.configuration.utils.BuildConfigFieldFactory
import org.gradle.api.Project

internal fun LibraryExtension.configure(project: Project) {
    configureCompileSdk()
    configureDefaultConfig()
    configureBuildTypes()
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

private fun LibraryExtension.configureBuildTypes() {
    buildTypes {
        BuildType.values().forEach { buildVariant ->
            maybeCreate(buildVariant.id).apply {
                BuildConfigFieldFactory(
                    fields = buildVariant.configFields,
                    builder = ::buildConfigField,
                ).create()
                if (buildVariant in customBuildTypes) matchingFallbacks.add(BuildType.Release.id)
            }
        }
    }
}

// External AARs are published with the standard debug/release variants only. The custom build types fall back to
// release, as they do in the app module, so such a dependency resolves in a library module too.
private val customBuildTypes = BuildType.values().toSet() - BuildType.Debug - BuildType.Release

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