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
class PersonalizeViewModelFactory(private val jsonPersonalizeString: String) : ViewModelProvider.NewInstanceFactory() {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T = PersonalizeViewModel(jsonPersonalizeString) as T
}

class PersonalizeViewModel(private val jsonPersonalizeString: String) : ViewModel() {

    val ldBlockList: MutableLiveData<List<Block>> by lazy { MutableLiveData(initBlockList()) }

    private fun initBlockList(): List<Block> = parseJsonToBlockList(jsonPersonalizeString)

    fun parseJsonToBlockList(jsonString: String): List<Block> {
        val config = Gson().fromJson(jsonString, PersonalizeConfig::class.java)
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