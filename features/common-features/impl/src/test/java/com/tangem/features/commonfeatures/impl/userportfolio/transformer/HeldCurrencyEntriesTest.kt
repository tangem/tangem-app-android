package com.tangem.features.commonfeatures.impl.userportfolio.transformer

import com.google.common.truth.Truth.assertThat
import com.tangem.common.test.domain.wallet.MockUserWalletFactory
import com.tangem.common.ui.markets.tokenselector.TokenSelectorEntry
import com.tangem.domain.core.utils.lceError
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.TokensSortType
import com.tangem.domain.models.TotalFiatBalance
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.NetworkAddress
import com.tangem.domain.models.tokenlist.TokenList
import com.tangem.features.commonfeatures.api.addtoportfolio.AvailableToAddData
import com.tangem.features.commonfeatures.api.addtoportfolio.AvailableToAddWallet
import com.tangem.test.core.ProvideTestModels
import com.tangem.test.mock.MockAccounts
import java.math.BigDecimal
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class HeldCurrencyEntriesTest {

    private val userWallet = MockUserWalletFactory.create()
    private val onPolygon = token(networkId = POLYGON)
    private val onEthereum = token(networkId = ETHEREUM)
    private val otherToken = MockAccounts.createPaymentAccountToken(rawId = "tether", networkId = POLYGON)

    private val account = AccountStatus.Personal(
        account = MockAccounts.createAccount(
            derivationIndex = 0,
            cryptoCurrencies = listOf(onPolygon, onEthereum, otherToken),
        ),
        tokenList = TokenList.Ungrouped(
            currencies = listOf(onPolygon, onEthereum, otherToken).map(::currencyStatus),
            totalFiatBalance = TotalFiatBalance.Loaded(BigDecimal.ONE, StatusSource.ACTUAL),
            sortedBy = TokensSortType.NONE,
        ),
        priceChangeLce = Unit.lceError(),
    )

    private val data = AvailableToAddData(
        availableToAddWallets = mapOf(
            userWallet.walletId to AvailableToAddWallet(
                userWallet = userWallet,
                accounts = listOf(account),
                availableNetworks = emptySet(),
                availableToAddAccounts = emptyMap(),
            ),
        ),
    )

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class NetworkScope {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN the token held on two networks WHEN heldCurrencyEntries THEN only the scoped holdings are returned`(
            model: HeldModel,
        ) {
            // Act
            val actual = data.heldCurrencyEntries(
                rawCurrencyId = RAW_ID,
                networkIds = model.networkIds,
            )

            // Assert
            assertThat(actual).containsExactlyElementsIn(model.expected.map { entry(account, it) }).inOrder()
        }

        private fun provideTestModels() = listOf(
            HeldModel(networkIds = null, expected = listOf(onPolygon, onEthereum)),
            HeldModel(networkIds = setOf(ETHEREUM), expected = listOf(onEthereum)),
            HeldModel(networkIds = setOf(POLYGON, ETHEREUM), expected = listOf(onPolygon, onEthereum)),
            HeldModel(networkIds = setOf("tron"), expected = emptyList()),
            HeldModel(networkIds = emptySet(), expected = emptyList()),
        )
    }

    data class HeldModel(val networkIds: Set<String>?, val expected: List<CryptoCurrency.Token>)

    private fun entry(account: AccountStatus.CryptoPortfolio, token: CryptoCurrency.Token) = TokenSelectorEntry(
        wallet = userWallet,
        account = account,
        currencyStatus = currencyStatus(token),
    )

    private fun token(networkId: String) = MockAccounts.createPaymentAccountToken(
        rawId = RAW_ID.value,
        networkId = networkId,
    )

    private fun currencyStatus(token: CryptoCurrency.Token) = CryptoCurrencyStatus(
        currency = token,
        value = CryptoCurrencyStatus.NoQuote(
            amount = BigDecimal.ONE,
            networkAddress = NetworkAddress.Single(
                defaultAddress = NetworkAddress.Address(type = NetworkAddress.Address.Type.Primary, value = ADDRESS),
            ),
            yieldSupplyStatus = null,
            hasCurrentNetworkTransactions = false,
            pendingTransactions = emptySet(),
            sources = CryptoCurrencyStatus.Sources(),
        ),
    )

    private companion object {
        val RAW_ID = CryptoCurrency.RawID("usd-coin")
        const val POLYGON = "polygon"
        const val ETHEREUM = "ethereum"
        const val ADDRESS = "0x0000000000000000000000000000000000000001"
    }
}