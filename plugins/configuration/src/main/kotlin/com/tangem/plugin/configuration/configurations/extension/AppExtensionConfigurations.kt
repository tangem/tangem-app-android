package com.tangem.plugin.configuration.configurations.extension

import com.android.build.gradle.AppExtension
import com.tangem.plugin.configuration.model.AppConfig
import com.tangem.plugin.configuration.model.BuildType
import com.tangem.plugin.configuration.utils.BuildConfigFieldFactory
import org.gradle.api.Project
import com.android.build.gradle.internal.dsl.BuildType as AndroidBuildType

internal fun AppExtension.configure(project: Project) {
    configureCompileSdk()
    configureDefaultConfig(project)
    configureBuildFeatures()
    configureBuildTypes()
    configurePackagingOptions()
    configureCompose(project)
    configureCompilerOptions()
}

private fun AppExtension.configureDefaultConfig(project: Project) {
    defaultConfig {
        applicationId = AppConfig.packageName
        minSdk = AppConfig.minSdkVersion
        targetSdk = AppConfig.targetSdkVersion

        versionCode = if (project.hasProperty("versionCode")) {
            (project.property("versionCode") as String).toInt()
        } else {
            AppConfig.versionCode
        }

        versionName = if (project.hasProperty("versionName")) {
            project.property("versionName") as String
        } else {
            AppConfig.versionName
        }

        buildFeatures.buildConfig = true

        testInstrumentationRunner = "com.tangem.common.HiltTestRunner"
    }
}
// [REDACTED_TODO_COMMENT]
private fun AppExtension.configureBuildFeatures() {
    buildFeatures.apply {
        viewBinding = true
    }
}

private fun AppExtension.configureBuildTypes() {
    buildTypes {
        BuildType.values().forEach { buildVariant ->
            maybeCreate(buildVariant.id).apply {
                configureBuildVariant(extension = this@configureBuildTypes, buildVariant)

                BuildConfigFieldFactory(
                    fields = buildVariant.configFields,
                    builder = ::buildConfigField,
                ).create()
            }
        }
    }
}

private fun AndroidBuildType.configureBuildVariant(extension: AppExtension, buildType: BuildType) {
    when (buildType) {
        BuildType.Release,
        BuildType.External,
        -> {
            isDebuggable = false
            isMinifyEnabled = false
            proguardFiles(extension.getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
        }
        BuildType.Debug -> {
            isDebuggable = true
            isMinifyEnabled = false
        }
        BuildType.Internal,
        BuildType.Mocked,
        -> {
            initWith(extension.buildTypes.getByName(BuildType.Release.id))
            matchingFallbacks.add(BuildType.Release.id)
            signingConfig = extension.signingConfigs.getByName(BuildType.Debug.id)
            isDebuggable = true
        }
    }

    versionNameSuffix = buildType.versionSuffix?.let { "-$it" }
    applicationIdSuffix = buildType.appIdSuffix?.let { ".$it" }
}

private fun AppExtension.configurePackagingOptions() {
    packagingOptions {
        resources {
            excludes += "lib/x86_64/darwin/libscrypt.dylib"
            excludes += "lib/x86_64/freebsd/libscrypt.so"
            excludes += "lib/x86_64/linux/libscrypt.so"
            excludes += "META-INF/gradle/incremental.annotation.processors"
        }
    }
}
