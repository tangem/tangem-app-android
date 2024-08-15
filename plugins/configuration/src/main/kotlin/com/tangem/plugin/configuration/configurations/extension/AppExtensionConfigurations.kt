package com.tangem.plugin.configuration.configurations.extension

import com.android.build.gradle.AppExtension
import com.android.build.gradle.internal.dsl.DefaultConfig
import com.tangem.plugin.configuration.model.AppConfig
import com.tangem.plugin.configuration.model.BuildType
import com.tangem.plugin.configuration.utils.BuildConfigFieldFactory
import org.gradle.api.Project
import com.android.build.gradle.internal.dsl.BuildType as AndroidBuildType

internal fun AppExtension.configure(project: Project) {
    configureCompileSdk()
    val defaultConfig = configureDefaultConfig(project)
    configureBuildFeatures()
    configureBuildTypes(defaultConfig)
    configurePackagingOptions()
    configureCompose(project)
    configureCompilerOptions()
}

private fun AppExtension.configureDefaultConfig(project: Project): DefaultConfig {
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

    return defaultConfig
}

// TODO: [REDACTED_JIRA]
private fun AppExtension.configureBuildFeatures() {
    buildFeatures.apply {
        viewBinding = true
    }
}

private fun AppExtension.configureBuildTypes(defaultConfig: DefaultConfig) {
    buildTypes {
        BuildType.values().forEach { buildType ->
            maybeCreate(buildType.id).apply {
                configureBuildVariant(
                    appExtension = this@configureBuildTypes,
                    buildType = buildType,
                    defaultConfig = defaultConfig
                )

                BuildConfigFieldFactory(
                    fields = buildType.configFields,
                    builder = ::buildConfigField,
                ).create()
            }
        }
    }
    testBuildType = BuildType.Mocked.id
}

private fun AndroidBuildType.configureBuildVariant(
    appExtension: AppExtension,
    buildType: BuildType,
    defaultConfig: DefaultConfig,
) {
    when (buildType) {
        BuildType.Release -> {
            isDebuggable = false
            isMinifyEnabled = false
            proguardFiles(appExtension.getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
        }
        BuildType.Debug -> {
            isDebuggable = true
            isMinifyEnabled = false
        }
        BuildType.External -> {
            initWith(appExtension.buildTypes.getByName(BuildType.Release.id))
            matchingFallbacks.add(BuildType.Release.id)
            signingConfig = appExtension.signingConfigs.getByName(BuildType.Debug.id)
        }
        BuildType.Internal -> {
            initWith(appExtension.buildTypes.getByName(BuildType.Release.id))
            matchingFallbacks.add(BuildType.Release.id)
            signingConfig = appExtension.signingConfigs.getByName(BuildType.Debug.id)
            isDebuggable = true
        }
        BuildType.Mocked -> {
            defaultConfig.versionName = LARGE_VERSION_NAME
            initWith(appExtension.buildTypes.getByName(BuildType.Release.id))
            matchingFallbacks.add(BuildType.Mocked.id)
            signingConfig = appExtension.signingConfigs.getByName(BuildType.Debug.id)
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

private const val LARGE_VERSION_NAME = "100.0.0"