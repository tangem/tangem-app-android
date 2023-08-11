package com.tangem.datasource.local.datastore

import com.squareup.moshi.JsonAdapter
import com.tangem.datasource.files.FileReader
import com.tangem.datasource.local.datastore.core.StringKeyDataStore
import com.tangem.datasource.local.datastore.utils.Trigger
import kotlinx.coroutines.flow.*
import timber.log.Timber

internal class FileDataStore<Value : Any>(
    private val fileReader: FileReader,
    private val adapter: JsonAdapter<Value>,
) : StringKeyDataStore<Value> {

    private val writeTrigger = Trigger()

    override fun get(key: String): Flow<Value> {
        return writeTrigger
            .map { getInternal(key) }
            .filterNotNull()
            .distinctUntilChanged()
    }

    override fun getAll(): Flow<List<Value>> {
        Timber.e("`getAll()` function not implemented for `FileDataStore`")

        return emptyFlow()
    }

    override suspend fun getSyncOrNull(key: String): Value? {
        return getInternal(key)
    }

    override suspend fun getAllSyncOrNull(): List<Value> {
        Timber.e("`getAllSyncOrNull()` function not implemented for `FileDataStore`")

        return emptyList()
    }

    override suspend fun store(key: String, item: Value) {
        try {
            val json = adapter.toJson(item)

            fileReader.rewriteFile(json, key)
            writeTrigger.trigger()
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
        Timber.e("`clear()` function not implemented for `FileDataStore`")
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
