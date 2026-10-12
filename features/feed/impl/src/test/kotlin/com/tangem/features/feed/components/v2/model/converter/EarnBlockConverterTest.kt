package com.tangem.features.feed.components.v2.model.converter

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.yield.supply.YieldSupplyStatus
import com.tangem.domain.staking.model.StakingAvailability
import com.tangem.features.feed.ui.v2.state.EarnBlockUM
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.util.Locale

internal class EarnBlockConverterTest {

    @Test
    fun `GIVEN nothing earning WHEN convert THEN onboards with the catalogue rate`() {
        // Arrange
        val usdc = createToken(symbol = "USDC", contractAddress = "0xUSDC")
        val statuses = listOf(createStatus(usdc, fiatAmount = "100"))

        // Act
        val actual = convert(
            statuses = statuses,
            yieldSupplyApy = mapOf(usdc.yieldSupplyKey() to BigDecimal("8")),
            catalogueApy = BigDecimal("0.1264"),
        )

        // Assert
        assertThat(actual).isEqualTo(
            EarnBlockUM(
                pitch = EarnBlockUM.Pitch.Onboarding(apy = "12.64%"),
                target = EarnBlockUM.Target.EarnTab,
            ),
        )
    }

    @Test
    fun `GIVEN no catalogue rate yet WHEN convert THEN pitches without a rate`() {
        // Act
        val actual = convert(statuses = emptyList(), catalogueApy = null)

        // Assert
        assertThat(actual).isEqualTo(
            EarnBlockUM(pitch = EarnBlockUM.Pitch.Plain, target = EarnBlockUM.Target.EarnTab),
        )
    }

    @Test
    fun `GIVEN an active holding WHEN convert THEN pitches that token and leads to For You`() {
        // Arrange
        val usdc = createToken(symbol = "USDC", contractAddress = "0xUSDC")
        val statuses = listOf(createStatus(usdc, fiatAmount = "100", isYieldSupplyActive = true))

        // Act
        val actual = convert(
            statuses = statuses,
            yieldSupplyApy = mapOf(usdc.yieldSupplyKey() to BigDecimal("8")),
            catalogueApy = BigDecimal("0.1264"),
        )

        // Assert
        assertThat(actual).isEqualTo(
            EarnBlockUM(
                pitch = EarnBlockUM.Pitch.Growth(apy = "8.00%", tokenSymbol = "USDC"),
                target = EarnBlockUM.Target.ForYou,
            ),
        )
    }

    @Test
    fun `GIVEN several earnable holdings WHEN convert THEN the best rate is pitched`() {
        // Arrange
        val usdc = createToken(symbol = "USDC", contractAddress = "0xUSDC")
        val usdt = createToken(symbol = "USDT", contractAddress = "0xUSDT")
        val statuses = listOf(
            createStatus(usdc, fiatAmount = "1000", isYieldSupplyActive = true),
            createStatus(usdt, fiatAmount = "10"),
        )

        // Act — USDT pays more despite the far smaller balance
        val actual = convert(
            statuses = statuses,
            yieldSupplyApy = mapOf(
                usdc.yieldSupplyKey() to BigDecimal("4"),
                usdt.yieldSupplyKey() to BigDecimal("9"),
            ),
        )

        // Assert
        assertThat(actual.pitch).isEqualTo(EarnBlockUM.Pitch.Growth(apy = "9.00%", tokenSymbol = "USDT"))
    }

    @Test
    fun `GIVEN equal rates WHEN convert THEN the bigger holding is pitched`() {
        // Arrange
        val usdc = createToken(symbol = "USDC", contractAddress = "0xUSDC")
        val usdt = createToken(symbol = "USDT", contractAddress = "0xUSDT")
        val statuses = listOf(
            createStatus(usdc, fiatAmount = "10", isYieldSupplyActive = true),
            createStatus(usdt, fiatAmount = "1000"),
        )

        // Act
        val actual = convert(
            statuses = statuses,
            yieldSupplyApy = mapOf(
                usdc.yieldSupplyKey() to BigDecimal("5"),
                usdt.yieldSupplyKey() to BigDecimal("5"),
            ),
        )

        // Assert
        assertThat(actual.pitch).isEqualTo(EarnBlockUM.Pitch.Growth(apy = "5.00%", tokenSymbol = "USDT"))
    }

    @Test
    fun `GIVEN earning but no holding worth naming WHEN convert THEN onboarding wording keeps the growth target`() {
        // Arrange — earning on a token whose fiat balance is unknown, so it cannot be named
        val usdc = createToken(symbol = "USDC", contractAddress = "0xUSDC")
        val statuses = listOf(createStatus(usdc, fiatAmount = null, isYieldSupplyActive = true))

        // Act
        val actual = convert(
            statuses = statuses,
            yieldSupplyApy = mapOf(usdc.yieldSupplyKey() to BigDecimal("8")),
            catalogueApy = BigDecimal("0.1264"),
        )

        // Assert
        assertThat(actual).isEqualTo(
            EarnBlockUM(
                pitch = EarnBlockUM.Pitch.Onboarding(apy = "12.64%"),
                target = EarnBlockUM.Target.ForYou,
            ),
        )
    }

    @Test
    fun `GIVEN a holding Earn does not support WHEN convert THEN it is never pitched`() {
        // Arrange
        val usdc = createToken(symbol = "USDC", contractAddress = "0xUSDC")
        val statuses = listOf(createStatus(usdc, fiatAmount = "1000", isYieldSupplyActive = true))

        // Act — no earn instrument lists the token, so nothing about it is known
        val actual = convert(statuses = statuses, catalogueApy = BigDecimal("0.1264"))

        // Assert
        assertThat(actual).isEqualTo(
            EarnBlockUM(
                pitch = EarnBlockUM.Pitch.Onboarding(apy = "12.64%"),
                target = EarnBlockUM.Target.EarnTab,
            ),
        )
    }

    @Test
    fun `GIVEN a locale that spaces its percent sign WHEN convert THEN the rate carries no space`() {
        // Arrange
        val previousLocale = Locale.getDefault()
        Locale.setDefault(Locale.forLanguageTag("ru"))

        try {
            // Act
            val actual = convert(statuses = emptyList(), catalogueApy = BigDecimal("0.1264"))

            // Assert
            assertThat(actual.pitch).isEqualTo(EarnBlockUM.Pitch.Onboarding(apy = "12,64%"))
        } finally {
            Locale.setDefault(previousLocale)
        }
    }

    private fun convert(
        statuses: List<CryptoCurrencyStatus>,
        yieldSupplyApy: Map<String, BigDecimal> = emptyMap(),
        stakingAvailability: Map<CryptoCurrency, StakingAvailability> = emptyMap(),
        catalogueApy: BigDecimal? = null,
    ): EarnBlockUM = EarnBlockConverter(
        yieldSupplyApy = yieldSupplyApy,
        stakingAvailability = stakingAvailability,
        catalogueApy = catalogueApy,
    ).convert(statuses)

    private fun CryptoCurrency.Token.yieldSupplyKey(): String = "${network.rawId}_$contractAddress"

    private fun createStatus(
        currency: CryptoCurrency,
        fiatAmount: String?,
        isYieldSupplyActive: Boolean = false,
    ): CryptoCurrencyStatus {
        val yieldSupplyStatus = YieldSupplyStatus(
            isActive = isYieldSupplyActive,
            isInitialized = isYieldSupplyActive,
            isAllowedToSpend = isYieldSupplyActive,
            effectiveProtocolBalance = null,
        )
        val sources = CryptoCurrencyStatus.Sources(
            networkSource = StatusSource.ACTUAL,
            quoteSource = StatusSource.ACTUAL,
            stakingBalanceSource = StatusSource.ACTUAL,
        )

        // A priced holding is Loaded; one whose fiat value is unknown can only be a quote-less status
        val value = if (fiatAmount == null) {
            CryptoCurrencyStatus.NoQuote(
                amount = BigDecimal.ONE,
                stakingBalance = null,
                yieldSupplyStatus = yieldSupplyStatus,
                hasCurrentNetworkTransactions = false,
                pendingTransactions = emptySet(),
                networkAddress = mockk(),
                sources = sources,
            )
        } else {
            CryptoCurrencyStatus.Loaded(
                amount = BigDecimal.ONE,
                fiatAmount = BigDecimal(fiatAmount),
                fiatRate = BigDecimal.ONE,
                priceChange = BigDecimal.ZERO,
                stakingBalance = null,
                yieldSupplyStatus = yieldSupplyStatus,
                hasCurrentNetworkTransactions = false,
                pendingTransactions = emptySet(),
                networkAddress = mockk(),
                sources = sources,
            )
        }

        return CryptoCurrencyStatus(currency = currency, value = value)
    }

    private fun createToken(symbol: String, contractAddress: String): CryptoCurrency.Token = CryptoCurrency.Token(
        id = mockk {
            every { value } returns "token-$symbol"
            every { rawCurrencyId } returns CryptoCurrency.RawID(symbol.lowercase())
        },
        network = createNetwork(networkId = "ethereum"),
        name = symbol,
        symbol = symbol,
        decimals = 6,
        iconUrl = null,
        isCustom = false,
        contractAddress = contractAddress,
    )

    private fun createNetwork(networkId: String): Network = mockk {
        every { id } returns mockk {
            every { rawId } returns Network.RawID(networkId)
        }
        every { name } returns networkId
        every { rawId } returns networkId
        every { isTestnet } returns false
    }
}