package com.tangem.plugin.configuration.configurations

import com.squareup.kotlinpoet.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.util.Locale

/**
 * Generator for toggle configuration Kotlin enum classes from JSON files.
 * Parses a JSON array of `{ "name": "...", "version": "..." }` entries
 * and generates an enum class with `rawName` and `version` properties for each toggle.
 *
[REDACTED_AUTHOR]
 */
object TogglesGenerator {

    private const val PACKAGE_NAME = "com.tangem.core.configtoggle"

    /**
     * Generates a Kotlin enum class from a JSON toggles config file.
     *
     * @param inputFile     JSON configuration file (array of objects with "name" and "version" fields)
     * @param outputDir     output directory for the generated Kotlin file
     * @param fileName      name of the generated Kotlin enum class (e.g. "FeatureToggles")
     */
    fun generate(inputFile: File, outputDir: File, fileName: String) {
        val jsonText = inputFile.readText()
        val jsonArray = Json.parseToJsonElement(jsonText).jsonArray

        val entries = jsonArray.map { element ->
            val obj = element.jsonObject
            val name = obj.getValue("name").jsonPrimitive.content
            val version = obj.getValue("version").jsonPrimitive.content
            ToggleEntry(name = name, enumName = name.toEnumEntryName(), version = version)
        }

        val enumBuilder = TypeSpec.enumBuilder(fileName)
            .addKdoc("Generated from ${inputFile.name}\nAuto-generated - do not edit manually.")
            .primaryConstructor(
                FunSpec.constructorBuilder()
                    .addParameter(ParameterSpec.builder("rawName", String::class).build())
                    .addParameter(ParameterSpec.builder("version", String::class).build())
                    .build(),
            )
            .addProperty(
                PropertySpec.builder("rawName", String::class)
                    .initializer("rawName")
                    .build(),
            )
            .addProperty(
                PropertySpec.builder("version", String::class)
                    .initializer("version")
                    .build(),
            )
            .apply {
                entries.forEach { entry ->
                    addEnumConstant(
                        entry.enumName,
                        TypeSpec.anonymousClassBuilder()
                            .addSuperclassConstructorParameter("%S", entry.name)
                            .addSuperclassConstructorParameter("%S", entry.version)
                            .build(),
                    )
                }
            }

        val fileSpec = FileSpec.builder(packageName = PACKAGE_NAME, fileName = fileName)
            .indent("    ")
            .addType(enumBuilder.build())
            .build()

        outputDir.mkdirs()
        fileSpec.writeTo(outputDir)

        val generatedFile = File(outputDir, "${PACKAGE_NAME.replace('.', '/')}/$fileName.kt")
        if (generatedFile.exists()) {
            val content = generatedFile.readText()
            val fixedContent = content
                .replace("public enum class ", "enum class ")
                .replace("public companion object", "companion object")
                .replace("public val ", "val ")
                .replace(Regex(""",\s*;\s*\n\s*}"""), ",\n}")
            generatedFile.writeText(fixedContent)
        }
    }

    /**
     * Converts a toggle name to a valid Kotlin enum entry name.
     * - Replaces `/`, `-`, `.`, spaces with `_`
     * - Converts to UPPER_CASE
     * - Empty string becomes `EMPTY`
     */
    private fun String.toEnumEntryName(): String {
        if (isBlank()) return "EMPTY"
        return replace(Regex("[/\\-. ]"), "_").uppercase(Locale.ROOT)
    }

    private data class ToggleEntry(val name: String, val enumName: String, val version: String)
}