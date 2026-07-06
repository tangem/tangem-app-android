package com.tangem.features.foryou.impl.model.converter

import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.ds.row.token.TangemTokenRowUM
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.Network
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.math.BigDecimal

internal class ForYouTokenRowConverterTest {

    private val appCurrency: AppCurrency = AppCurrency.Default

    @Nested
    inner class ConvertNetworkGroup {

        @Test
        fun `GIVEN all statuses Loading WHEN convertNetworkGroup THEN row is Loading with representative id`() {
            // Arrange
            val currency = createCurrency(id = "coin-eth", symbol = "ETH")
            val statuses = listOf(createStatus(currency, CryptoCurrencyStatus.Loading))
            val converter = createConverter(totalFiatBalance = BigDecimal("1000"))

            // Act
            val result = converter.convertNetworkGroup(statuses)

            // Assert
            assertThat(result).isEqualTo(TangemTokenRowUM.Loading(id = "coin-eth"))
        }

        @Test
        fun `GIVEN single loaded status WHEN convertNetworkGroup THEN row is Content with its amounts`() {
            // Arrange
            val currency = createCurrency(id = "coin-eth", symbol = "ETH", networkName = "Ethereum")
            val statuses = listOf(
                createStatus(currency, loadedValue(amount = BigDecimal("2"), fiatAmount = BigDecimal("400"))),
            )
            val converter = createConverter(totalFiatBalance = BigDecimal("1000"))

            // Act
            val result = converter.convertNetworkGroup(statuses) as TangemTokenRowUM.Content

            // Assert
            assertThat(result.id).isEqualTo("coin-eth")
            val topEnd = result.topEndContentUM as TangemTokenRowUM.EndContentUM.Content
            val bottomEnd = result.bottomEndContentUM as TangemTokenRowUM.EndContentUM.Content
            assertThat(topEnd.text).isEqualTo(BigDecimal("400").toForYouFiatText(appCurrency))
            assertThat(bottomEnd.text).isEqualTo(BigDecimal("400").toForYouPercentText(BigDecimal("1000")))
        }

        @Test
        fun `GIVEN several statuses of the same asset on one network WHEN convertNetworkGroup THEN amounts are summed`() {
            // Arrange — same asset held in two accounts on the same network aggregates into one row
            val currency = createCurrency(id = "coin-eth", symbol = "ETH", networkName = "Ethereum")
            val statuses = listOf(
                createStatus(currency, loadedValue(amount = BigDecimal("1"), fiatAmount = BigDecimal("200"))),
                createStatus(currency, loadedValue(amount = BigDecimal("2"), fiatAmount = BigDecimal("400"))),
            )
            val converter = createConverter(totalFiatBalance = BigDecimal("1000"))

            // Act
            val result = converter.convertNetworkGroup(statuses) as TangemTokenRowUM.Content

            // Assert
            val topEnd = result.topEndContentUM as TangemTokenRowUM.EndContentUM.Content
            assertThat(topEnd.text).isEqualTo(BigDecimal("600").toForYouFiatText(appCurrency))
        }

        @Test
        fun `GIVEN mixed Loading and Loaded statuses WHEN convertNetworkGroup THEN row is Content`() {
            // Arrange — not *all* statuses are Loading, so it should not collapse to a Loading row
            val currency = createCurrency(id = "coin-eth", symbol = "ETH")
            val statuses = listOf(
                createStatus(currency, CryptoCurrencyStatus.Loading),
                createStatus(currency, loadedValue(amount = BigDecimal("1"), fiatAmount = BigDecimal("100"))),
            )
            val converter = createConverter(totalFiatBalance = BigDecimal("1000"))

            // Act
            val result = converter.convertNetworkGroup(statuses)

            // Assert
            assertThat(result).isInstanceOf(TangemTokenRowUM.Content::class.java)
        }
    }

    private fun createConverter(totalFiatBalance: BigDecimal) = ForYouTokenRowConverter(
        appCurrency = appCurrency,
        totalFiatBalance = totalFiatBalance,
    )

    private fun createStatus(currency: CryptoCurrency, value: CryptoCurrencyStatus.Value) = CryptoCurrencyStatus(
        currency = currency,
        value = value,
    )

    private fun loadedValue(amount: BigDecimal, fiatAmount: BigDecimal): CryptoCurrencyStatus.Loaded = mockk {
        every { this@mockk.amount } returns amount
        every { this@mockk.fiatAmount } returns fiatAmount
        every { isError } returns false
    }

    private fun createCurrency(
        id: String,
        symbol: String,
        networkName: String = "Network",
    ): CryptoCurrency {
        val network: Network = mockk {
            every { name } returns networkName
            every { isTestnet } returns false
            every { this@mockk.id } returns mockk { every { rawId } returns Network.RawID(id) }
        }
        val currencyId: CryptoCurrency.ID = mockk {
            every { value } returns id
            every { rawCurrencyId } returns null
        }
        return mockk<CryptoCurrency.Coin> {
            every { this@mockk.id } returns currencyId
            every { this@mockk.symbol } returns symbol
            every { this@mockk.network } returns network
            every { this@mockk.decimals } returns 8
            every { isCustom } returns false
            every { iconUrl } returns null
        }
    }
}