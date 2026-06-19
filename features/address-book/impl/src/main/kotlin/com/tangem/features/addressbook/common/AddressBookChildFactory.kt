package com.tangem.features.addressbook.common

import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.addressbook.model.ContactId
import com.tangem.features.addressbook.addaddress.DefaultAddAddressComponent
import com.tangem.features.addressbook.editcontact.DefaultEditContactComponent
import com.tangem.features.addressbook.editcontact.ui.state.ValidatedAddress
import com.tangem.features.addressbook.list.DefaultAddressBookListComponent
import com.tangem.features.addressbook.route.AddressBookRoute
import kotlinx.collections.immutable.persistentListOf
import javax.inject.Inject

/**
 * Builds the child screens of the address book feature for a given [AddressBookRoute], wiring their callbacks to the
 * container's [AddressBookClickIntents]. Mirrors the `FeedEntryChildFactory` pattern used by the feed feature.
 */
internal class AddressBookChildFactory @Inject constructor() {

    fun createChild(
        route: AddressBookRoute,
        context: AppComponentContext,
        clickIntents: AddressBookClickIntents,
    ): ComposableContentComponent = when (route) {
        AddressBookRoute.List -> DefaultAddressBookListComponent(
            appComponentContext = context,
            params = DefaultAddressBookListComponent.Params(
                onContactClick = { clickIntents.onContactClick(ContactId(it)) },
                onAddContactClick = clickIntents::onAddContactClick,
            ),
        )
        is AddressBookRoute.EditContact -> DefaultEditContactComponent(
            appComponentContext = context,
            params = DefaultEditContactComponent.Params(
                contactId = route.contactId?.let(::ContactId),
                predefinedAddress = buildPredefinedAddress(route),
                onBackClick = clickIntents::onEditContactBack,
                onAddAddressClick = clickIntents::onAddAddressClick,
            ),
        )
        AddressBookRoute.AddAddress -> DefaultAddAddressComponent(
            appComponentContext = context,
            params = DefaultAddAddressComponent.Params(
                onBackClick = clickIntents::onAddAddressBack,
                onConfirm = clickIntents::onAddressConfirmed,
            ),
        )
    }

    /** Builds the address attached up-front in WithContactCreation mode, when both the address and network are known. */
    private fun buildPredefinedAddress(route: AddressBookRoute.EditContact): ValidatedAddress? {
        val address = route.predefinedAddress ?: return null
        val networkId = route.predefinedNetworkId ?: return null
        return ValidatedAddress(address = address, networkIds = persistentListOf(networkId))
    }
}