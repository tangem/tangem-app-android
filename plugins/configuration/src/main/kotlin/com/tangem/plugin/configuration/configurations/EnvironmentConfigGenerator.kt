package com.tangem.plugin.configuration.configurations

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import kotlinx.serialization.json.*
import java.io.File
import java.util.Locale

/**
 * Generator for environment configuration Kotlin object from JSON file.
 * Automatically parses JSON structure and generates corresponding Kotlin code.
 *
[REDACTED_AUTHOR]
 */
object EnvironmentConfigGenerator {

    private const val PACKAGE_NAME = "com.tangem.datasource.local.config.environment.generated"
    private const val CLASS_NAME = "GeneratedEnvironmentConfig"

    /**
     * Generates GeneratedEnvironmentConfig object from JSON file.
     *
     * @param inputFile JSON configuration file
     * @param outputDir Output directory for generated Kotlin file
     */
    fun generate(inputFile: File, outputDir: File) {
        val jsonText = inputFile.readText()
        val json = Json.parseToJsonElement(jsonText).jsonObject

        val objectBuilder = TypeSpec.objectBuilder(CLASS_NAME)
            .addKdoc("Generated from ${inputFile.name}\nAuto-generated - do not edit manually.")

        // Iterate over all JSON keys and generate properties
        json.entries.forEach { (key, value) ->
            addPropertyFromJsonValue(objectBuilder, key, value)
        }

        val fileSpec = FileSpec.builder(PACKAGE_NAME, CLASS_NAME)
            .indent("    ") // Use 4 spaces for indentation
            .addType(objectBuilder.build())
            .build()

        outputDir.mkdirs()
        fileSpec.writeTo(outputDir)

        // Post-process generated file
        val generatedFile = File(outputDir, PACKAGE_NAME.replace('.', '/') + "/$CLASS_NAME.kt")
        if (generatedFile.exists()) {
            val content = generatedFile.readText()
            val fixedContent = content
                // Add suppress annotation at file level
                .replaceFirst(
                    "package $PACKAGE_NAME",
                    "@file:Suppress(\n" +
                        "    \"MaximumLineLength\",\n" +
                        "    \"MaxLineLength\",\n" +
                        "    \"Indentation\",\n" +
                        ")\n\npackage $PACKAGE_NAME"
                )
                // Remove redundant public modifiers
                .replace("public object ", "object ")
                .replace("public val ", "val ")
                .replace("public const val ", "const val ")
            generatedFile.writeText(fixedContent)
        }
    }

    /**
     * Adds a property to the TypeSpec based on the JSON value type
     */
    private fun addPropertyFromJsonValue(builder: TypeSpec.Builder, name: String, value: JsonElement) {
        val propertyName = name.toValidIdentifier()
        when (value) {
            is JsonPrimitive -> {
                when {
                    value.isString -> {
                        val stringValue = value.content
                        val propertySpec = PropertySpec.builder(propertyName, STRING)
                            .addModifiers(KModifier.CONST)
                            .initializer("%S", stringValue)

                        builder.addProperty(propertySpec.build())
                    }
                    value.booleanOrNull != null -> {
                        builder.addProperty(
                            PropertySpec.builder(propertyName, BOOLEAN)
                                .addModifiers(KModifier.CONST)
                                .initializer("%L", value.boolean)
                                .build()
                        )
                    }
                    value.longOrNull != null -> {
                        builder.addProperty(
                            PropertySpec.builder(propertyName, LONG)
                                .addModifiers(KModifier.CONST)
                                .initializer("%L", value.long)
                                .build()
                        )
                    }
                    value.doubleOrNull != null -> {
                        builder.addProperty(
                            PropertySpec.builder(propertyName, DOUBLE)
                                .addModifiers(KModifier.CONST)
                                .initializer("%L", value.double)
                                .build()
                        )
                    }
                    else -> {
                        // Null value
                        builder.addProperty(
                            PropertySpec.builder(propertyName, STRING.copy(nullable = true))
                                .initializer("null")
                                .build()
                        )
                    }
                }
            }
            is JsonArray -> {
                val listType = LIST.parameterizedBy(STRING)
                val values = value.map { it.jsonPrimitive.content }
                builder.addProperty(
                    PropertySpec.builder(propertyName, listType)
                        .initializer(
                            CodeBlock.builder()
                                .add("listOf(\n")
                                .apply {
                                    values.forEach { v ->
                                        add("    %S,\n", v)
                                    }
                                }
                                .add(")")
                                .build()
                        )
                        .build()
                )
            }
            is JsonObject -> {
                // Generate nested object with proper naming (convert dashes to camelCase)
                val nestedClassName = name.toPascalCase()
                val nestedObjectBuilder = TypeSpec.objectBuilder(nestedClassName)

                value.entries.forEach { (nestedKey, nestedValue) ->
                    addPropertyFromJsonValue(nestedObjectBuilder, nestedKey, nestedValue)
                }

                builder.addType(nestedObjectBuilder.build())
            }
        }
    }

    /**
     * Converts a string to PascalCase for use as a class/object name.
     * - If the string contains separators (dots, dashes, underscores), splits and joins in PascalCase
     * - If no separators, just capitalizes the first letter to preserve original casing (e.g., "AppsFlyer" stays "AppsFlyer")
     */
    private fun String.toPascalCase(): String {
        val hasSeparators = contains('.') || contains('-') || contains('_')
        return if (hasSeparators) {
            this.split("-", "_", ".")
                .filter { it.isNotEmpty() }
                .joinToString("") { part ->
                    part.lowercase(Locale.ROOT).replaceFirstChar { it.uppercase(Locale.ROOT) }
                }
        } else {
            this.replaceFirstChar { it.uppercase(Locale.ROOT) }
        }
    }

    /**
     * Converts a string to a valid Kotlin property identifier.
     * - If the string contains dots, converts to camelCase (dots cannot be escaped by KotlinPoet)
     * - Otherwise, ensures the first letter is lowercase (Kotlin property naming convention)
     */
    private fun String.toValidIdentifier(): String {
        return if (contains('.')) {
            toCamelCase()
        } else {
            this.replaceFirstChar { it.lowercase(Locale.ROOT) }
        }
    }

    /**
     * Converts a string to camelCase, handling dashes, underscores, and dots.
     * Normalizes each segment to lowercase first for consistent results.
     * Examples: "cosmos-hub" -> "cosmosHub", "customer.io" -> "customerIo", "CUSTOMER.IO" -> "customerIo"
     */
    private fun String.toCamelCase(): String {
        val parts = this.split("-", "_", ".")
            .filter { it.isNotEmpty() }
        return parts.mapIndexed { index, part ->
            val normalized = part.lowercase(Locale.ROOT)
            if (index == 0) normalized
            else normalized.replaceFirstChar { it.uppercase(Locale.ROOT) }
        }.joinToString("")
    }
}
