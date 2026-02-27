import com.tangem.plugin.configuration.configurations.EnvironmentConfigGenerator
import com.tangem.plugin.configuration.configurations.extension.kaptForObfuscatingVariants
import com.tangem.plugin.configuration.model.BuildType

plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
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

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

dependencies {

    /** Project */
    implementation(projects.core.analytics)
    implementation(projects.core.utils)
    implementation(projects.core.res)
    implementation(projects.libs.auth)
    implementation(projects.domain.appTheme.models)
    implementation(projects.domain.core)
    implementation(projects.domain.tokens.models)
    implementation(projects.domain.wallets.models)
    implementation(projects.domain.balanceHiding.models)
    implementation(projects.domain.txhistory.models)
    implementation(projects.domain.staking.models)
    implementation(projects.domain.onramp.models)
    implementation(projects.domain.models)
    implementation(projects.domain.nft.models)
    implementation(projects.domain.walletConnect.models)
    implementation(projects.domain.yieldSupply.models)
    implementation(projects.domain.visa.models)

    /** Tangem libraries */
    implementation(tangemDeps.blockchain)
    implementation(tangemDeps.card.core)

    /** DI */
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)

    /** Coroutines */
    implementation(deps.kotlin.coroutines)
    implementation(deps.kotlin.coroutines.rx2)
    implementation(deps.kotlin.datetime)

    /** Logging */
    implementation(deps.timber)

    /** Network */
    implementation(deps.moshi)
    implementation(deps.moshi.kotlin)
    implementation(deps.moshi.adapters)
    implementation(deps.moshi.adapters.ext)
    implementation(deps.okHttp)
    implementation(deps.okHttp.prettyLogging)
    implementation(deps.retrofit)
    implementation(deps.retrofit.moshi)
    ksp(deps.moshi.kotlin.codegen)
    kaptForObfuscatingVariants(deps.retrofit.response.type.keeper)

    /** Time */
    implementation(deps.jodatime)

    /** Security */
    implementation(deps.spongecastle.core)

    /** Chucker */
    debugImplementation(deps.chucker)
    mockedImplementation(deps.chucker)
    externalImplementation(deps.chuckerStub)
    internalImplementation(deps.chuckerStub)
    releaseImplementation(deps.chuckerStub)

    /** Local storages */
    implementation(deps.androidx.datastore)
    implementation(deps.room.runtime)
    implementation(deps.room.ktx)
    ksp(deps.room.compiler)

    testImplementation(projects.test.core)
    testRuntimeOnly(deps.test.junit5.engine)
}