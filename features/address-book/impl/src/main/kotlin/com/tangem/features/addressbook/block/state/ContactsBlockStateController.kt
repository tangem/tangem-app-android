package com.tangem.features.addressbook.block.state

import com.tangem.core.decompose.di.ModelScoped
import com.tangem.features.addressbook.block.ui.state.ContactsBlockUM
import com.tangem.utils.transformer.Transformer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@ModelScoped
internal class ContactsBlockStateController @Inject constructor() {

    val uiState: StateFlow<ContactsBlockUM>
        field = MutableStateFlow<ContactsBlockUM>(value = ContactsBlockUM.Hidden)

    fun update(transformer: Transformer<ContactsBlockUM>) {
        uiState.update(function = transformer::transform)
    }
}