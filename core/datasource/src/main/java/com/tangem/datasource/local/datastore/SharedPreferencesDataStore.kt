package com.tangem.datasource.local.datastore

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import androidx.core.content.edit
import com.squareup.moshi.JsonAdapter
import com.tangem.datasource.local.datastore.core.StringKeyDataStore
import com.tangem.datasource.local.datastore.utils.Trigger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import timber.log.Timber

internal class SharedPreferencesDataStore<Value : Any>(
    preferencesName: String,
    private val context: Context,
    private val adapter: JsonAdapter<Value>,
) : StringKeyDataStore<Value> {

    private val sharedPreferences: SharedPreferences by lazy {
        context.getSharedPreferences(preferencesName, MODE_PRIVATE)
    }

    private val writeTrigger = Trigger()

    override fun get(key: String): Flow<Value> {
        return writeTrigger
            .map { getInternal(key) }
            .filterNotNull()
            .distinctUntilChanged()
    }

    override fun getAll(): Flow<List<Value>> {
        return writeTrigger
            .map { getAllInternal() }
            .distinctUntilChanged()
    }

    override suspend fun getSyncOrNull(key: String): Value? {
        return getInternal(key)
    }

    override suspend fun getAllSyncOrNull(): List<Value> {
        return getAllInternal()
    }

    override suspend fun store(key: String, item: Value) {
        try {
            val json = adapter.toJson(item)

            sharedPreferences.edit { putString(key, json) }
            writeTrigger.trigger()
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
        sharedPreferences.edit { remove(key) }
        writeTrigger.trigger()
    }

    override suspend fun clear() {
        sharedPreferences.edit { clear() }
        writeTrigger.trigger()
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

    private fun getAllInternal(): List<Value> {
        return sharedPreferences.all.mapNotNull { (key, value) ->
            try {
                val json = value as? String ?: return@mapNotNull null

                adapter.fromJson(json)
            } catch (e: Throwable) {
                Timber.e(e, "Unable to convert value from JSON: $key")
                null
            }
        }
    }
}