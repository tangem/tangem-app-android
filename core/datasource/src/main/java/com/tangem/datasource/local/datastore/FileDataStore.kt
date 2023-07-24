package com.tangem.datasource.local.datastore

import com.squareup.moshi.JsonAdapter
import com.tangem.datasource.files.FileReader
import com.tangem.domain.core.error.DataError
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map

internal class FileDataStore<Data>(
    private val fileNameProvider: (key: String) -> String,
    private val fileReader: FileReader,
    private val adapter: JsonAdapter<Data>,
) {

    private val writeTrigger = MutableSharedFlow<Unit>(
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    fun get(key: String): Flow<Data> {
        return writeTrigger
            .map { getInternal(fileNameProvider(key)) }
            .filterNotNull()
    }

    fun getSync(key: String): Data? {
        return getInternal(fileNameProvider(key))
    }

    fun store(key: String, content: Data) {
        try {
            val json = adapter.toJson(content)

            fileReader.rewriteFile(json, fileNameProvider(key))
            writeTrigger.tryEmit(Unit)
        } catch (e: Throwable) {
            throw DataError.PersistenceError.UnableToWriteFile(e)
        }
    }

    private fun getInternal(fileName: String): Data? {
        return try {
            val json = fileReader.readFile(fileName)

            adapter.fromJson(json)
        } catch (e: Throwable) {
            throw DataError.PersistenceError.UnableToReadFile(e)
        }
    }
}
