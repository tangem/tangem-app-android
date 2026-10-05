package com.tangem.test.core.datastore

import androidx.datastore.core.DataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.KSerializer
import kotlinx.serialization.serializer
import java.io.File
import java.nio.file.Files

/**
 * Reads [json] as a DataStore file through the fileName overload of [createAppDataStoreFactory], i.e. with the
 * production JSON config. A file that config cannot parse is replaced with [defaultValue], so pass a default that
 * differs from the expected value.
 */
fun <T> readStoredJson(json: String, defaultValue: T, serializer: KSerializer<T>): T {
    return withStore(defaultValue, serializer) { store, file ->
        file.writeText(json)
        store.data.first()
    }
}

/** Writes [value] through the fileName overload of [createAppDataStoreFactory] and returns the file content. */
fun <T> writeStoredJson(value: T, defaultValue: T, serializer: KSerializer<T>): String {
    return withStore(defaultValue, serializer) { store, file ->
        store.updateData { value }
        file.readText()
    }
}

/** [readStoredJson] for `@Serializable` [T]. */
inline fun <reified T> readStoredJson(json: String, defaultValue: T): T {
    return readStoredJson(json = json, defaultValue = defaultValue, serializer = serializer())
}

/** [writeStoredJson] for `@Serializable` [T]. */
inline fun <reified T> writeStoredJson(value: T, defaultValue: T): String {
    return writeStoredJson(value = value, defaultValue = defaultValue, serializer = serializer())
}

private fun <T, R> withStore(
    defaultValue: T,
    serializer: KSerializer<T>,
    block: suspend (store: DataStore<T>, file: File) -> R,
): R {
    val dir = Files.createTempDirectory("stored_json").toFile()
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    return try {
        val store = createAppDataStoreFactory(filesDir = dir, appScope = scope)
            .create(defaultValue = defaultValue, serializer = serializer, fileName = FILE_NAME)
        runBlocking { block(store, File(dir, FILE_NAME)) }
    } finally {
        scope.cancel()
        dir.deleteRecursively()
    }
}

private const val FILE_NAME = "stored_json"