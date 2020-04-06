package com.tangem.tangemtest.ucase.variants.personalize.ui

import android.view.View
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.tangem.tangemtest._arch.structure.abstraction.BaseItem
import com.tangem.tangemtest._arch.structure.abstraction.Item
import com.tangem.tangemtest._arch.structure.abstraction.iterate
import com.tangem.tangemtest.ucase.variants.personalize.converter.PersonalizeConfigConverter
import com.tangem.tangemtest.ucase.variants.personalize.dto.PersonalizeConfig

/**
[REDACTED_AUTHOR]
 */
class PersonalizeViewModelFactory(private val config: PersonalizeConfig) : ViewModelProvider.NewInstanceFactory() {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T = PersonalizeViewModel(config) as T
}

class PersonalizeViewModel(private val config: PersonalizeConfig) : ViewModel() {

    val ldBlockList: MutableLiveData<List<Item>> by lazy { MutableLiveData(initBlockList()) }

    private fun initBlockList(): List<Item> = PersonalizeConfigConverter().convert(config)

    fun toggleDescriptionVisibility(state: Boolean) {
        ldBlockList.value?.iterate {
            val baseItem = it as? BaseItem<*> ?: return@iterate
            baseItem.viewModel.viewState.descriptionVisibility = if (state) View.VISIBLE else View.GONE
        }
    }
}