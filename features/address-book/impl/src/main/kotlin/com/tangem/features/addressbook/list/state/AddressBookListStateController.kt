package com.tangem.features.addressbook.list.state

import com.tangem.core.decompose.di.ModelScoped
import com.tangem.features.addressbook.list.ui.state.AddressBookListUM
import com.tangem.utils.transformer.Transformer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@ModelScoped
internal class AddressBookListStateController @Inject constructor() {

    val uiState: StateFlow<AddressBookListUM>
        field = MutableStateFlow(value = getInitialState())

    fun update(transformer: Transformer<AddressBookListUM>) {
        uiState.update(function = transformer::transform)
    }

    private fun getInitialState(): AddressBookListUM = AddressBookListUM.Empty(onAddClick = {})
}