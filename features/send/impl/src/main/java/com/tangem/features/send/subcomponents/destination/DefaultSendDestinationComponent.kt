package com.tangem.features.send.subcomponents.destination

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
import com.tangem.core.ui.components.PrimaryButton
import com.tangem.core.ui.components.appbar.AppBarWithBackButtonAndIcon
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.send.api.subcomponents.destination.SendDestinationComponent
import com.tangem.features.send.api.subcomponents.destination.SendDestinationComponentParams
import com.tangem.features.send.api.subcomponents.destination.entity.DestinationUM
import com.tangem.features.send.impl.R
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
    override fun Title() {
        BackHandler(onBack = model::onBackClick)
        AppBarWithBackButtonAndIcon(
            text = params.title.resolveReference(),
            onBackClick = model::onBackClick,
            backIconRes = if (params.route.isEditMode) {
                R.drawable.ic_back_24
            } else {
                R.drawable.ic_close_24
            },
            backgroundColor = TangemTheme.colors.background.tertiary,
            modifier = Modifier.height(TangemTheme.dimens.size56),
        )
    }

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

    @Composable
    override fun Footer() {
        val state by model.uiState.collectAsStateWithLifecycle()
        PrimaryButton(
            text = if (params.route.isEditMode) {
                stringResourceSafe(R.string.common_continue)
            } else {
                stringResourceSafe(R.string.common_next)
            },
            enabled = state.isPrimaryButtonEnabled,
            onClick = {
                model.saveResult()
                params.callback.onNextClick(params.route)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    bottom = 16.dp,
                ),
        )
    }

    @AssistedFactory
    interface Factory : SendDestinationComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: SendDestinationComponentParams.DestinationParams,
        ): DefaultSendDestinationComponent
    }
}