package com.tangem.features.foryou.impl.model.converter

import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.ds.row.token.TangemTokenRowUM
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.Network
import com.tangem.features.foryou.impl.R
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.math.BigDecimal

internal class ForYouTokenListConverterTest {

    private val appCurrency: AppCurrency = AppCurrency.Default

    @Nested
    inner class Convert {

        @Test
        fun `GIVEN single-network coin WHEN convert THEN subtitle is common main network`() {
            // Arrange
            val currency = createCoin(rawCurrencyId = "bitcoin", symbol = "BTC", networkId = "bitcoin")
            val status = createStatus(currency, loadedValue(BigDecimal("1"), BigDecimal("100")))
            val converter = createConverter(totalFiatBalance = BigDecimal("100"))

            // Act
            val result = converter.convert(listOf(status))

            // Assert
            val row = result.single().tokenRowUM as TangemTokenRowUM.Content
            val subtitle = row.subtitleUM as TangemTokenRowUM.SubtitleUM.Content
            assertThat(subtitle.text).isEqualTo(resourceReference(R.string.common_main_network))
        }

        @Test
        fun `GIVEN single-network token WHEN convert THEN subtitle is the network standard type name`() {
            // Arrange
            val currency = createToken(
                rawCurrencyId = "usdc",
                symbol = "USDC",
                networkId = "ethereum",
                standardTypeName = "ERC20",
            )
            val status = createStatus(currency, loadedValue(BigDecimal("1"), BigDecimal("100")))
            val converter = createConverter(totalFiatBalance = BigDecimal("100"))

            // Act
            val result = converter.convert(listOf(status))

            // Assert
            val row = result.single().tokenRowUM as TangemTokenRowUM.Content
            val subtitle = row.subtitleUM as TangemTokenRowUM.SubtitleUM.Content
            assertThat(subtitle.text).isEqualTo(stringReference("ERC20"))
        }

        @Test
        fun `GIVEN asset spans multiple networks WHEN convert THEN subtitle shows network count`() {
            // Arrange — same asset (shared rawCurrencyId) on two different networks
            val onEth = createToken(rawCurrencyId = "usdc", symbol = "USDC", networkId = "ethereum")
            val onSol = createToken(rawCurrencyId = "usdc", symbol = "USDC", networkId = "solana")
            val statusEth = createStatus(onEth, loadedValue(BigDecimal("1"), BigDecimal("100")))
            val statusSol = createStatus(onSol, loadedValue(BigDecimal("2"), BigDecimal("200")))
            val converter = createConverter(
                totalFiatBalance = BigDecimal("300"),
            )

            // Act
            val result = converter.convert(listOf(statusEth, statusSol))

            // Assert
            val item = result.single()
            val row = item.tokenRowUM as TangemTokenRowUM.Content
            val subtitle = row.subtitleUM as TangemTokenRowUM.SubtitleUM.Content
            assertThat(subtitle.text).isEqualTo(stringReference("2 networks"))
            assertThat(item.tokenList).hasSize(2)
        }

        @Test
        fun `GIVEN multi-network asset WHEN convert THEN child rows ordered by descending fiat balance`() {
            // Arrange
            val onEth = createToken(rawCurrencyId = "usdc", symbol = "USDC", networkId = "ethereum")
            val onSol = createToken(rawCurrencyId = "usdc", symbol = "USDC", networkId = "solana")
            val statusEth = createStatus(onEth, loadedValue(BigDecimal("1"), BigDecimal("100")))
            val statusSol = createStatus(onSol, loadedValue(BigDecimal("2"), BigDecimal("500")))
            val converter = createConverter(
                totalFiatBalance = BigDecimal("600"),
            )

            // Act
            val result = converter.convert(listOf(statusEth, statusSol))

            // Assert — Solana holding (500) ranks above Ethereum holding (100)
            val childIds = result.single().tokenList.map { it.id }
            assertThat(childIds).containsExactly("token-usdc-solana", "token-usdc-ethereum").inOrder()
        }

        @Test
        fun `GIVEN all statuses of an asset are Loading WHEN convert THEN asset row is Loading`() {
            // Arrange
            val currency = createCoin(rawCurrencyId = "bitcoin", symbol = "BTC", networkId = "bitcoin")
            val status = createStatus(currency, CryptoCurrencyStatus.Loading)
            val converter = createConverter(totalFiatBalance = BigDecimal.ZERO)

            // Act
            val result = converter.convert(listOf(status))

            // Assert
            assertThat(result.single().tokenRowUM).isInstanceOf(TangemTokenRowUM.Loading::class.java)
        }

        @Test
        fun `GIVEN otherAssetCount is zero WHEN convert THEN no Other row is appended`() {
            // Arrange
            val currency = createCoin(rawCurrencyId = "bitcoin", symbol = "BTC", networkId = "bitcoin")
            val status = createStatus(currency, loadedValue(BigDecimal("1"), BigDecimal("100")))
            val converter = createConverter(
                totalFiatBalance = BigDecimal("100"),
                otherAssetCount = 0,
            )

            // Act
            val result = converter.convert(listOf(status))

            // Assert
            assertThat(result).hasSize(1)
        }

        @Test
        fun `GIVEN otherAssetCount is one WHEN convert THEN Other row subtitle is singular`() {
            // Arrange
            val currency = createCoin(rawCurrencyId = "bitcoin", symbol = "BTC", networkId = "bitcoin")
            val status = createStatus(currency, loadedValue(BigDecimal("1"), BigDecimal("100")))
            val converter = createConverter(
                totalFiatBalance = BigDecimal("100"),
                otherAssetCount = 1,
                otherFiatBalance = BigDecimal("50"),
            )

            // Act
            val result = converter.convert(listOf(status))

            // Assert
            val otherRow = result.last().tokenRowUM as TangemTokenRowUM.Content
            assertThat(otherRow.id).isEqualTo("for_you_other_assets")
            val subtitle = otherRow.subtitleUM as TangemTokenRowUM.SubtitleUM.Content
            assertThat(subtitle.text).isEqualTo(stringReference("1 asset"))
        }

        @Test
        fun `GIVEN otherAssetCount is more than one WHEN convert THEN Other row subtitle is plural`() {
            // Arrange
            val currency = createCoin(rawCurrencyId = "bitcoin", symbol = "BTC", networkId = "bitcoin")
            val status = createStatus(currency, loadedValue(BigDecimal("1"), BigDecimal("100")))
            val converter = createConverter(
                totalFiatBalance = BigDecimal("100"),
                otherAssetCount = 3,
                otherFiatBalance = BigDecimal("50"),
            )

            // Act
            val result = converter.convert(listOf(status))

            // Assert
            val otherRow = result.last().tokenRowUM as TangemTokenRowUM.Content
            val subtitle = otherRow.subtitleUM as TangemTokenRowUM.SubtitleUM.Content
            assertThat(subtitle.text).isEqualTo(stringReference("3 assets"))
        }

        @Test
        fun `GIVEN asset id in expandedAssetIds WHEN convert THEN item isExpanded is true`() {
            // Arrange
            val currency = createCoin(rawCurrencyId = "bitcoin", symbol = "BTC", networkId = "bitcoin")
            val status = createStatus(currency, loadedValue(BigDecimal("1"), BigDecimal("100")))
            val converter = createConverter(
                totalFiatBalance = BigDecimal("100"),
                expandedAssetIds = setOf("bitcoin"),
            )

            // Act
            val result = converter.convert(listOf(status))

            // Assert
            assertThat(result.single().isExpanded).isTrue()
        }

        @Test
        fun `GIVEN asset id not in expandedAssetIds WHEN convert THEN item isExpanded is false`() {
            // Arrange
            val currency = createCoin(rawCurrencyId = "bitcoin", symbol = "BTC", networkId = "bitcoin")
            val status = createStatus(currency, loadedValue(BigDecimal("1"), BigDecimal("100")))
            val converter = createConverter(
                totalFiatBalance = BigDecimal("100"),
                expandedAssetIds = emptySet(),
            )

            // Act
            val result = converter.convert(listOf(status))

            // Assert
            assertThat(result.single().isExpanded).isFalse()
        }
    }

    private fun createConverter(
        totalFiatBalance: BigDecimal,
        expandedAssetIds: Set<String> = emptySet(),
        otherAssetCount: Int = 0,
        otherFiatBalance: BigDecimal = BigDecimal.ZERO,
    ): ForYouTokenListConverter = ForYouTokenListConverter(
        appCurrency = appCurrency,
        totalFiatBalance = totalFiatBalance,
        expandedAssetIds = expandedAssetIds,
        expandClick = {},
        otherAssetCount = otherAssetCount,
        otherFiatBalance = otherFiatBalance,
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

    private fun createCoin(rawCurrencyId: String, symbol: String, networkId: String): CryptoCurrency.Coin {
        val network = createNetwork(networkId = networkId, standardTypeName = "MAIN")
        val currencyId = createCurrencyId(idValue = "coin-$rawCurrencyId-$networkId", rawCurrencyId = rawCurrencyId)
        return mockk<CryptoCurrency.Coin> {
            every { this@mockk.id } returns currencyId
            every { this@mockk.symbol } returns symbol
            every { this@mockk.network } returns network
            every { this@mockk.decimals } returns 8
            every { isCustom } returns false
            every { iconUrl } returns null
        }
    }

    private fun createToken(
        rawCurrencyId: String,
        symbol: String,
        networkId: String,
        standardTypeName: String = "ERC20",
    ): CryptoCurrency.Token {
        val network = createNetwork(networkId = networkId, standardTypeName = standardTypeName)
        val currencyId = createCurrencyId(idValue = "token-$rawCurrencyId-$networkId", rawCurrencyId = rawCurrencyId)
        return mockk<CryptoCurrency.Token> {
            every { this@mockk.id } returns currencyId
            every { this@mockk.symbol } returns symbol
            every { this@mockk.network } returns network
            every { this@mockk.decimals } returns 6
            every { isCustom } returns false
            every { iconUrl } returns null
            every { contractAddress } returns "0xCONTRACT"
        }
    }

    private fun createCurrencyId(idValue: String, rawCurrencyId: String): CryptoCurrency.ID = mockk {
        every { value } returns idValue
        every { this@mockk.rawCurrencyId } returns CryptoCurrency.RawID(rawCurrencyId)
    }

    private fun createNetwork(networkId: String, standardTypeName: String): Network {
        val standardType: Network.StandardType = mockk {
            every { name } returns standardTypeName
        }
        return mockk {
            every { id } returns mockk {
                every { rawId } returns Network.RawID(networkId)
            }
            every { name } returns networkId
            every { isTestnet } returns false
            every { this@mockk.standardType } returns standardType
        }
    }
}