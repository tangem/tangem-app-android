import com.tangem.plugin.configuration.configurations.TogglesGenerator
import io.gitlab.arturbosch.detekt.Detekt

plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.hilt.android)
    alias(deps.plugins.ksp)
    id("configuration")
}

abstract class GenerateTogglesTask : DefaultTask() {

    @get:InputFiles
    abstract val configFiles: ConfigurableFileCollection

    @get:Input
    abstract val fileNames: ListProperty<String>

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun generate() {
        val files = configFiles.files.toList()
        val names = fileNames.get()

        require(files.size == names.size) {
            "configFiles (${files.size}) and fileNames (${names.size}) must have the same size"
        }

        files.zip(names)
            .groupBy(keySelector = { it.second }, valueTransform = { it.first })
            .forEach { (fileName, inputFiles) ->
                inputFiles.forEach { require(it.exists()) { "Config file not found: ${it.absolutePath}" } }
                logger.lifecycle("Generating $fileName from ${inputFiles.joinToString { it.path }}")
                TogglesGenerator.generate(inputFiles, outputDir.get().asFile, fileName)
            }
    }
}

android {
    namespace = "com.tangem.core.configtoggle"
    sourceSets["main"].java.srcDir("build/generated/source/toggles")
}

/**
 * Each stream keeps its feature toggles in `<stream>/config-toggles/feature_toggles.json`, so CODEOWNERS routes
 * a new toggle to the stream team instead of core. All of them are merged into one `FeatureToggles` enum.
 */
val featureTogglesStreams = listOf("core", "store", "grow", "spend")

/** Config file to generated enum class name mapping; files with the same enum name are merged in this order */
val toggles = featureTogglesStreams.associate { stream ->
    rootProject.file("$stream/config-toggles/feature_toggles.json") to "FeatureToggles"
} + mapOf(
    file("src/main/assets/configs/excluded_blockchains_config.json") to "ExcludedBlockchainToggles",
)

val generateToggles = tasks.register<GenerateTogglesTask>("generateToggles") {
    configFiles.from(toggles.keys)
    fileNames.set(toggles.values.toList())
    outputDir.set(layout.buildDirectory.dir("generated/source/toggles"))
}

// Tests read every stream file, registered or not, so an unregistered one must invalidate their cached result
tasks.withType<Test>().configureEach {
    inputs.files(rootProject.fileTree(rootDir) { include("*/config-toggles/feature_toggles.json") })
        .withPathSensitivity(PathSensitivity.RELATIVE)
        .withPropertyName("featureToggleConfigs")
}

tasks.named("preBuild") {
    dependsOn(generateToggles)
}

tasks.withType<Detekt>().configureEach {
    exclude { it.file.absolutePath.contains("/build/generated/") }
}
dependencies {

    // region DI
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)
    // endregion

    // region Kotlin
    implementation(deps.kotlin.coroutines)
    // endregion

    // region AndroidX
    implementation(deps.androidx.annotation)
    implementation(deps.androidx.datastore)
    // endregion

    // region Other libraries
    implementation(deps.moshi)
    ksp(deps.moshi.kotlin.codegen)
    // endregion

    // region Core modules
    implementation(projects.core.local)
    implementation(projects.core.utils)
    // endregion

    // region Tests
    testImplementation(projects.test.core)
    // endregion
    runtimeOnly(deps.room.runtime)
}