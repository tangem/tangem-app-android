package com.tangem.tangemtest.card_use_cases.ui.personalize

import android.view.View
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.gson.Gson
import com.tangem.tangemtest._arch.structure.base.Block
import com.tangem.tangemtest._arch.structure.base.DataUnit
import com.tangem.tangemtest.card_use_cases.ui.personalize.converter.BlockToJsonConverter
import com.tangem.tangemtest.card_use_cases.ui.personalize.converter.JsonBlockEnDe
import com.tangem.tangemtest.card_use_cases.ui.personalize.converter.JsonToBlockConverter
import com.tangem.tangemtest.card_use_cases.ui.personalize.converter.TestJsonDto

/**
* [REDACTED_AUTHOR]
 */
class PersonalizeViewModelFactory(private val jsonPersonalizeString: String) : ViewModelProvider.NewInstanceFactory() {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T = PersonalizeViewModel(jsonPersonalizeString) as T
}

class PersonalizeViewModel(private val jsonPersonalizeString: String) : ViewModel() {

    val ldBlockList: MutableLiveData<List<Block>> by lazy { MutableLiveData(readJson()) }

    private fun readJson(): List<Block> {
        val enDe = JsonBlockEnDe(JsonToBlockConverter(), BlockToJsonConverter())
        val jsonDto = Gson().fromJson(jsonPersonalizeString, TestJsonDto::class.java)
        return enDe.decode(jsonDto)
    }

    fun toggleDescriptionVisibility(state: Boolean) {
        ldBlockList.value?.forEach { block ->
            block.unitList.forEach { unit ->
                val vm = unit as? DataUnit<*> ?: return@forEach
                vm.viewModel?.viewState?.descriptionVisibility = if (state) View.VISIBLE else View.GONE
            }
        }
    }
}