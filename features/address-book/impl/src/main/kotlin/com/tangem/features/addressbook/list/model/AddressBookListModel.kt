package com.tangem.features.addressbook.list.model

import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.features.addressbook.list.contract.AddressBookListUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@ModelScoped
internal class AddressBookListModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
) : Model() {

    val state: StateFlow<AddressBookListUM> = MutableStateFlow(
        AddressBookListUM.Empty,
    )
}