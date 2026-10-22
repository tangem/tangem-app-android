package com.tangem.features.tangempay.withdraw.bank.model

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.pay.model.bankofframp.OfframpDestination
import com.tangem.features.tangempay.withdraw.bank.BASE_CHAIN_ID
import com.tangem.features.tangempay.withdraw.bank.BSC_CHAIN_ID
import com.tangem.features.tangempay.withdraw.bank.USDC_BASE
import com.tangem.features.tangempay.withdraw.bank.USDC_BSC
import com.tangem.features.tangempay.withdraw.bank.bankWithdrawOptions
import com.tangem.features.tangempay.withdraw.bank.bankWithdrawSource
import com.tangem.features.tangempay.withdraw.bank.offrampDestination
import com.tangem.features.tangempay.withdraw.bank.paymentToken
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.NullSource
import org.junit.jupiter.params.provider.ValueSource
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class BankWithdrawOptionsFactoryTest {

    private val usdcPolygon = paymentToken()
    private val usdcBase = paymentToken(networkId = "base", networkName = "Base", contractAddress = USDC_BASE)
    private val usdcBsc = paymentToken(networkId = "bsc", networkName = "BNB Smart Chain", contractAddress = USDC_BSC)
    private val usdtPolygon = paymentToken(contractAddress = "0xc2132D05D31c914a87C6611C10748AEb04B58e8F", symbol = "USDT")
    private val usdtBase = paymentToken(
        networkId = "base",
        networkName = "Base",
        contractAddress = "0xfde4C96c8593536E31F229EA8f37b2ADa2699bb2",
        symbol = "USDT",
    )
    private val daiPolygon = paymentToken(
        contractAddress = "0x8f3Cf7ad23Cd3CaDbD9735AFf958023239c6A063",
        symbol = "DAI",
    )

    private val destinations = bankWithdrawOptions().destinations

    @Test
    fun `GIVEN available sources WHEN create THEN they come with the rails, the bounds and the account currency`() {
        // Arrange
        val sources = listOf(
            bankWithdrawSource(token = usdcPolygon, fiatBalance = BigDecimal("2325.55")),
            bankWithdrawSource(token = usdcBase, chainId = BASE_CHAIN_ID, fiatBalance = BigDecimal("12325.55")),
        )

        // Act
        val actual = BankWithdrawOptionsFactory.create(destinations, sources, balanceCurrencyCode = "USD")

        // Assert
        assertThat(actual).isEqualTo(bankWithdrawOptions(sources = sources))
    }

    @ParameterizedTest
    @MethodSource("provideSortModels")
    fun `GIVEN sources WHEN create THEN available ones lead and each part is grouped by symbol with USDC first`(
        model: SortModel,
    ) {
        // Act
        val actual = BankWithdrawOptionsFactory.create(destinations, model.sources, balanceCurrencyCode = "USD")

        // Assert
        assertThat(actual).isEqualTo(bankWithdrawOptions(sources = model.expected))
    }

    @ParameterizedTest
    @MethodSource("provideNoOptionsModels")
    fun `GIVEN no rail or no available source WHEN create THEN null`(model: NoOptionsModel) {
        // Act
        val actual = BankWithdrawOptionsFactory.create(model.destinations, model.sources, balanceCurrencyCode = "USD")

        // Assert
        assertThat(actual).isNull()
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = ["XXXX"])
    fun `GIVEN account currency the platform does not know WHEN create THEN the rail fee currency is used`(
        balanceCurrencyCode: String?,
    ) {
        // Arrange
        val sources = listOf(bankWithdrawSource(token = usdcPolygon))
        val rails = listOf(offrampDestination())

        // Act
        val actual = BankWithdrawOptionsFactory.create(rails, sources, balanceCurrencyCode)

        // Assert
        assertThat(actual).isEqualTo(bankWithdrawOptions(sources = sources, destinations = rails))
    }

    private fun provideSortModels(): List<SortModel> {
        val usdcPolygonAvailable = bankWithdrawSource(token = usdcPolygon, fiatBalance = BigDecimal("100"))
        val usdtPolygonUnavailable = bankWithdrawSource(
            token = usdtPolygon,
            fiatBalance = BigDecimal("5"),
            isAvailable = false,
        )
        val usdcBaseUnavailable = bankWithdrawSource(
            token = usdcBase,
            chainId = BASE_CHAIN_ID,
            fiatBalance = BigDecimal("40"),
            isAvailable = false,
        )
        val usdcBaseAvailable = bankWithdrawSource(token = usdcBase, chainId = BASE_CHAIN_ID)
        val usdcBscUnavailable = bankWithdrawSource(token = usdcBsc, chainId = BSC_CHAIN_ID, isAvailable = false)
        val usdtPolygonAvailable = bankWithdrawSource(token = usdtPolygon)
        val usdtBaseAvailable = bankWithdrawSource(token = usdtBase, chainId = BASE_CHAIN_ID)
        val daiPolygonAvailable = bankWithdrawSource(token = daiPolygon)
        val usdcPolygonDefault = bankWithdrawSource(token = usdcPolygon)
        return listOf(
            SortModel(
                description = "unavailable sources follow the available one, USDC first among them",
                sources = listOf(usdtPolygonUnavailable, usdcPolygonAvailable, usdcBaseUnavailable),
                expected = listOf(usdcPolygonAvailable, usdcBaseUnavailable, usdtPolygonUnavailable),
            ),
            SortModel(
                description = "an unavailable source between available ones moves after them",
                sources = listOf(usdcPolygonDefault, usdcBscUnavailable, usdcBaseAvailable),
                expected = listOf(usdcPolygonDefault, usdcBaseAvailable, usdcBscUnavailable),
            ),
            SortModel(
                description = "an available USDT comes before an unavailable USDC",
                sources = listOf(usdcBaseUnavailable, usdtPolygonAvailable),
                expected = listOf(usdtPolygonAvailable, usdcBaseUnavailable),
            ),
            SortModel(
                description = "USDC comes first",
                sources = listOf(usdtPolygonAvailable, usdcPolygonDefault),
                expected = listOf(usdcPolygonDefault, usdtPolygonAvailable),
            ),
            SortModel(
                description = "other symbols keep the order they first appear in",
                sources = listOf(usdtPolygonAvailable, daiPolygonAvailable, usdtBaseAvailable),
                expected = listOf(usdtPolygonAvailable, usdtBaseAvailable, daiPolygonAvailable),
            ),
            SortModel(
                description = "one symbol on several networks keeps the given order",
                sources = listOf(usdcBaseAvailable, usdcPolygonDefault),
                expected = listOf(usdcBaseAvailable, usdcPolygonDefault),
            ),
        )
    }

    private fun provideNoOptionsModels(): List<NoOptionsModel> = listOf(
        NoOptionsModel(
            description = "no rail",
            destinations = emptyList(),
            sources = listOf(bankWithdrawSource(token = usdcPolygon)),
        ),
        NoOptionsModel(
            description = "no source",
            destinations = destinations,
            sources = emptyList(),
        ),
        NoOptionsModel(
            description = "only sources the off-ramp does not accept",
            destinations = destinations,
            sources = listOf(bankWithdrawSource(token = usdtPolygon, isAvailable = false)),
        ),
    )

    internal data class SortModel(
        val description: String,
        val sources: List<BankWithdrawSource>,
        val expected: List<BankWithdrawSource>,
    )

    internal data class NoOptionsModel(
        val description: String,
        val destinations: List<OfframpDestination>,
        val sources: List<BankWithdrawSource>,
    )
}