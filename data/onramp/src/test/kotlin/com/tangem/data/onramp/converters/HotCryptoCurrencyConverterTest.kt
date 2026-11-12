package com.tangem.data.onramp.converters

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchainsdk.utils.ExcludedBlockchains
import com.tangem.datasource.api.tangemTech.models.HotCryptoResponse
import com.tangem.domain.models.wallet.UserWallet
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.NullAndEmptySource
import org.junit.jupiter.params.provider.ValueSource
import java.math.BigDecimal

internal class HotCryptoCurrencyConverterTest {

    private val converter = HotCryptoCurrencyConverter(mockk<UserWallet.Hot>(), "https://images/", ExcludedBlockchains())

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = ["unknown-network"])
    fun `GIVEN missing or unsupported network WHEN convert THEN token is discarded`(networkId: String?) {
        assertThat(converter.convert(createToken().copy(networkId = networkId))).isNull()
    }

    @Test
    fun `GIVEN missing id WHEN convert THEN token is discarded`() {
        assertThat(converter.convert(createToken().copy(id = null))).isNull()
    }

    @Test
    fun `GIVEN Ethereum quote WHEN convert THEN image and percentage use domain format`() {
        // Act
        val actual = requireNotNull(converter.convert(createToken()))
        // Assert
        assertThat(actual.cryptoCurrency.iconUrl).isEqualTo("https://images/large/ethereum.png")
        assertThat(actual.quoteStatus.value).isEqualTo(
            com.tangem.domain.models.quote.QuoteStatus.Data(
                source = com.tangem.domain.models.StatusSource.ACTUAL,
                fiatRate = BigDecimal("2000"),
                fiatRateUSD = BigDecimal.ZERO,
                priceChange = BigDecimal("0.125"),
            ),
        )
    }

    private fun createToken() = HotCryptoResponse.Token(
        id = "ethereum", name = "Ethereum", symbol = "ETH", networkId = "ethereum",
        currentPrice = BigDecimal("2000"), priceChangePercentage = BigDecimal("12.5"),
    )
}