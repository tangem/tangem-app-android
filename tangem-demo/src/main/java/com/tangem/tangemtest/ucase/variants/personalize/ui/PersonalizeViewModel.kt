package com.tangem.tangemtest.ucase.variants.personalize.ui

import android.view.View
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.gson.Gson
import com.tangem.commands.personalization.entities.CardConfig
import com.tangem.tangemtest._arch.structure.abstraction.BaseItem
import com.tangem.tangemtest._arch.structure.abstraction.Block
import com.tangem.tangemtest._arch.structure.abstraction.Item
import com.tangem.tangemtest.ucase.variants.personalize.converter.PersonalizeConfigConverter
import com.tangem.tangemtest.ucase.variants.personalize.converter.fromTo.PersonalizeConfigToCardConfig
import com.tangem.tangemtest.ucase.variants.personalize.dto.PersonalizeConfig

/**
[REDACTED_AUTHOR]
 */
class PersonalizeViewModelFactory(private val config: PersonalizeConfig) : ViewModelProvider.NewInstanceFactory() {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T = PersonalizeViewModel(config) as T
}

class PersonalizeViewModel(private val config: PersonalizeConfig) : ViewModel() {

    val ldBlockList: MutableLiveData<List<Item>> by lazy { MutableLiveData(initBlockList()) }
    private val configConverter = PersonalizeConfigConverter()

    private fun initBlockList(): List<Item> = createBlocksFromConfig(config)

    fun createBlocksFromConfig(config: PersonalizeConfig): List<Item> {
        return configConverter.convert(config)
    }

    fun createConfig(blocList: List<Item>): PersonalizeConfig {
        return configConverter.convert(blocList, PersonalizeConfig())
    }

    fun createCardConfig(config: PersonalizeConfig): CardConfig {
        return PersonalizeConfigToCardConfig().convert(config)
    }

    fun convertToJson(config: PersonalizeConfig): String {
        return Gson().toJson(config)
    }

    fun toggleDescriptionVisibility(state: Boolean) {
        ldBlockList.value?.forEach { item ->
            when(item) {
                is BaseItem<*> -> {
                    item.viewModel.viewState.descriptionVisibility = if (state) View.VISIBLE else View.GONE
                }
                is Block -> {
                    item.itemList.forEach { blockItem ->
                        val vm = blockItem as? BaseItem<*> ?: return@forEach
                        vm.viewModel.viewState.descriptionVisibility = if (state) View.VISIBLE else View.GONE
                    }
                }
            }

        }
    }
}