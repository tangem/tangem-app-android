package com.tangem.tangemtest.ucase.variants.personalize.ui

import android.view.View
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.gson.Gson
import com.tangem.commands.personalization.CardConfig
import com.tangem.tangemtest._arch.structure.abstraction.BaseItem
import com.tangem.tangemtest._arch.structure.abstraction.Block
import com.tangem.tangemtest.ucase.variants.personalize.converter.PersonalizeConfigConverter
import com.tangem.tangemtest.ucase.variants.personalize.dto.PersonalizeConfig

/**
[REDACTED_AUTHOR]
 */
class PersonalizeViewModelFactory(private val config: PersonalizeConfig) : ViewModelProvider.NewInstanceFactory() {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T = PersonalizeViewModel(config) as T
}

class PersonalizeViewModel(private val config: PersonalizeConfig) : ViewModel() {

    val ldBlockList: MutableLiveData<List<Block>> by lazy { MutableLiveData(initBlockList()) }

    private fun initBlockList(): List<Block> = createBlocksFromConfig(config)

    fun createBlocksFromConfig(config: PersonalizeConfig): List<Block> {
        return PersonalizeConfigConverter().toBlock(config)
    }

    fun createConfig(blocList: List<Block>): PersonalizeConfig {
        return PersonalizeConfigConverter().toConfig(blocList, PersonalizeConfig())
    }

    fun createCardConfig(config: PersonalizeConfig): CardConfig {
        return PersonalizeConfigConverter().createCardConfig(config)
    }

    fun convertToJson(config: PersonalizeConfig): String {
        return Gson().toJson(config)
    }

    fun toggleDescriptionVisibility(state: Boolean) {
        ldBlockList.value?.forEach { block ->
            block.itemList.forEach { item ->
                val vm = item as? BaseItem<*> ?: return@forEach
                vm.viewModel.viewState.descriptionVisibility = if (state) View.VISIBLE else View.GONE
            }
        }
    }
}