package com.tangem.tangemtest.ucase.variants.personalize.ui.presets

import com.tangem.tangemtest.R
import com.tangem.tangemtest.ucase.domain.paramsManager.ItemsManager
import com.tangem.tangemtest.ucase.variants.personalize.PersonalizationConfigStore
import com.tangem.tangemtest.ucase.variants.personalize.converter.PersonalizationConfigConverter
import com.tangem.tangemtest.ucase.variants.personalize.dto.PersonalizationConfig

class PersonalizationPresetManager(
        private val itemsManager: ItemsManager,
        private val store: PersonalizationConfigStore,
        private val view: PersonalizationPresetView
) {

    fun resetToDefault() {
        val config = PersonalizationConfig.default()
        val converter = PersonalizationConfigConverter()
        itemsManager.updateByItemList(converter.convert(config))
        store.save(config)
    }

    fun loadPreset() {
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

    fun savePreset() {
        view.showSavePresetDialog { name ->
            val converter = PersonalizationConfigConverter()
            val config = converter.convert(itemsManager.getItems(), PersonalizationConfig.default())
            store.save(name, config)
        }
    }
}