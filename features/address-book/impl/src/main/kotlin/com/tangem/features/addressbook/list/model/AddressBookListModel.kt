package com.tangem.features.addressbook.list.model

import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.features.addressbook.list.AddressBookListComponent
import com.tangem.features.addressbook.list.contract.AddressBookListEvent
import com.tangem.features.addressbook.list.contract.AddressBookListUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@ModelScoped
internal class AddressBookListModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
) : Model() {

    private val params: AddressBookListComponent.Params = paramsContainer.require()

    val state: StateFlow<AddressBookListUM> = MutableStateFlow(
        AddressBookListUM.Empty,
    )

    fun onAction(event: AddressBookListEvent) {
        when (event) {
            AddressBookListEvent.NewContactClick -> params
            is AddressBookListEvent.ContactClick -> Unit
            is AddressBookListEvent.ChipClick -> Unit
            is AddressBookListEvent.SearchInput -> Unit
        }
    }
}