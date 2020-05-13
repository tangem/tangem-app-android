package com.tangem.devkit.ucase.variants.personalize.ui.presets

import com.tangem.devkit.R
import com.tangem.devkit.ucase.domain.paramsManager.ItemsManager
import com.tangem.devkit.ucase.variants.personalize.PersonalizationConfigStore
import com.tangem.devkit.ucase.variants.personalize.converter.PersonalizationConfigConverter
import com.tangem.devkit.ucase.variants.personalize.converter.PersonalizationJsonConverter
import com.tangem.devkit.ucase.variants.personalize.dto.PersonalizationConfig
import com.tangem.devkit.ucase.variants.personalize.dto.PersonalizationJson

class PersonalizationPresetManager(
        private val itemsManager: ItemsManager,
        private val view: PersonalizationPresetView
) {

    fun resetToDefault(store: PersonalizationConfigStore) {
        val config = PersonalizationConfig.default()
        val converter = PersonalizationConfigConverter()
        itemsManager.updateByItemList(converter.convert(config))
        store.save(config)
    }

    fun loadPreset(store: PersonalizationConfigStore) {
        val presets = store.restoreAll()
        presets.remove(PersonalizationConfigStore.defaultKey)
        val namesList = presets.map { it.key }.toMutableList()
        if (namesList.isEmpty()) {
            view.showSnackbar(R.string.error_nothing_to_load)
            return
        }

        view.showLoadPresetDialog(namesList, {
            val converter = PersonalizationConfigConverter()
            val config = store.restore(it)
            itemsManager.updateByItemList(converter.convert(config))
        }, {
            store.delete(it)
        })
    }

    fun savePreset(store: PersonalizationConfigStore) {
        view.showSavePresetDialog { name ->
            val converter = PersonalizationConfigConverter()
            val config = converter.convert(itemsManager.getItems(), PersonalizationConfig.default())
            store.save(name, config)
        }
    }

    fun importJsonConfig(jsonString: String) {
        if (jsonString.isEmpty()) return

        val jsonDto = try {
            PersonalizationJson.getJsonConverter().fromJson(jsonString, PersonalizationJson::class.java)
        } catch (ex: Exception) {
            view.showSnackbar("Can't convert imported string to Json object. Error: $ex")
            return
        }

        val config = PersonalizationJsonConverter().aToB(jsonDto)
        val converter = PersonalizationConfigConverter()
        itemsManager.updateByItemList(converter.convert(config))
    }

    fun exportJsonConfig(): String {
        val converter = PersonalizationConfigConverter()
        val config = converter.convert(itemsManager.getItems(), PersonalizationConfig.default())
        val jsonDto = PersonalizationJsonConverter().bToA(config)
        val jsonString = PersonalizationJson.getJsonConverter().toJson(jsonDto)
        return jsonString
    }
}