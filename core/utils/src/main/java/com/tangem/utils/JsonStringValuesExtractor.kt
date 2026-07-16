package com.tangem.utils

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/**
 * Extracts all string primitive values from a [JsonElement] tree (recursively into
 * objects and arrays). Non-string primitives are ignored.
 */
object JsonStringValuesExtractor {

    fun extract(json: JsonElement): List<String> = json.extractStringValues()

    private fun JsonElement.extractStringValues(): List<String> = when (this) {
        is JsonPrimitive -> if (isString) listOfNotNull(contentOrNull) else emptyList()
        is JsonObject -> values.flatMap { it.extractStringValues() }
        is JsonArray -> flatMap { it.extractStringValues() }
    }
}