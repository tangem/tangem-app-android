package com.tangem.tangemtest.ucase.variants.personalize.ui

import android.view.View
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.gson.Gson
import com.tangem.tangemtest._arch.structure.abstraction.BaseItem
import com.tangem.tangemtest._arch.structure.abstraction.Block
import com.tangem.tangemtest.ucase.variants.personalize.converter.BlockToJsonConverter
import com.tangem.tangemtest.ucase.variants.personalize.converter.JsonToBlockConverter
import com.tangem.tangemtest.ucase.variants.personalize.converter.ValueMapper
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
        return JsonToBlockConverter().convert(config)
    }

    fun prepareJson(blocList: List<Block>): String {
        val jsonDto = BlockToJsonConverter(ValueMapper(), PersonalizeConfig()).convert(blocList)
        return Gson().toJson(jsonDto)
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