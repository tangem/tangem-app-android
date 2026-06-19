package com.tangem.features.addressbook.list.model

import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.features.addressbook.list.DefaultAddressBookListComponent
import com.tangem.features.addressbook.list.state.AddressBookListStateController
import com.tangem.features.addressbook.list.state.transformers.UpdateAddressBookListInitialStateTransformer
import com.tangem.features.addressbook.list.ui.state.AddressBookListUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@ModelScoped
internal class AddressBookListModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val stateController: AddressBookListStateController,
) : Model() {

    private val params = paramsContainer.require<DefaultAddressBookListComponent.Params>()

    val state: StateFlow<AddressBookListUM> get() = stateController.uiState

    init {
        stateController.update(
            UpdateAddressBookListInitialStateTransformer(onAddContactClick = params.onAddContactClick),
        )
    }
}