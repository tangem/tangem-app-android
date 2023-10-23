package com.tangem.datasource.local.datastore

import android.content.Context
import androidx.core.content.edit
import com.squareup.moshi.JsonAdapter

internal class JsonSharedPreferencesDataStore<Value : Any>(
    preferencesName: String,
    context: Context,
    private val adapter: JsonAdapter<Value>,
) : SharedPreferencesDataStore<Value>(preferencesName, context) {

    override fun getByKey(key: String): Value? {
        val json = sharedPreferences.getString(key, null) ?: return null

        return adapter.fromJson(json)
    }

    override fun storeByKey(key: String, value: Value) {
        val json = adapter.toJson(value)

        sharedPreferences.edit { putString(key, json) }
    }
}