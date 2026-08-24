package com.tangem.common.ui.markets.tokenselector

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.common.ui.account.toUM
import com.tangem.core.ui.ds.image.DeviceIconUM
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.account.AccountName
import com.tangem.domain.models.account.CryptoPortfolioIcon
import com.tangem.domain.models.account.DerivationIndex
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.portfolio.UserAssetEntry
import com.tangem.domain.models.wallet.UserWalletId
import org.junit.jupiter.api.Test

internal class UserAssetEntrySectionsConverterTest {

    private val currencyFactory = MockCryptoCurrencyFactory()
    private val eth = currencyFactory.createCoin(Blockchain.Ethereum)
    private val btc = currencyFactory.createCoin(Blockchain.Bitcoin)

    @Test
    fun `GIVEN one wallet and one account WHEN convert THEN one group with an account header and no wallet header`() {
        // Arrange
        val entries = listOf(entry(currency = eth), entry(currency = btc))

        // Act
        val sections = converter().convert(entries)

        // Assert
        val group = sections.single() as TokenSelectorSectionUM.TokenGroup
        assertThat(group.accountHeader?.accountName).isEqualTo(AccountName("Main").getOrNull()!!.toUM().value)
        assertThat(group.items).hasSize(2)
    }

    @Test
    fun `GIVEN two wallets WHEN convert THEN a wallet header precedes each wallet group`() {
        // Arrange
        val entries = listOf(
            entry(walletId = FIRST_WALLET_ID, walletName = "First", currency = eth),
            entry(walletId = SECOND_WALLET_ID, walletName = "Second", currency = btc),
        )

        // Act
        val sections = converter().convert(entries)

        // Assert
        assertThat(sections.filterIsInstance<TokenSelectorSectionUM.WalletHeader>().map { it.walletName })
            .containsExactly("First", "Second")
            .inOrder()
        assertThat(sections.filterIsInstance<TokenSelectorSectionUM.TokenGroup>()).hasSize(2)
    }

    @Test
    fun `GIVEN one wallet with two accounts WHEN convert THEN each group carries an account header`() {
        // Arrange
        val entries = listOf(
            entry(accountIndex = 0, accountName = "Main", currency = eth),
            entry(accountIndex = 1, accountName = "Second", currency = btc),
        )

        // Act
        val sections = converter().convert(entries)

        // Assert
        val groups = sections.filterIsInstance<TokenSelectorSectionUM.TokenGroup>()
        assertThat(groups).hasSize(2)
        assertThat(groups.all { it.accountHeader != null }).isTrue()
        assertThat(sections.filterIsInstance<TokenSelectorSectionUM.WalletHeader>()).isEmpty()
    }

    @Test
    fun `GIVEN a wallet missing from walletIcons WHEN convert THEN its header falls back to a stub icon`() {
        // Arrange
        val entries = listOf(
            entry(walletId = FIRST_WALLET_ID, currency = eth),
            entry(walletId = SECOND_WALLET_ID, currency = btc),
        )

        // Act
        val sections = converter(walletIcons = emptyMap()).convert(entries)

        // Assert
        assertThat(sections.filterIsInstance<TokenSelectorSectionUM.WalletHeader>().map { it.deviceIcon })
            .containsExactly(DeviceIconUM.Stub(cardsCount = 1), DeviceIconUM.Stub(cardsCount = 1))
    }

    @Test
    fun `GIVEN an entry WHEN convert THEN its row id joins wallet account and currency`() {
        // Arrange
        val entry = entry(walletId = FIRST_WALLET_ID, currency = eth)

        // Act
        val row = (converter().convert(listOf(entry)).single() as TokenSelectorSectionUM.TokenGroup).items.single()

        // Assert
        assertThat(row.id).isEqualTo(
            "${entry.userWalletId.stringValue}_${entry.accountId.value}_${eth.id.value}",
        )
    }

    @Test
    fun `GIVEN a clicked row WHEN onClick invoked THEN its entry is reported back`() {
        // Arrange
        var clicked: UserAssetEntry? = null
        val entry = entry(currency = eth)

        // Act
        val sections = converter(onEntryClick = { clicked = it }).convert(listOf(entry))
        (sections.single() as TokenSelectorSectionUM.TokenGroup).items.single().onClick()

        // Assert
        assertThat(clicked).isEqualTo(entry)
    }

    @Test
    fun `GIVEN no entries WHEN convert THEN sections are empty`() {
        // Act
        val sections = converter().convert(emptyList())

        // Assert
        assertThat(sections).isEmpty()
    }

    private fun converter(
        walletIcons: Map<UserWalletId, DeviceIconUM> = mapOf(
            FIRST_WALLET_ID to DeviceIconUM.Stub(cardsCount = 2),
            SECOND_WALLET_ID to DeviceIconUM.Stub(cardsCount = 3),
        ),
        onEntryClick: (UserAssetEntry) -> Unit = {},
    ) = UserAssetEntrySectionsConverter(
        appCurrency = AppCurrency.Default,
        isBalanceHidden = false,
        walletIcons = walletIcons,
        onEntryClick = onEntryClick,
    )

    private fun entry(
        currency: CryptoCurrency,
        walletId: UserWalletId = FIRST_WALLET_ID,
        walletName: String = "Wallet",
        accountIndex: Int = 0,
        accountName: String = "Main",
    ) = UserAssetEntry(
        userWalletId = walletId,
        userWalletName = walletName,
        accountId = AccountId.forCryptoPortfolio(walletId, DerivationIndex(value = accountIndex).getOrNull()!!),
        accountName = AccountName(accountName).getOrNull()!!,
        accountIcon = CryptoPortfolioIcon.ofDefaultCustomAccount(),
        currencyStatus = CryptoCurrencyStatus(currency = currency, value = CryptoCurrencyStatus.Loading),
    )

    private companion object {
        val FIRST_WALLET_ID = UserWalletId("011")
        val SECOND_WALLET_ID = UserWalletId("022")
    }
}