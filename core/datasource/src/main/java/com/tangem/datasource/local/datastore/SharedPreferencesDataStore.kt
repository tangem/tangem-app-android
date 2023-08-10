package com.tangem.datasource.local.datastore

import android.content.Context
import android.content.Context.MODE_PRIVATE
import androidx.core.content.edit
import com.squareup.moshi.JsonAdapter
import com.tangem.datasource.local.datastore.core.StringKeyDataStore
import com.tangem.datasource.local.datastore.model.WriteTrigger
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import timber.log.Timber

internal class SharedPreferencesDataStore<Value : Any>(
    preferencesName: String,
    private val context: Context,
    private val adapter: JsonAdapter<Value>,
) : StringKeyDataStore<Value> {

    private val sharedPreferences by lazy {
        context.getSharedPreferences(preferencesName, MODE_PRIVATE)
    }

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

            sharedPreferences.edit { putString(key, json) }
            writeTrigger.emit(WriteTrigger)
        } catch (e: Throwable) {
            Timber.e(e, "Unable to edit preferences: $key")
        }
    }

    override suspend fun store(items: Map<String, Value>) {
        items.forEach { (key, item) ->
            store(key, item)
        }
    }

    override suspend fun remove(key: String) {
        sharedPreferences.edit {
            remove(key)
        }
    }

    override suspend fun clear() {
        sharedPreferences.edit { clear() }
    }

    private fun getInternal(key: String): Value? {
        return try {
            val json = sharedPreferences.getString(key, null) ?: return null

            adapter.fromJson(json)
        } catch (e: Throwable) {
            Timber.e(e, "Unable to get value from preferences: $key")
            null
        }
    }
}