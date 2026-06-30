package com.tangem.features.addressbook.common

import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.addressbook.model.ContactId
import com.tangem.features.addressbook.AddressSelectorComponent
import com.tangem.features.addressbook.addaddress.DefaultAddAddressComponent
import com.tangem.features.addressbook.editcontact.DefaultEditContactComponent
import com.tangem.features.addressbook.editcontact.ui.state.ValidatedAddress
import com.tangem.features.addressbook.list.DefaultAddressBookListComponent
import com.tangem.features.addressbook.route.AddressBookRoute
import com.tangem.features.addressbook.selectnetworks.DefaultSelectNetworksComponent
import com.tangem.features.commonfeatures.api.portfolioselector.PortfolioSelectorComponent
import kotlinx.collections.immutable.persistentListOf
import javax.inject.Inject

/**
 * Builds the child screens of the address book feature for a given [AddressBookRoute], wiring their callbacks to the
 * container's [AddressBookClickIntents]. Mirrors the `FeedEntryChildFactory` pattern used by the feed feature.
 */
internal class AddressBookChildFactory @Inject constructor(
    private val addressSelectorFactory: AddressSelectorComponent.Factory,
    private val portfolioSelectorComponentFactory: PortfolioSelectorComponent.Factory,
) {

    fun createChild(
        route: AddressBookRoute,
        context: AppComponentContext,
        clickIntents: AddressBookClickIntents,
    ): ComposableContentComponent = when (route) {
        is AddressBookRoute.List -> DefaultAddressBookListComponent(
            appComponentContext = context,
            params = DefaultAddressBookListComponent.Params(
                mode = route.mode,
                onContactClick = { clickIntents.onContactClick(ContactId(it)) },
                onAddContactClick = clickIntents::onAddContactClick,
            ),
            addressSelectorFactory = addressSelectorFactory,
        )
        is AddressBookRoute.EditContact -> DefaultEditContactComponent(
            appComponentContext = context,
            params = DefaultEditContactComponent.Params(
                contactId = route.contactId?.let(::ContactId),
                predefinedAddress = buildPredefinedAddress(route),
                onBackClick = clickIntents::onEditContactBack,
                onAddAddressClick = clickIntents::onAddAddressClick,
            ),
            portfolioSelectorComponentFactory = portfolioSelectorComponentFactory,
        )
        AddressBookRoute.AddAddress -> DefaultAddAddressComponent(
            appComponentContext = context,
            params = DefaultAddAddressComponent.Params(
                onBackClick = clickIntents::onAddAddressBack,
                onSelectNetworksClick = clickIntents::onSelectNetworksClick,
                onConfirm = clickIntents::onAddressConfirmed,
            ),
        )
        is AddressBookRoute.SelectNetworks -> DefaultSelectNetworksComponent(
            appComponentContext = context,
            params = DefaultSelectNetworksComponent.Params(
                address = route.address,
                selectedNetworkIds = route.selectedNetworkIds,
                onBackClick = clickIntents::onSelectNetworksBack,
                onDone = clickIntents::onNetworksSelected,
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