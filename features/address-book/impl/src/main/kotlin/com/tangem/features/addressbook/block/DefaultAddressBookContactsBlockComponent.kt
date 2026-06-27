package com.tangem.features.addressbook.block

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.features.addressbook.AddressBookContactsBlockComponent
import com.tangem.features.addressbook.block.model.ContactsBlockModel
import com.tangem.features.addressbook.block.ui.ContactsBlock
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultAddressBookContactsBlockComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    @Assisted params: AddressBookContactsBlockComponent.Params,
) : AddressBookContactsBlockComponent, AppComponentContext by appComponentContext {

    private val model: ContactsBlockModel = getOrCreateModel(params)

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.state.collectAsStateWithLifecycle()
        ContactsBlock(state = state, modifier = modifier)
    }

    @AssistedFactory
    interface Factory : AddressBookContactsBlockComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: AddressBookContactsBlockComponent.Params,
        ): DefaultAddressBookContactsBlockComponent
    }
}