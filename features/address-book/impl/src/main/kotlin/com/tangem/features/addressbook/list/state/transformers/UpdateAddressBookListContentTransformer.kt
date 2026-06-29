package com.tangem.features.addressbook.list.state.transformers

import com.tangem.core.ui.R
import com.tangem.core.ui.ds2.search.TangemSearch
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.addressbook.model.VerifiedContact
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.addressbook.MatchedContact
import com.tangem.features.addressbook.common.ContactMatcher
import com.tangem.features.addressbook.list.state.transformers.converter.DefaultContactConverter
import com.tangem.features.addressbook.list.state.transformers.converter.SelectorContactConverter
import com.tangem.features.addressbook.list.ui.state.AddressBookChipUM
import com.tangem.features.addressbook.list.ui.state.AddressBookListUM
import com.tangem.features.addressbook.list.ui.state.ContactUM
import com.tangem.features.addressbook.list.ui.state.ContentMode
import com.tangem.features.addressbook.route.AddressBookRoute
import com.tangem.utils.transformer.Transformer
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

@Suppress("LongParameterList")
internal class UpdateAddressBookListContentTransformer(
    wallets: Map<UserWalletId, UserWallet>,
    private val allContacts: List<VerifiedContact>,
    private val matchedContacts: List<VerifiedContact>,
    private val mode: AddressBookRoute.ListMode,
    private val selectedWalletId: String?,
    private val query: String,
    private val onContactClick: (String) -> Unit,
    private val onPickContact: (MatchedContact) -> Unit,
    private val onQueryChange: (String) -> Unit,
    private val onActiveChange: (Boolean) -> Unit,
    private val onClearQuery: () -> Unit,
    private val onChipSelected: (String?) -> Unit,
    private val onAddContactClick: () -> Unit,
) : Transformer<AddressBookListUM> {

    private val orderedWalletIds: List<String> = wallets.values.map { it.walletId.stringValue }
    private val walletNamesById: Map<String, String> = wallets.values.associate { it.walletId.stringValue to it.name }

    override fun transform(prevState: AddressBookListUM): AddressBookListUM {
        val matchedItems = matchedItems()

        if (matchedItems.isEmpty() && query.isBlank()) {
            return AddressBookListUM.Empty(onAddClick = onAddContactClick)
        }

        val matchingWalletIds = matchedItems.map { it.walletId }.distinct()
        val effectiveSelected = selectedWalletId.takeIf { it in matchingWalletIds }
        val areChipsVisible = totalWalletIds().size >= 2 && matchedItems.isNotEmpty()

        // On the "All" chip of a multi-wallet book each contact shows which wallet it belongs to.
        val shouldShowWalletName = areChipsVisible && effectiveSelected == null

        val displayContacts = matchedItems
            .filter { effectiveSelected == null || it.walletId == effectiveSelected }
            .map { if (shouldShowWalletName) it.copy(walletName = walletNamesById[it.walletId]) else it }
            .toImmutableList()

        return AddressBookListUM.Content(
            searchBar = (prevState as? AddressBookListUM.Content)?.searchBar ?: buildSearchBar(),
            chips = if (areChipsVisible) buildChips(matchingWalletIds, effectiveSelected) else persistentListOf(),
            contacts = displayContacts,
            isNothingFound = matchedItems.isEmpty(),
            contentMode = contentMode(),
        )
    }

    private fun matchedItems(): List<ContactUM> = when (val mode = mode) {
        AddressBookRoute.ListMode.Default ->
            DefaultContactConverter(onContactClick).convertList(matchedContacts)
        is AddressBookRoute.ListMode.Selector ->
            SelectorContactConverter(onPickContact)
                .convertList(ContactMatcher.match(matchedContacts.map { it.contact }, mode.networkId))
    }

    /** Wallets that own at least one contact (respecting the network filter in selector mode) — drives chip visibility. */
    private fun totalWalletIds(): Set<String> = when (val mode = mode) {
        AddressBookRoute.ListMode.Default -> allContacts.mapTo(mutableSetOf()) { it.contact.walletId.stringValue }
        is AddressBookRoute.ListMode.Selector ->
            ContactMatcher.match(allContacts.map { it.contact }, mode.networkId).mapTo(mutableSetOf()) { it.walletId }
    }

    private fun contentMode(): ContentMode = when (mode) {
        AddressBookRoute.ListMode.Default -> ContentMode.Default(onAddClick = onAddContactClick)
        is AddressBookRoute.ListMode.Selector -> ContentMode.Select
    }

    private fun buildSearchBar(): TangemSearch.State = TangemSearch.State(
        placeholderText = resourceReference(R.string.common_search),
        query = query,
        onQueryChange = onQueryChange,
        isActive = false,
        onActiveChange = onActiveChange,
        onClearClick = onClearQuery,
        onCloseClick = { onActiveChange(false) },
    )

    private fun buildChips(matchingWalletIds: List<String>, effectiveSelected: String?) = buildList {
        add(
            AddressBookChipUM(
                id = ALL_CHIP_ID,
                text = resourceReference(R.string.common_all),
                isSelected = effectiveSelected == null,
                onClick = { onChipSelected(null) },
            ),
        )
        orderedWalletIds
            .filter { it in matchingWalletIds }
            .forEach { walletId ->
                add(
                    AddressBookChipUM(
                        id = walletId,
                        text = stringReference(walletNamesById[walletId] ?: walletId),
                        isSelected = walletId == effectiveSelected,
                        onClick = { onChipSelected(walletId) },
                        iconRes = R.drawable.ic_key_card_20,
                    ),
                )
            }
    }.toImmutableList()

    private companion object {
        const val ALL_CHIP_ID = "all"
    }
}