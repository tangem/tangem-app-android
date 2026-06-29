package com.tangem.features.addressbook.addressselector

import androidx.compose.runtime.Composable
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.features.addressbook.AddressSelectorComponent
import com.tangem.features.addressbook.addressselector.ui.AddressSelectorBottomSheet
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultAddressSelectorComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    @Assisted private val params: AddressSelectorComponent.Params,
) : AddressSelectorComponent, AppComponentContext by appComponentContext {

    override fun dismiss() = params.onDismiss()

    @Composable
    override fun BottomSheet() {
        AddressSelectorBottomSheet(
            contact = params.contact,
            onAddressClick = { entry -> params.onAddressSelected(params.contact.toSelectedContact(entry)) },
            onDismiss = ::dismiss,
        )
    }

    @AssistedFactory
    interface Factory : AddressSelectorComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: AddressSelectorComponent.Params,
        ): DefaultAddressSelectorComponent
    }
}