package com.tangem.features.send.subcomponents.destination

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.slot.childSlot
import com.arkivanov.decompose.router.slot.dismiss
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.child
import com.tangem.core.decompose.context.childByContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.features.addressbook.AddressBookContactsBlockComponent
import com.tangem.features.addressbook.AddressBookFeatureToggles
import com.tangem.features.addressbook.AddressSelectorComponent
import com.tangem.features.send.api.subcomponents.destination.SendDestinationComponent
import com.tangem.features.send.api.subcomponents.destination.SendDestinationComponentParams
import com.tangem.features.send.api.subcomponents.destination.entity.DestinationUM
import com.tangem.features.send.subcomponents.destination.model.SendDestinationModel
import com.tangem.features.send.subcomponents.destination.ui.SendDestinationContent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultSendDestinationComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    @Assisted private val params: SendDestinationComponentParams.DestinationParams,
    addressBookFeatureToggles: AddressBookFeatureToggles,
    contactsBlockFactory: AddressBookContactsBlockComponent.Factory,
    addressSelectorFactory: AddressSelectorComponent.Factory,
) : SendDestinationComponent, AppComponentContext by appComponentContext {

    private val model: SendDestinationModel = getOrCreateModel(params = params)

    private val contactsBlock: AddressBookContactsBlockComponent? by lazy {
        if (addressBookFeatureToggles.isAddressBookEnabled) {
            contactsBlockFactory.create(
                context = child("send_contacts_block"),
                params = AddressBookContactsBlockComponent.Params(
                    userWalletId = params.userWalletId,
                    network = params.cryptoCurrency.network,
                    queryFlow = model.addressQuery,
                    onContactClick = model::onContactClick,
                    onSeeAllClick = model::onSeeAllContactsClick,
                ),
            )
        } else {
            null
        }
    }

    private val addressSelectorSlot = childSlot(
        source = model.addressSelectorNavigation,
        serializer = null,
        key = "send_address_selector_slot",
        handleBackButton = true,
        childFactory = { contact, componentContext ->
            addressSelectorFactory.create(
                context = childByContext(componentContext),
                params = AddressSelectorComponent.Params(
                    contact = contact,
                    onAddressSelected = model::applySelectedContact,
                    onDismiss = { model.addressSelectorNavigation.dismiss() },
                ),
            )
        },
    )

    override fun updateState(destinationUM: DestinationUM) = model.updateState(destinationUM)

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()
        val isBalanceHidden by params.isBalanceHidingFlow.collectAsStateWithLifecycle()
        val selector by addressSelectorSlot.subscribeAsState()

        SendDestinationContent(
            state = state,
            clickIntents = model,
            isBalanceHidden = isBalanceHidden,
            contactsBlock = contactsBlock,
        )
        selector.child?.instance?.BottomSheet()
    }

    @AssistedFactory
    interface Factory : SendDestinationComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: SendDestinationComponentParams.DestinationParams,
        ): DefaultSendDestinationComponent
    }
}