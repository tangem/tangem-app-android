package com.tangem.feature.tester.presentation.testpush.utils

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

internal object TestPushPayloadParser {

    const val NOTIFICATION_KEY_PREFIX = "gcm.n."

    private const val MESSAGE_KEY = "message"
    private const val DATA_KEY = "data"
    private const val NOTIFICATION_KEY = "notification"

    private const val NOTIFICATION_ENABLED_KEY = "gcm.n.e"
    private const val NOTIFICATION_ENABLED_VALUE = "1"

    fun parse(rawPayload: String): Either<String, Map<String, String>> {
        val root = runCatching { Json.parseToJsonElement(rawPayload) }
            .getOrElse { return "Invalid JSON: ${it.message}".left() }

        val rootObject = root as? JsonObject ?: return "Payload must be a JSON object".left()
        val source = rootObject[MESSAGE_KEY] as? JsonObject ?: rootObject

        val data = source[DATA_KEY] as? JsonObject
        val notification = source[NOTIFICATION_KEY] as? JsonObject

        if (data == null && notification == null) return source.toDataMap().right()

        return buildMap {
            data?.let { putAll(it.toDataMap()) }
            notification?.let { putAll(it.toNotificationMap()) }
        }.right()
    }

    private fun JsonObject.toDataMap(): Map<String, String> = entries.associate { (key, value) ->
        key to value.asPushValue()
    }

    private fun JsonObject.toNotificationMap(): Map<String, String> = buildMap {
        put(NOTIFICATION_ENABLED_KEY, NOTIFICATION_ENABLED_VALUE)
        this@toNotificationMap.forEach { (key, value) ->
            put(NOTIFICATION_KEY_PREFIX + key, value.asPushValue())
        }
    }

    private fun JsonElement.asPushValue(): String = (this as? JsonPrimitive)?.content ?: toString()
}