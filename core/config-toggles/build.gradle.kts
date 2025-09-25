import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import org.json.JSONArray

plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.hilt.android)
    alias(deps.plugins.ksp)
    id("configuration")
}

buildscript {
    dependencies {
        classpath("com.squareup:kotlinpoet:1.15.0")
        classpath("org.json:json:20231013")
    }
}

android {
    namespace = "com.tangem.core.configtoggle"
    sourceSets["main"].java.srcDir("build/generated/source/toggles")
}

tasks.named("preBuild") {
    dependsOn(generateFeatureToggles, generateExcludedBlockchainToggles)
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
    implementation(deps.timber)
    ksp(deps.moshi.kotlin.codegen)

    /** Core modules */
    implementation(projects.core.datasource)
    implementation(projects.core.utils)

    testImplementation(deps.test.coroutine)
    testImplementation(deps.test.junit5)
    testRuntimeOnly(deps.test.junit5.engine)
    testImplementation(deps.test.mockk)
    testImplementation(deps.test.truth)
    testImplementation(projects.common.test)
}

val generateFeatureToggles by tasks.registering {
    generateToggles(
        inputFilePath = "src/main/assets/configs/feature_toggles_config.json",
        generatedFileName = "FeatureToggles",
    )
}

val generateExcludedBlockchainToggles by tasks.registering {
    generateToggles(
        inputFilePath = "src/main/assets/configs/excluded_blockchains_config.json",
        generatedFileName = "ExcludedBlockchainToggles",
    )
}

fun Task.generateToggles(inputFilePath: String, generatedFileName: String) {
    val inputFile = file(inputFilePath)
    val outputDir = file("build/generated/source/toggles")

    inputs.file(inputFile)
    outputs.dir(outputDir)

    doLast {
        val jsonText = inputFile.readText()
        val jsonArray = JSONArray(jsonText)

        val entries = (0 until jsonArray.length()).map { i ->
            val obj = jsonArray.getJSONObject(i)
            val name = obj.getString("name")
            val version = obj.getString("version")
            CodeBlock.of("%S to %S", name, version)
        }

        val mapInitializer = CodeBlock.builder()
            .add("mapOf(\n")
            .indent()
            .apply {
                entries.forEachIndexed { index, entry ->
                    add(entry)
                    if (index != entries.lastIndex) add(",\n") else add("\n")
                }
            }
            .unindent()
            .add(")")
            .build()

        val objectBuilder = TypeSpec.objectBuilder(name = generatedFileName)
            .addKdoc("Generated from $inputFilePath")
            .addProperty(
                PropertySpec.builder("values", MAP.parameterizedBy(STRING, STRING))
                    .initializer(mapInitializer)
                    .build()
            )

        val fileSpec = FileSpec.builder(packageName = "com.tangem.core.configtoggle", fileName = generatedFileName)
            .addType(objectBuilder.build())
            .build()

        val outputPackageDir = File(outputDir, "")
        outputPackageDir.mkdirs()
        fileSpec.writeTo(outputPackageDir)
    }
}