package com.tangem.features.addressbook.list.ui.preview

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.tangem.common.ui.account.AccountIconUM
import com.tangem.core.ui.R
import com.tangem.core.ui.ds2.search.TangemSearch
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.models.account.CryptoPortfolioIcon
import com.tangem.features.addressbook.list.ui.state.AddressBookChipUM
import com.tangem.features.addressbook.list.ui.state.AddressBookListUM
import com.tangem.features.addressbook.list.ui.state.ContactUM
import com.tangem.features.addressbook.list.ui.state.ContentMode
import kotlinx.collections.immutable.persistentListOf

internal data class AddressBookListPreviewScenario(
    val title: String,
    val state: AddressBookListUM.Content,
)

internal object AddressBookListPreviewFixtures {

    private val scenarioDefaultWithContacts = AddressBookListPreviewScenario(
        title = "Default – chips and contacts",
        state = AddressBookListUM.Content(
            searchBar = searchBar(query = ""),
            chips = persistentListOf(
                AddressBookChipUM(
                    id = "all",
                    text = resourceReference(R.string.common_all),
                    isSelected = true,
                    onClick = {},
                ),
                AddressBookChipUM(
                    id = "00",
                    text = stringReference("Wallet 1"),
                    isSelected = false,
                    onClick = {},
                    iconRes = R.drawable.ic_key_card_20,
                ),
                AddressBookChipUM(
                    id = "01",
                    text = stringReference("Wallet 2"),
                    isSelected = false,
                    onClick = {},
                    iconRes = R.drawable.ic_key_card_20,
                ),
            ),
            contacts = persistentListOf(
                contact(
                    walletId = "00",
                    name = "Binance",
                    color = CryptoPortfolioIcon.Color.Azure,
                    count = 1,
                ),
                contact(
                    walletId = "01",
                    name = "Alice",
                    color = CryptoPortfolioIcon.Color.UFOGreen,
                    count = 3,
                ),
            ),
            isNothingFound = false,
            contentMode = ContentMode.Default(onAddClick = {}),
        ),
    )

    val scenarioSelectNothingFound = AddressBookListPreviewScenario(
        title = "Select – nothing found",
        state = AddressBookListUM.Content(
            searchBar = searchBar(query = "Antonio"),
            chips = persistentListOf(),
            contacts = persistentListOf(),
            isNothingFound = true,
            contentMode = ContentMode.Select,
        ),
    )

    fun allScenarios(): List<AddressBookListPreviewScenario> = listOf(
        scenarioDefaultWithContacts,
        scenarioSelectNothingFound,
    )

    private fun searchBar(query: String) = TangemSearch.State(
        placeholderText = resourceReference(R.string.common_search),
        query = query,
        onQueryChange = {},
        isActive = false,
        onActiveChange = {},
    )

    private fun contact(walletId: String, name: String, color: CryptoPortfolioIcon.Color, count: Int) = ContactUM(
        id = name + walletId,
        walletId = walletId,
        name = name,
        icon = AccountIconUM.CryptoPortfolio(value = CryptoPortfolioIcon.Icon.Letter, color = color),
        networkAddressCount = count,
        onClick = {},
    )
}

/** All [AddressBookListPreviewScenario] values for the Preview Parameter dropdown in Android Studio. */
internal class AddressBookListPreviewParameterProvider : PreviewParameterProvider<AddressBookListPreviewScenario> {
    override val values: Sequence<AddressBookListPreviewScenario>
        get() = AddressBookListPreviewFixtures.allScenarios().asSequence()
}