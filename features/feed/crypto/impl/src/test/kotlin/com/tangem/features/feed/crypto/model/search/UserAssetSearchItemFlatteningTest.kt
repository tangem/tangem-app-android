package com.tangem.features.feed.crypto.model.search

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.account.AccountName
import com.tangem.domain.models.account.CryptoPortfolioIcon
import com.tangem.domain.models.account.DerivationIndex
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.portfolio.UserAssetEntry
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.search.model.UserAssetSearchItem
import org.junit.jupiter.api.Test

internal class UserAssetSearchItemFlatteningTest {

    private val currencyFactory = MockCryptoCurrencyFactory()
    private val eth = currencyFactory.createCoin(Blockchain.Ethereum)
    private val btc = currencyFactory.createCoin(Blockchain.Bitcoin)

    @Test
    fun `GIVEN single and grouped items WHEN flattened THEN every entry appears once in item order`() {
        // Arrange
        val single = entry(eth)
        val groupedFirst = entry(btc)
        val groupedSecond = entry(btc, walletId = SECOND_WALLET_ID)
        val items = listOf(
            UserAssetSearchItem.Single(entry = single),
            UserAssetSearchItem.Grouped(
                tokenName = btc.name,
                tokenSymbol = btc.symbol,
                tokenIconUrl = null,
                entries = listOf(groupedFirst, groupedSecond),
            ),
        )

        // Act
        val actual = items.toEntries()

        // Assert
        assertThat(actual).containsExactly(single, groupedFirst, groupedSecond).inOrder()
    }

    @Test
    fun `GIVEN no items WHEN flattened THEN the result is empty`() {
        // Act
        val actual = emptyList<UserAssetSearchItem>().toEntries()

        // Assert
        assertThat(actual).isEmpty()
    }

    private fun entry(currency: CryptoCurrency, walletId: UserWalletId = FIRST_WALLET_ID) = UserAssetEntry(
        userWalletId = walletId,
        userWalletName = "Wallet",
        accountId = AccountId.forCryptoPortfolio(walletId, DerivationIndex(value = 0).getOrNull()!!),
        accountName = AccountName("Main").getOrNull()!!,
        accountIcon = CryptoPortfolioIcon.ofDefaultCustomAccount(),
        currencyStatus = CryptoCurrencyStatus(currency = currency, value = CryptoCurrencyStatus.Loading),
    )

    private companion object {
        val FIRST_WALLET_ID = UserWalletId("011")
        val SECOND_WALLET_ID = UserWalletId("022")
    }
}