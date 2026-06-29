package com.tangem.features.addressbook.list

import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.slot.childSlot
import com.arkivanov.decompose.router.slot.dismiss
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.childByContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.addressbook.AddressSelectorComponent
import com.tangem.features.addressbook.list.model.AddressBookListModel
import com.tangem.features.addressbook.list.ui.AddressBookEmptyScreen
import com.tangem.features.addressbook.list.ui.AddressBookListScreen
import com.tangem.features.addressbook.list.ui.state.AddressBookListUM
import com.tangem.features.addressbook.route.AddressBookRoute

internal class DefaultAddressBookListComponent(
    appComponentContext: AppComponentContext,
    params: Params,
    addressSelectorFactory: AddressSelectorComponent.Factory,
) : ComposableContentComponent, AppComponentContext by appComponentContext {

    private val model: AddressBookListModel = getOrCreateModel(params)

    private val selectorSlot = childSlot(
        source = model.selectorNavigation,
        serializer = null,
        key = "address_selector_slot",
        handleBackButton = true,
        childFactory = { contact, componentContext ->
            addressSelectorFactory.create(
                context = childByContext(componentContext),
                params = AddressSelectorComponent.Params(
                    contact = contact,
                    onAddressSelected = model::deliverSelection,
                    onDismiss = { model.selectorNavigation.dismiss() },
                ),
            )
        },
    )

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.state.collectAsStateWithLifecycle()
        val selector by selectorSlot.subscribeAsState()
        when (val addressBookListUM = state) {
            is AddressBookListUM.Empty -> AddressBookEmptyScreen(
                onAddContactClick = addressBookListUM.onAddClick,
                onBackClick = router::pop,
                modifier = modifier.background(TangemTheme.colors3.bg.primary),
            )
            is AddressBookListUM.Content -> AddressBookListScreen(
                state = addressBookListUM,
                onBackClick = router::pop,
                modifier = modifier.background(TangemTheme.colors3.bg.primary),
            )
        }
        selector.child?.instance?.BottomSheet()
    }

    /**
     * @property mode               Default (management) or Selector (pick a contact for a network)
     * @property onContactClick     management mode — opens the contact editor (TODO [REDACTED_TASK_KEY])
     * @property onAddContactClick  opens the new-contact editor
     */
    data class Params(
        val mode: AddressBookRoute.ListMode,
        val onContactClick: (String) -> Unit,
        val onAddContactClick: () -> Unit,
    )
}