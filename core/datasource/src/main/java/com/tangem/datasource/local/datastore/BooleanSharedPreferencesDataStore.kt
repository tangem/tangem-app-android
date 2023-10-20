package com.tangem.datasource.local.datastore

import android.content.Context
import androidx.core.content.edit

internal class BooleanSharedPreferencesDataStore(
    preferencesName: String,
    context: Context,
) : SharedPreferencesDataStore<Boolean>(preferencesName, context) {

    override fun getByKey(key: String): Boolean = sharedPreferences.getBoolean(key, false)

    override fun storeByKey(key: String, value: Boolean) {
        sharedPreferences.edit { putBoolean(key, value) }
    }
}