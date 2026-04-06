import com.tangem.plugin.configuration.configurations.TogglesGenerator

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

        files.zip(names).forEach { (inputFile, fileName) ->
            require(inputFile.exists()) { "Config file not found: ${inputFile.absolutePath}" }
            logger.lifecycle("Generating toggles from ${inputFile.name}")
            TogglesGenerator.generate(inputFile, outputDir.get().asFile, fileName)
        }
    }
}

android {
    namespace = "com.tangem.core.configtoggle"
    sourceSets["main"].java.srcDir("build/generated/source/toggles")
}

/** Config file to generated enum class name mapping */
val toggles = mapOf(
    file("src/main/assets/configs/feature_toggles_config.json") to "FeatureToggles",
    file("src/main/assets/configs/excluded_blockchains_config.json") to "ExcludedBlockchainToggles",
)

val generateToggles = tasks.register<GenerateTogglesTask>("generateToggles") {
    configFiles.from(toggles.keys)
    fileNames.set(toggles.values.toList())
    outputDir.set(layout.buildDirectory.dir("generated/source/toggles"))
}

tasks.named("preBuild") {
    dependsOn(generateToggles)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

dependencies {
    /** DI */
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)

    /** Local storages */
    implementation(deps.androidx.datastore)

    /** Other libraries */
    implementation(deps.moshi)
    implementation(deps.moshi.kotlin)
    ksp(deps.moshi.kotlin.codegen)

    /** Core modules */
    implementation(projects.core.datasource)
    implementation(projects.core.utils)

    testImplementation(projects.test.core)
    testRuntimeOnly(deps.test.junit5.engine)
}