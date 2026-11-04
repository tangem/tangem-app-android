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
    private const val SOURCE_PATH_SEGMENTS = 3

    /**
     * Generates a Kotlin enum class from a JSON toggles config file.
     *
     * @param inputFile     JSON configuration file (array of objects with "name" and "version" fields)
     * @param outputDir     output directory for the generated Kotlin file
     * @param fileName      name of the generated Kotlin enum class (e.g. "FeatureToggles")
     */
    fun generate(inputFile: File, outputDir: File, fileName: String) {
        generate(inputFiles = listOf(inputFile), outputDir = outputDir, fileName = fileName)
    }

    /**
     * Generates one Kotlin enum class from several JSON toggles config files, entries in the order of [inputFiles].
     * Fails if a toggle maps to the same enum entry in more than one place.
     */
    fun generate(inputFiles: List<File>, outputDir: File, fileName: String) {
        val entries = inputFiles.flatMap(::parseEntries)
        requireNoDuplicates(entries, fileName)

        val sources = inputFiles.joinToString(separator = "\n") { it.relativeName() }
        val enumBuilder = TypeSpec.enumBuilder(fileName)
            .addKdoc("Generated from\n$sources\nAuto-generated - do not edit manually.")
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

    private fun parseEntries(inputFile: File): List<ToggleEntry> {
        val jsonArray = Json.parseToJsonElement(inputFile.readText()).jsonArray

        return jsonArray.map { element ->
            val obj = element.jsonObject
            val name = obj.getValue("name").jsonPrimitive.content
            val version = obj.getValue("version").jsonPrimitive.content
            ToggleEntry(name = name, enumName = name.toEnumEntryName(), version = version, source = inputFile)
        }
    }

    private fun requireNoDuplicates(entries: List<ToggleEntry>, fileName: String) {
        val duplicates = entries.groupBy(ToggleEntry::enumName).filterValues { it.size > 1 }

        require(duplicates.isEmpty()) {
            val details = duplicates.entries.joinToString(separator = "; ") { (enumName, group) ->
                "$enumName in " + group.joinToString { it.source.relativeName() }
            }
            "Duplicate entries in $fileName: $details"
        }
    }

    /** Last path segments (`store/config-toggles/feature_toggles.json`): the bare file name repeats across streams */
    private fun File.relativeName(): String {
        val path = toPath()
        return path.subpath(maxOf(0, path.nameCount - SOURCE_PATH_SEGMENTS), path.nameCount).toString()
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

    private data class ToggleEntry(val name: String, val enumName: String, val version: String, val source: File)
}