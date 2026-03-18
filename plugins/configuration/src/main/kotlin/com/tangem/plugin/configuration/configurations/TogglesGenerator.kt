package com.tangem.plugin.configuration.configurations

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.util.Locale

/**
 * Generator for toggle configuration Kotlin enum classes from JSON files.
 * Parses a JSON array of `{ "name": "...", "version": "..." }` entries
 * and generates an enum class with an entry for each toggle and a `values: Map<String, String>`
 * in companion object mapping toggle name to its version string.
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

        val mapInitializer = buildCodeBlock {
            addStatement("mapOf(")
            withIndent {
                entries.forEach { entry ->
                    addStatement("%S to %S,", entry.name, entry.version)
                }
            }
            add(")")
        }

        val companionBuilder = TypeSpec.companionObjectBuilder()
            .addProperty(
                PropertySpec.builder("values", MAP.parameterizedBy(STRING, STRING))
                    .initializer(mapInitializer)
                    .build(),
            )

        val enumBuilder = TypeSpec.enumBuilder(fileName)
            .addKdoc("Generated from ${inputFile.name}\nAuto-generated - do not edit manually.")
            .apply {
                entries.forEach { entry ->
                    addEnumConstant(entry.enumName)
                }
            }
            .addType(companionBuilder.build())

        val fileSpec = FileSpec.builder(packageName = PACKAGE_NAME, fileName = fileName)
            .indent("    ")
            .addType(enumBuilder.build())
            .build()

        outputDir.mkdirs()
        fileSpec.writeTo(outputDir)

        // Remove redundant public visibility modifiers
        val generatedFile = File(outputDir, "${PACKAGE_NAME.replace('.', '/')}/$fileName.kt")
        if (generatedFile.exists()) {
            val content = generatedFile.readText()
            val fixedContent = content
                .replace("public enum class ", "enum class ")
                .replace("public companion object", "companion object")
                .replace("public val ", "val ")
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