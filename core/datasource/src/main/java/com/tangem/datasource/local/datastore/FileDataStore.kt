package com.tangem.datasource.local.datastore

import com.squareup.moshi.JsonAdapter
import com.tangem.datasource.files.FileReader
import com.tangem.datasource.local.datastore.core.StringKeyDataStore
import com.tangem.datasource.local.datastore.model.WriteTrigger
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import timber.log.Timber

internal class FileDataStore<Value : Any>(
    private val fileReader: FileReader,
    private val adapter: JsonAdapter<Value>,
) : StringKeyDataStore<Value> {

    private val writeTrigger = MutableSharedFlow<WriteTrigger>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    override fun get(key: String): Flow<Value> {
        return writeTrigger
            .onEmpty { emit(WriteTrigger) }
            .map { getInternal(key) }
            .filterNotNull()
    }

    override suspend fun getSyncOrNull(key: String): Value? {
        return getInternal(key)
    }

    override suspend fun store(key: String, item: Value) {
        try {
            val json = adapter.toJson(item)

            fileReader.rewriteFile(json, key)
            writeTrigger.tryEmit(WriteTrigger)
        } catch (e: Throwable) {
            Timber.e(e, "Unable to write file: $key")
        }
    }

    override suspend fun store(items: Map<String, Value>) {
        items.forEach { (key, item) ->
            store(key, item)
        }
    }

    override suspend fun remove(key: String) {
        fileReader.removeFile(key)
    }

    override suspend fun clear() {
// [REDACTED_TODO_COMMENT]
    }

    private fun getInternal(fileName: String): Value? {
        return try {
            val json = fileReader.readFile(fileName)

            adapter.fromJson(json)
        } catch (e: Throwable) {
            Timber.e(e, "Unable to read file: $fileName")
            null
        }
    }
}
