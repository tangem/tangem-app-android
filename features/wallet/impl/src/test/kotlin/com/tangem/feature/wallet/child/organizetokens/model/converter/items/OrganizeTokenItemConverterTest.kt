package com.tangem.feature.wallet.child.organizetokens.model.converter.items

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.common.ui.components.currency.icon.converter.CryptoCurrencyToIconStateConverter
import com.tangem.common.ui.tokens.toTokenIconState
import com.tangem.core.ui.ds2.tokenrow.TangemTokenRow
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.domain.account.status.model.AccountCryptoCurrencyStatus
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.NetworkAddress
import com.tangem.test.mock.MockAccounts
import org.junit.jupiter.api.Test
import java.math.BigDecimal

internal class OrganizeTokenItemConverterTest {

    private val appCurrency = AppCurrency.Default
    private val coin = MockCryptoCurrencyFactory().createCoin(Blockchain.Ethereum)
    private val account = MockAccounts.createAccount(derivationIndex = 1, cryptoCurrencies = listOf(coin))

    private val converter = OrganizeTokenItemConverter(appCurrency = appCurrency)

    @Test
    fun `GIVEN loaded currency status WHEN convert THEN redesigned organize row state is built`() {
        // Arrange
        val status = CryptoCurrencyStatus(currency = coin, value = loaded(fiatAmount = BigDecimal.TEN))

        // Act
        val actual = converter.convert(AccountCryptoCurrencyStatus(account = account, status = status))

        // Assert
        val expected = TangemTokenRow.State.Organize(
            id = coin.id.value,
            icon = CryptoCurrencyToIconStateConverter().convert(status).toTokenIconState(),
            title = stringReference(coin.name),
            ticker = stringReference(coin.symbol),
            fiatBalance = stringReference(
                BigDecimal.TEN.format {
                    fiat(fiatCurrencyCode = appCurrency.code, fiatCurrencySymbol = appCurrency.symbol)
                },
            ),
        )
        assertThat(actual.tokenRowState).isEqualTo(expected)
        assertThat(actual.id).isEqualTo(coin.id.value)
    }

    private fun loaded(fiatAmount: BigDecimal) = CryptoCurrencyStatus.Loaded(
        amount = BigDecimal.ONE,
        fiatAmount = fiatAmount,
        fiatRate = fiatAmount,
        priceChange = BigDecimal.ZERO,
        yieldSupplyStatus = null,
        hasCurrentNetworkTransactions = false,
        pendingTransactions = emptySet(),
        networkAddress = NetworkAddress.Single(
            defaultAddress = NetworkAddress.Address(value = "addr", type = NetworkAddress.Address.Type.Primary),
        ),
        sources = CryptoCurrencyStatus.Sources(),
    )
}