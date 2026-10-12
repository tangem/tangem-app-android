package com.tangem.datasource.local.preferences.utils

import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.json.Json

fun <T> Preferences.getJsonOrNull(
    key: Preferences.Key<String>,
    json: Json,
    deserializer: DeserializationStrategy<T>,
): T? {
    val encoded = this[key] ?: return null

    return runCatching { json.decodeFromString(deserializer, encoded) }
        .onFailure { TangemLogger.e("Preference [${key.name}] cannot be decoded and is treated as absent", it) }
        .getOrNull()
}

fun <T> MutablePreferences.setJson(
    key: Preferences.Key<String>,
    json: Json,
    serializer: SerializationStrategy<T>,
    value: T,
) {
    this[key] = json.encodeToString(serializer, value)
}

suspend fun <T> AppPreferencesStore.getJsonSyncOrNull(
    key: Preferences.Key<String>,
    json: Json,
    deserializer: DeserializationStrategy<T>,
): T? = withContext(dispatchers.io) {
    data.firstOrNull()?.getJsonOrNull(key, json, deserializer)
}