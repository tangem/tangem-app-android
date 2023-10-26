package com.tangem.datasource.local.datastore

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import androidx.core.content.edit
import com.tangem.datasource.local.datastore.core.StringKeyDataStore
import com.tangem.datasource.local.datastore.utils.Trigger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.mapNotNull
import timber.log.Timber

internal abstract class SharedPreferencesDataStore<Value : Any>(
    preferencesName: String,
    context: Context,
) : StringKeyDataStore<Value> {

    protected val sharedPreferences: SharedPreferences by lazy {
        context.getSharedPreferences(preferencesName, MODE_PRIVATE)
    }

    private val writeTrigger = Trigger()

    abstract fun getByKey(key: String): Value?

    abstract fun storeByKey(key: String, value: Value)

    override suspend fun isEmpty(): Boolean {
        return sharedPreferences.all.isEmpty()
    }

    override suspend fun contains(key: String): Boolean {
        return sharedPreferences.contains(key)
    }

    override fun get(key: String): Flow<Value> {
        return writeTrigger
            .mapNotNull { getInternal(key) }
            .distinctUntilChanged()
    }

    override fun getAll(): Flow<List<Value>> {
        throw UnsupportedOperationException("Unknown key")
    }

    override suspend fun getSyncOrNull(key: String): Value? = getInternal(key)

    override suspend fun getAllSyncOrNull(): List<Value> {
        throw UnsupportedOperationException("Unknown key")
    }

    override suspend fun store(key: String, value: Value) {
        try {
            storeByKey(key, value)
            writeTrigger.trigger()
        } catch (e: Throwable) {
            Timber.e(e, "Unable to edit preferences: $key")
        }
    }

    override suspend fun store(values: Map<String, Value>) {
        values.forEach { (key, item) ->
            store(key, item)
        }
    }

    override suspend fun remove(key: String) {
        sharedPreferences.edit { remove(key) }
        writeTrigger.trigger()
    }

    override suspend fun remove(keys: Collection<String>) {
        sharedPreferences.edit {
            keys.forEach { key ->
                remove(key)
            }
        }

        writeTrigger.trigger()
    }

    override suspend fun clear() {
        sharedPreferences.edit { clear() }
        writeTrigger.trigger()
    }

    private fun getInternal(key: String): Value? {
        if (!sharedPreferences.contains(key)) return null

        return try {
            getByKey(key)
        } catch (e: Throwable) {
            Timber.e(e, "Unable to get value from preferences: $key")
            null
        }
    }
}