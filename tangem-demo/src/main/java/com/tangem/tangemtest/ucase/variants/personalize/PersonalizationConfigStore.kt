package com.tangem.tangemtest.ucase.variants.personalize

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.google.gson.Gson
import com.tangem.tangemtest.AppTangemDemo
import com.tangem.tangemtest.commons.KeyedStore
import com.tangem.tangemtest.commons.Store
import com.tangem.tangemtest.ucase.variants.personalize.dto.PersonalizationConfig

class PersonalizationConfigStore(context: Context) : Store<PersonalizationConfig>, KeyedStore<PersonalizationConfig> {

    companion object {
        val defaultKey = "default"
    }

    private val sharedPreferencesKey = "personalization_presets"

    private val sp: SharedPreferences = (context.applicationContext as AppTangemDemo).sharedPreferences(sharedPreferencesKey)
    private val gson: Gson = Gson()

    override fun save(value: PersonalizationConfig) {
        save(defaultKey, value)
    }

    override fun restore(): PersonalizationConfig = restore(defaultKey)

    override fun save(key: String, value: PersonalizationConfig) {
        sp.edit(true) { putString(key, toJson(value)) }
    }

    override fun restore(key: String): PersonalizationConfig {
        val json = sp.getString(key, toJson(getDefaultConfig()))
        return fromJson(json!!)
    }

    override fun restoreAll(): MutableMap<String, PersonalizationConfig> {
        val map = mutableMapOf<String, PersonalizationConfig>()
        sp.all.forEach { map[it.key] = fromJson(it.value as String) }
        return map.toSortedMap()
    }

    override fun delete(key: String) {
        sp.edit(true) { remove(key) }
    }

    private fun getDefaultConfig(): PersonalizationConfig = PersonalizationConfig.default()

    private fun toJson(value: PersonalizationConfig): String = gson.toJson(value)

    private fun fromJson(json: String): PersonalizationConfig = gson.fromJson(json, PersonalizationConfig::class.java)
}