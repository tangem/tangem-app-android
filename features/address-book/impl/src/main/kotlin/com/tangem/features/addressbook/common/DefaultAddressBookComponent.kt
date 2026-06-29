package com.tangem.features.addressbook.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.slide
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.pushNew
import com.tangem.common.routing.entity.AddressBookOpenMode
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.childByContext
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.addressbook.model.ContactId
import com.tangem.features.addressbook.AddressBookComponent
import com.tangem.features.addressbook.editcontact.ui.state.ValidatedAddress
import com.tangem.features.addressbook.route.AddressBookRoute
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultAddressBookComponent @AssistedInject constructor(
    @Assisted context: AppComponentContext,
    @Assisted private val params: AddressBookComponent.Params,
    private val childFactory: AddressBookChildFactory,
    private val resultHolder: AddressBookResultHolder,
    private val selectNetworksResultHolder: SelectNetworksResultHolder,
) : AddressBookComponent, AppComponentContext by context {

    private val navigation = StackNavigation<AddressBookRoute>()

    init {
        // Drop any results left over from a previous session before the (possibly preloaded) stack starts collecting.
        resultHolder.clear()
        selectNetworksResultHolder.clear()
    }

    private val clickIntents = object : AddressBookClickIntents {

        override fun onContactClick(contactId: ContactId) {
            navigation.pushNew(AddressBookRoute.EditContact(contactId = contactId.value))
        }

        override fun onAddContactClick() {
            navigation.pushNew(AddressBookRoute.EditContact())
        }

        override fun onEditContactBack() {
            navigation.pop()
        }

        override fun onAddAddressClick() {
            navigation.pushNew(AddressBookRoute.AddAddress)
        }

        override fun onAddAddressBack() {
            navigation.pop()
        }

        override fun onAddressConfirmed(address: ValidatedAddress) {
            resultHolder.setConfirmedAddress(address)
            navigation.pop()
        }

        override fun onSelectNetworksClick(address: String, selectedNetworkIds: List<String>) {
            navigation.pushNew(
                AddressBookRoute.SelectNetworks(address = address, selectedNetworkIds = selectedNetworkIds),
            )
        }

        override fun onSelectNetworksBack() {
            navigation.pop()
        }

        override fun onNetworksSelected(selectedNetworkIds: Set<String>) {
            selectNetworksResultHolder.setSelectedNetworkIds(selectedNetworkIds)
            navigation.pop()
        }
    }

    private val contentStack = childStack(
        key = "address_book_stack",
        source = navigation,
        serializer = AddressBookRoute.serializer(),
        initialStack = ::initialStack,
        handleBackButton = false,
        childFactory = ::screenChild,
    )

    @Composable
    override fun Content(modifier: Modifier) {
        val childStack by contentStack.subscribeAsState()
        Children(
            modifier = modifier,
            stack = childStack,
            animation = stackAnimation(slide()),
        ) { child ->
            child.instance.Content(Modifier)
        }
    }

    private fun screenChild(config: AddressBookRoute, componentContext: ComponentContext): ComposableContentComponent {
        return childFactory.createChild(
            route = config,
            context = childByContext(componentContext),
            clickIntents = clickIntents,
        )
    }

    private fun initialStack(): List<AddressBookRoute> = when (val mode = params.addressBookOpenMode) {
        AddressBookOpenMode.Default -> listOf(AddressBookRoute.List())
        is AddressBookOpenMode.ContactSelection -> listOf(
            AddressBookRoute.List(mode = AddressBookRoute.ListMode.Selector(networkId = mode.networkId)),
        )
        is AddressBookOpenMode.WithContactCreation -> listOf(
            AddressBookRoute.List(),
            // Address + network are already known, so open the new contact with that address attached — no AddAddress.
            AddressBookRoute.EditContact(
                predefinedAddress = mode.address,
                predefinedNetworkId = mode.networkId,
            ),
        )
    }

    @AssistedFactory
    interface Factory : AddressBookComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: AddressBookComponent.Params,
        ): DefaultAddressBookComponent
    }
}