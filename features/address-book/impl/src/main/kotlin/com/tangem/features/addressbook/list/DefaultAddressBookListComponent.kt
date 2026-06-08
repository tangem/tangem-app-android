package com.tangem.features.addressbook.list

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.features.addressbook.list.contract.AddressBookListEvent
import com.tangem.features.addressbook.list.contract.AddressBookListUM
import com.tangem.features.addressbook.list.model.AddressBookListModel
import com.tangem.features.addressbook.list.ui.AddressBookEmptyScreen
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultAddressBookListComponent @AssistedInject constructor(
    @Assisted context: AppComponentContext,
    @Assisted params: AddressBookListComponent.Params,
) : AddressBookListComponent, AppComponentContext by context {

    private val model: AddressBookListModel = getOrCreateModel(params)

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.state.collectAsStateWithLifecycle()
        when (state) {
            AddressBookListUM.Empty -> AddressBookEmptyScreen(
                onAddContactClick = { model.onAction(event = AddressBookListEvent.NewContactClick) },
                modifier = modifier,
            )
        }
    }

    @AssistedFactory
    interface Factory : AddressBookListComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: AddressBookListComponent.Params,
        ): DefaultAddressBookListComponent
    }
}