package com.tangem.features.send.feeselector.component.token.model

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.core.ui.ds2.tokenrow.TangemTokenRow
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.NetworkAddress
import com.tangem.utils.Provider
import org.junit.jupiter.api.Test
import java.math.BigDecimal

internal class FeeTokenForListConverterTest {

    private val coin = MockCryptoCurrencyFactory().createCoin(Blockchain.Solana)

    @Test
    fun `GIVEN redesigned rows enabled AND selectable token WHEN convert THEN clickable DS3 row`() {
        // Arrange
        var clicked = false
        val converter = createConverter(canPayFee = true, onTokenClick = { clicked = true }, isRedesigned = true)

        // Act
        val actual = converter.convert(loadedStatus(amount = BigDecimal.ONE))

        // Assert
        val row = actual.tokenRow as TangemTokenRow.State.Content
        assertThat(row.id).isEqualTo(coin.id.value)
        row.onClick?.invoke()
        assertThat(clicked).isTrue()
    }

    @Test
    fun `GIVEN redesigned rows enabled AND not enough for fee WHEN convert THEN DS3 row is not clickable`() {
        // Arrange
        val converter = createConverter(canPayFee = false, isRedesigned = true)

        // Act
        val actual = converter.convert(loadedStatus(amount = BigDecimal.ONE))

        // Assert
        assertThat(actual.isAvailable).isFalse()
        assertThat((actual.tokenRow as TangemTokenRow.State.Content).onClick).isNull()
    }

    @Test
    fun `GIVEN redesigned rows enabled AND sponsored fee WHEN convert THEN DS3 row is keyed and titled as sponsored`() {
        // Arrange
        val converter = createConverter(canPayFee = true, isSponsoredFee = true, isRedesigned = true)

        // Act
        val actual = converter.convert(loadedStatus(amount = BigDecimal.ZERO))

        // Assert
        val row = actual.tokenRow as TangemTokenRow.State.Content
        assertThat(row.id).isEqualTo("${coin.id.value}_sponsored")
        assertThat(row.title).isEqualTo(stringReference("SmartGas ${coin.symbol}"))
        assertThat(row.id).isEqualTo(actual.state.id)
    }

    @Test
    fun `GIVEN redesigned rows disabled WHEN convert THEN no DS3 row`() {
        // Arrange
        val converter = createConverter(canPayFee = true, isRedesigned = false)

        // Act
        val actual = converter.convert(loadedStatus(amount = BigDecimal.ONE))

        // Assert
        assertThat(actual.tokenRow).isNull()
    }

    private fun createConverter(
        canPayFee: Boolean,
        isRedesigned: Boolean,
        isSponsoredFee: Boolean = false,
        onTokenClick: () -> Unit = {},
    ) = FeeTokenForListConverter(
        appCurrencyProvider = Provider { AppCurrency.Default },
        canPayFee = canPayFee,
        onTokenClick = onTokenClick,
        isSponsoredFee = isSponsoredFee,
        isRedesignedTokenRowsEnabled = isRedesigned,
    )

    private fun loadedStatus(amount: BigDecimal) = CryptoCurrencyStatus(
        currency = coin,
        value = CryptoCurrencyStatus.Loaded(
            amount = amount,
            fiatAmount = amount,
            fiatRate = BigDecimal.ONE,
            priceChange = BigDecimal.ZERO,
            yieldSupplyStatus = null,
            hasCurrentNetworkTransactions = false,
            pendingTransactions = emptySet(),
            networkAddress = NetworkAddress.Single(
                defaultAddress = NetworkAddress.Address(value = "addr", type = NetworkAddress.Address.Type.Primary),
            ),
            sources = CryptoCurrencyStatus.Sources(),
        ),
    )
}