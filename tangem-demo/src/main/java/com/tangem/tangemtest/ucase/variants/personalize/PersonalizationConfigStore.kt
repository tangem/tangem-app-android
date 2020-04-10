package com.tangem.tangemtest.ucase.variants.personalize

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.google.gson.Gson
import com.tangem.tangemtest.AppTangemDemo
import com.tangem.tangemtest.commons.Store
import com.tangem.tangemtest.ucase.variants.personalize.dto.PersonalizationConfig

class PersonalizationConfigStore(context: Context) : Store<PersonalizationConfig> {

    private val key = "personalization_config"

    private val sp: SharedPreferences = (context.applicationContext as AppTangemDemo).sharedPreferences()
    private val gson: Gson = Gson()

    override fun save(config: PersonalizationConfig) {
        sp.edit(true) { putString(key, gson.toJson(config)) }
    }

    override fun restore(): PersonalizationConfig {
        val json = sp.getString(key, gson.toJson(PersonalizationConfig()))
        return gson.fromJson(json, PersonalizationConfig::class.java)
    }
}