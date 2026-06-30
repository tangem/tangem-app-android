import com.tangem.plugin.configuration.configurations.EnvironmentConfigGenerator
import com.tangem.plugin.configuration.configurations.extension.kaptForObfuscatingVariants
import com.tangem.plugin.configuration.model.BuildType

plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.kotlin.serialization)
    alias(deps.plugins.hilt.android)
    alias(deps.plugins.room)
    alias(deps.plugins.ksp)
    id("configuration")
}

abstract class GenerateEnvironmentConfigTask : DefaultTask() {

    @get:InputFile
    abstract val configFile: RegularFileProperty

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun generate() {
        val input = configFile.get().asFile
        require(input.exists()) { "Config file not found: ${input.absolutePath}" }
        logger.lifecycle("Generating EnvironmentConfig from ${input.name}")
        EnvironmentConfigGenerator.generate(input, outputDir.get().asFile)
    }
}

android {
    namespace = "com.tangem.datasource"

    room {
        schemaDirectory("$projectDir/schemas")
    }
}

androidComponents {
    onVariants { variant ->
        val buildType = BuildType.values().firstOrNull { it.id == variant.buildType } ?: BuildType.Debug
        val configFile = rootProject.file(
            "app/src/main/assets/tangem-app-config/config_${buildType.environment}.json",
        )

        val taskProvider = tasks.register<GenerateEnvironmentConfigTask>(
            "generateEnvironmentConfig${variant.name.replaceFirstChar { it.uppercaseChar() }}",
        ) {
            this.configFile.set(configFile)
            outputDir.set(layout.buildDirectory.dir("generated/source/environment-config/${variant.name}"))
            doFirst {
                logger.lifecycle("[Environment config] Running: ${this.name}")
            }
        }

        variant.sources.java?.addGeneratedSourceDirectory(taskProvider, GenerateEnvironmentConfigTask::outputDir)
    }
}
dependencies {

    // region DI
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)
    // endregion

    // region Kotlin
    api(deps.kotlin.coroutines)
    api(deps.kotlin.datetime)
    api(deps.kotlin.serialization)
    // endregion

    // region AndroidX
    implementation(deps.androidx.core)
    api(deps.androidx.datastore)
    // endregion

    // region Network
    api(deps.moshi)
    implementation(deps.moshi.adapters)
    implementation(deps.moshi.adapters.ext)
    implementation(deps.moshi.kotlin)
    ksp(deps.moshi.kotlin.codegen)
    api(deps.okHttp)
    implementation(deps.okHttp.prettyLogging)
    implementation(deps.okio)
    api(deps.retrofit)
    api(deps.retrofit.moshi)
    kaptForObfuscatingVariants(deps.retrofit.response.type.keeper)
    // endregion

    // region Room
    api(deps.room.runtime)
    ksp(deps.room.compiler)
    // endregion

    // region Other libraries
    api(deps.jodatime)
    runtimeOnly(deps.spongecastle.core)
    // endregion

    // region Chucker
    debugImplementation(deps.chucker)
    mockedImplementation(deps.chucker)
    externalImplementation(deps.chuckerStub)
    internalImplementation(deps.chuckerStub)
    releaseImplementation(deps.chuckerStub)
    // endregion

    // region Tangem
    api(tangemDeps.blockchain)
    api(tangemDeps.card.core)
    // endregion

    // region Core modules
    api(projects.core.analytics)
    implementation(projects.core.analytics.models)
    api(projects.core.utils)
    implementation(projects.core.res)
    // endregion

    // region Domain models
    api(projects.domain.models)
    api(projects.domain.nft.models)
    api(projects.domain.onramp.models)
    api(projects.domain.staking.models)
    api(projects.domain.txhistory.models)
    api(projects.domain.visa.models)
    api(projects.domain.walletConnect.models)
    api(projects.domain.wallets.models)
    api(projects.domain.yieldSupply.models)
    // endregion

    // region Tests
    testImplementation(projects.test.core)
    // endregion
}