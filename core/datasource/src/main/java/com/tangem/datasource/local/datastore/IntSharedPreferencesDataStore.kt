package com.tangem.datasource.local.datastore

import android.content.Context
import androidx.core.content.edit

internal class IntSharedPreferencesDataStore(
    preferencesName: String,
    context: Context,
) : SharedPreferencesDataStore<Int>(preferencesName, context) {

    override fun getByKey(key: String): Int = sharedPreferences.getInt(key, 0)

    override fun storeByKey(key: String, value: Int) {
        sharedPreferences.edit { putInt(key, value) }
    }
}