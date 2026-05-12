package com.tangem.domain.transaction

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.AmountType
import com.tangem.domain.models.currency.CryptoCurrency
import io.mockk.every
import io.mockk.mockk
import org.junit.Test
import java.math.BigDecimal

internal class CryptoCurrencyAmountExtTest {

    @Test
    fun `GIVEN currency is Coin WHEN toBlockchainAmount THEN returns Amount with AmountType_Coin`() {
        val currency = mockk<CryptoCurrency.Coin> {
            every { symbol } returns "BTC"
            every { decimals } returns 8
        }
        val value = BigDecimal("1.5")

        val result = currency.toBlockchainAmount(value)

        assertThat(result.currencySymbol).isEqualTo("BTC")
        assertThat(result.value).isEqualTo(value)
        assertThat(result.decimals).isEqualTo(8)
        assertThat(result.type).isEqualTo(AmountType.Coin)
    }

    @Test
    fun `GIVEN currency is Token WHEN toBlockchainAmount THEN returns Amount with AmountType_Token`() {
        val contractAddress = "0xdAC17F958D2ee523a2206206994597C13D831ec7"
        val currency = mockk<CryptoCurrency.Token> {
            every { symbol } returns "USDT"
            every { decimals } returns 6
            every { this@mockk.contractAddress } returns contractAddress
        }
        val value = BigDecimal("100")

        val result = currency.toBlockchainAmount(value)

        assertThat(result.currencySymbol).isEqualTo("USDT")
        assertThat(result.value).isEqualTo(value)
        assertThat(result.decimals).isEqualTo(6)
        val type = result.type as AmountType.Token
        assertThat(type.token.symbol).isEqualTo("USDT")
        assertThat(type.token.contractAddress).isEqualTo(contractAddress)
        assertThat(type.token.decimals).isEqualTo(6)
    }
}