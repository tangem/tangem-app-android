package com.tangem.features.addressbook.list.state

import com.tangem.core.decompose.di.ModelScoped
import com.tangem.features.addressbook.list.ui.state.AddressBookListUM
import com.tangem.utils.transformer.Transformer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@ModelScoped
internal class AddressBookListStateController @Inject constructor() {

    private val mutableUiState: MutableStateFlow<AddressBookListUM> =
        MutableStateFlow(value = getInitialState())

    val uiState: StateFlow<AddressBookListUM> get() = mutableUiState.asStateFlow()

    fun update(transformer: Transformer<AddressBookListUM>) {
        mutableUiState.update(function = transformer::transform)
    }

    private fun getInitialState(): AddressBookListUM = AddressBookListUM.Empty(onAddClick = {})
}