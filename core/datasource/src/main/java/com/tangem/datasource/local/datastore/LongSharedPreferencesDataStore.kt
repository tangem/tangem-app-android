package com.tangem.datasource.local.datastore

import android.content.Context
import androidx.core.content.edit

internal class LongSharedPreferencesDataStore(
    preferencesName: String,
    context: Context,
) : SharedPreferencesDataStore<Long>(preferencesName, context) {

    override fun getByKey(key: String): Long = sharedPreferences.getLong(key, 0)

    override fun storeByKey(key: String, value: Long) {
        sharedPreferences.edit { putLong(key, value) }
    }
}