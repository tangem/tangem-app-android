package com.tangem.plugin.configuration.configurations

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import kotlinx.serialization.json.*
import java.io.File

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
        when (value) {
            is JsonPrimitive -> {
                when {
                    value.isString -> {
                        val stringValue = value.content
                        val propertySpec = PropertySpec.builder(name, STRING)
                            .addModifiers(KModifier.CONST)
                            .initializer("%S", stringValue)

                        builder.addProperty(propertySpec.build())
                    }
                    value.booleanOrNull != null -> {
                        builder.addProperty(
                            PropertySpec.builder(name, BOOLEAN)
                                .addModifiers(KModifier.CONST)
                                .initializer("%L", value.boolean)
                                .build()
                        )
                    }
                    value.longOrNull != null -> {
                        builder.addProperty(
                            PropertySpec.builder(name, LONG)
                                .addModifiers(KModifier.CONST)
                                .initializer("%L", value.long)
                                .build()
                        )
                    }
                    value.doubleOrNull != null -> {
                        builder.addProperty(
                            PropertySpec.builder(name, DOUBLE)
                                .addModifiers(KModifier.CONST)
                                .initializer("%L", value.double)
                                .build()
                        )
                    }
                    else -> {
                        // Null value
                        builder.addProperty(
                            PropertySpec.builder(name, STRING.copy(nullable = true))
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
                    PropertySpec.builder(name, listType)
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
     * Converts a string to PascalCase, handling dashes and underscores.
     * Examples: "cosmos-hub" -> "CosmosHub", "polygon-zkevm" -> "PolygonZkevm"
     */
    private fun String.toPascalCase(): String {
        return this.split("-", "_")
            .filter { it.isNotEmpty() }
            .joinToString("") { part ->
                part.replaceFirstChar { it.uppercase() }
            }
    }
}
