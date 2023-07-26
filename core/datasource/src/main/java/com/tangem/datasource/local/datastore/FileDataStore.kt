package com.tangem.datasource.local.datastore

import com.squareup.moshi.JsonAdapter
import com.tangem.datasource.files.FileReader
import com.tangem.datasource.local.datastore.model.WriteTrigger
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import timber.log.Timber

internal class FileDataStore<Data : Any>(
    private val fileNameProvider: (key: String) -> String,
    private val fileReader: FileReader,
    private val adapter: JsonAdapter<Data>,
) {

    private val writeTrigger = MutableSharedFlow<WriteTrigger>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    fun get(key: String): Flow<Data> {
        return writeTrigger
            .onEmpty { emit(WriteTrigger) }
            .map { getInternal(fileNameProvider(key)) }
            .filterNotNull()
    }

    fun getSyncOrNull(key: String): Data? {
        return getInternal(fileNameProvider(key))
    }

    fun store(key: String, content: Data) {
        val fileName = fileNameProvider(key)

        try {
            val json = adapter.toJson(content)

            fileReader.rewriteFile(json, fileName)
            writeTrigger.tryEmit(WriteTrigger)
        } catch (e: Throwable) {
            Timber.e(e, "Unable to write file: $fileName")
        }
    }

    private fun getInternal(fileName: String): Data? {
        return try {
            val json = fileReader.readFile(fileName)

            adapter.fromJson(json)
        } catch (e: Throwable) {
            Timber.e(e, "Unable to read file: $fileName")
            null
        }
    }
}