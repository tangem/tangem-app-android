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

android {
    namespace = "com.tangem.datasource"

    sourceSets["main"].java.srcDir("build/generated/source/environment-config")

    room {
        schemaDirectory("$projectDir/schemas")
    }
}

fun resolveCurrentBuildType(): BuildType {
    val requestedTasks = gradle.startParameter.taskRequests.flatMap { it.args }
    return BuildType.values().firstOrNull { bt ->
        requestedTasks.any { task ->
            task.contains(bt.id.replaceFirstChar { it.uppercase() }, ignoreCase = true)
        }
    } ?: BuildType.Debug
}

val currentBuildType = resolveCurrentBuildType()

tasks.named("preBuild") {
    dependsOn(generateEnvironmentConfig)
}

val generateEnvironmentConfig by tasks.registering {
    generateEnvironmentConfig(
        configFilePath = "app/src/main/assets/tangem-app-config/config_${currentBuildType.environment}.json",
        buildType = currentBuildType,
    )
}

fun Task.generateEnvironmentConfig(configFilePath: String, buildType: BuildType) {
    val inputFile = rootProject.projectDir.resolve(configFilePath)
    val outputDir = file("build/generated/source/environment-config")

    inputs.file(inputFile)
    outputs.dir(outputDir)

    doLast {
        if (inputFile.exists()) {
            logger.lifecycle("Generating EnvironmentConfig for buildType=${buildType.id}, environment=${buildType.environment}")
            EnvironmentConfigGenerator.generate(inputFile, outputDir)
        } else {
            throw GradleException("Config file not found: ${inputFile.absolutePath}")
        }
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