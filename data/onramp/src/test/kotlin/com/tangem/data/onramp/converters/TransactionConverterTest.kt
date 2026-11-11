package com.tangem.data.onramp.converters

import com.google.common.truth.Truth.assertThat
import com.tangem.data.onramp.models.OnrampTransactionDTO
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.onramp.model.OnrampCurrency
import com.tangem.domain.onramp.model.OnrampStatus
import com.tangem.domain.onramp.model.cache.OnrampTransaction
import com.tangem.grow.datasource.onramp.models.response.Status
import org.junit.jupiter.api.Test
import java.math.BigDecimal

internal class TransactionConverterTest {

    private val converter = TransactionConverter()

    @Test
    fun `GIVEN dto WHEN convert THEN redirect url is omitted from cached model`() {
        // Act
        val actual = converter.convert(createDto())

        // Assert
        assertThat(actual).isEqualTo(createTransaction(redirectUrl = ""))
    }

    @Test
    fun `GIVEN transaction WHEN convertBack THEN status and currency are mapped`() {
        // Act
        val actual = converter.convertBack(createTransaction(redirectUrl = "https://widget"))

        // Assert
        assertThat(actual).isEqualTo(createDto())
    }

    private fun createDto() = OnrampTransactionDTO(
        txId = "tx-1",
        userWalletId = UserWalletId("0011"),
        fromAmount = BigDecimal("12.34"),
        fromCurrency = com.tangem.grow.datasource.onramp.models.response.model.OnrampCurrencyDTO(
            name = "US Dollar", code = "USD", image = null, precision = 2, unit = "USD",
        ),
        toAmount = BigDecimal("0.001"),
        toCurrencyId = "ethereum_native",
        status = Status.Finished,
        externalTxUrl = "https://provider/tx-1",
        externalTxId = "external-1",
        timestamp = 123L,
        providerName = "Provider",
        providerImageUrl = "https://provider/logo.png",
        providerType = "ONRAMP",
        paymentMethod = "Card",
        residency = "United States",
    )

    private fun createTransaction(redirectUrl: String) = OnrampTransaction(
        txId = "tx-1",
        userWalletId = UserWalletId("0011"),
        fromAmount = BigDecimal("12.34"),
        fromCurrency = OnrampCurrency("US Dollar", "USD", null, 2, "USD"),
        toAmount = BigDecimal("0.001"),
        toCurrencyId = "ethereum_native",
        status = OnrampStatus.Status.Finished,
        externalTxUrl = "https://provider/tx-1",
        externalTxId = "external-1",
        timestamp = 123L,
        providerName = "Provider",
        providerImageUrl = "https://provider/logo.png",
        providerType = "ONRAMP",
        redirectUrl = redirectUrl,
        paymentMethod = "Card",
        residency = "United States",
    )
}