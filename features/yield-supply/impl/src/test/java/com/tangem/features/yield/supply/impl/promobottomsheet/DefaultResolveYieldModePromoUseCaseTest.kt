package com.tangem.features.yield.supply.impl.promobottomsheet

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.currency.yieldSupplyKey
import com.tangem.domain.models.earn.EarnToken
import com.tangem.domain.models.earn.EarnYieldOffers
import com.tangem.domain.models.network.Network
import com.tangem.features.yield.supply.api.promobottomsheet.YieldModePromoOffer
import com.tangem.features.yield.supply.api.promobottomsheet.YieldModePromoResolution
import com.tangem.test.core.ProvideTestModels
import com.tangem.test.mock.earn.MockEarn
import com.tangem.test.mock.yield.MockYieldSupply
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DefaultResolveYieldModePromoUseCaseTest {

    private val useCase = DefaultResolveYieldModePromoUseCase()

    private val usdt = MockYieldSupply.token(contractAddress = "0xA0", symbol = "USDT")
    private val usdc = MockYieldSupply.token(contractAddress = "0xB0", symbol = "USDC")

    @Test
    fun `GIVEN eligible tokens WHEN invoke THEN offers the highest APY and the Earn top row`() {
        // Arrange
        val statuses = listOf(MockYieldSupply.loadedStatus(usdt), MockYieldSupply.loadedStatus(usdc))
        val yieldSupplyApy = mapOf(
            usdt.yieldSupplyKey() to BigDecimal("3.10"),
            usdc.yieldSupplyKey() to BigDecimal("6.12"),
        )
        val earnOffers = EarnYieldOffers(
            total = 36,
            best = MockEarn.yieldToken(apy = "0.1316", networkId = "arbitrum-one", tokenSymbol = "GHO"),
        )

        // Act
        val actual = useCase(statuses = statuses, yieldSupplyApy = yieldSupplyApy, earnOffers = earnOffers)

        // Assert
        val expected = YieldModePromoResolution.Ready(
            userMaxApy = offer(apy = "6.12", tokenSymbol = "USDC"),
            globalMaxApy = offer(apy = "13.16", tokenSymbol = "GHO", blockchain = "Arbitrum One"),
        )
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `GIVEN APYs with more decimals WHEN invoke THEN both offers round them half up to two`() {
        // Arrange
        val yieldSupplyApy = mapOf(usdt.yieldSupplyKey() to BigDecimal("3.3550"))
        val earnOffers = EarnYieldOffers(total = 36, best = MockEarn.yieldToken(apy = "0.240300"))

        // Act
        val actual = useCase(
            statuses = listOf(MockYieldSupply.loadedStatus(usdt)),
            yieldSupplyApy = yieldSupplyApy,
            earnOffers = earnOffers,
        )

        // Assert
        val expected = YieldModePromoResolution.Ready(
            userMaxApy = offer(apy = "3.36", tokenSymbol = "USDT"),
            globalMaxApy = offer(apy = "24.03", tokenSymbol = "USDT"),
        )
        assertThat(actual).isEqualTo(expected)
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN equal APYs WHEN invoke THEN the tie-break picks the candidate`(model: TieBreakModel) {
        // Arrange
        val yieldSupplyApy = model.statuses.associate { status ->
            (status.currency as CryptoCurrency.Token).yieldSupplyKey() to BigDecimal("5")
        }

        // Act
        val actual = useCase(statuses = model.statuses, yieldSupplyApy = yieldSupplyApy, earnOffers = earnOffers())

        // Assert
        assertThat(actual).isEqualTo(ready(userMaxApy = model.expected))
    }

    @Test
    fun `GIVEN one token in two accounts WHEN invoke THEN their balances are summed`() {
        // Arrange
        val statuses = listOf(
            MockYieldSupply.loadedStatus(usdt, amount = BigDecimal.ZERO, fiatAmount = BigDecimal.ZERO),
            MockYieldSupply.loadedStatus(usdtInCardAccount, amount = BigDecimal("2"), fiatAmount = BigDecimal("2")),
        )

        // Act
        val actual = useCase(statuses = statuses, yieldSupplyApy = apyOf(usdt), earnOffers = earnOffers())

        // Assert
        assertThat(actual).isEqualTo(ready(userMaxApy = offer(apy = "5.00", tokenSymbol = "USDT")))
    }

    @ParameterizedTest
    @MethodSource("provideNoEligibleTokenModels")
    fun `GIVEN no eligible position WHEN invoke THEN no eligible token`(model: NoEligibleModel) {
        // Act
        val actual = useCase(
            statuses = model.statuses,
            yieldSupplyApy = model.yieldSupplyApy,
            earnOffers = model.earnOffers,
        )

        // Assert
        assertThat(actual).isEqualTo(YieldModePromoResolution.NoEligibleToken)
    }

    @ParameterizedTest
    @MethodSource("provideStillLoadingStatuses")
    fun `GIVEN a token still loading WHEN invoke THEN waiting`(stillLoading: CryptoCurrencyStatus) {
        // Act
        val actual = useCase(
            statuses = listOf(MockYieldSupply.loadedStatus(usdt), stillLoading),
            yieldSupplyApy = apyOf(usdt),
            earnOffers = earnOffers(),
        )

        // Assert
        assertThat(actual).isEqualTo(YieldModePromoResolution.Waiting)
    }

    @ParameterizedTest
    @MethodSource("provideUnusableTopRows")
    fun `GIVEN no usable Earn top row WHEN invoke THEN data not ready`(best: EarnToken?) {
        // Act
        val actual = useCase(
            statuses = listOf(MockYieldSupply.loadedStatus(usdt)),
            yieldSupplyApy = apyOf(usdt),
            earnOffers = EarnYieldOffers(total = 36, best = best),
        )

        // Assert
        assertThat(actual).isEqualTo(YieldModePromoResolution.DataNotReady)
    }

    @Test
    fun `GIVEN an Earn top row on an unknown network WHEN invoke THEN the network id names the blockchain`() {
        // Arrange
        val earnOffers = EarnYieldOffers(total = 36, best = MockEarn.yieldToken(networkId = "unknown-chain"))

        // Act
        val actual = useCase(
            statuses = listOf(MockYieldSupply.loadedStatus(usdt)),
            yieldSupplyApy = apyOf(usdt),
            earnOffers = earnOffers,
        )

        // Assert
        val expected = YieldModePromoResolution.Ready(
            userMaxApy = offer(apy = "5.00", tokenSymbol = "USDT"),
            globalMaxApy = offer(apy = "9.20", tokenSymbol = "USDT", blockchain = "unknown-chain"),
        )
        assertThat(actual).isEqualTo(expected)
    }

    private fun provideTestModels() = listOf(
        TieBreakModel(
            statuses = listOf(
                MockYieldSupply.loadedStatus(usdt, fiatAmount = BigDecimal("10")),
                MockYieldSupply.loadedStatus(usdc, fiatAmount = BigDecimal("20")),
            ),
            expected = offer(apy = "5.00", tokenSymbol = "USDC"),
        ),
        TieBreakModel(
            statuses = listOf(
                MockYieldSupply.loadedStatus(
                    MockYieldSupply.token(contractAddress = "0xA0", symbol = "ARB_USDT", network = arbitrumNetwork),
                ),
                MockYieldSupply.loadedStatus(MockYieldSupply.token(contractAddress = "0xF0", symbol = "ETH_USDT")),
            ),
            expected = offer(apy = "5.00", tokenSymbol = "ETH_USDT"),
        ),
        TieBreakModel(
            statuses = listOf(MockYieldSupply.loadedStatus(usdc), MockYieldSupply.loadedStatus(usdt)),
            expected = offer(apy = "5.00", tokenSymbol = "USDT"),
        ),
    )

    private fun provideNoEligibleTokenModels() = listOf(
        NoEligibleModel(
            statuses = listOf(
                MockYieldSupply.loadedStatus(usdt),
                MockYieldSupply.loadedStatus(usdc, yieldSupplyStatus = MockYieldSupply.activeYieldSupplyStatus()),
            ),
        ),
        NoEligibleModel(statuses = listOf(MockYieldSupply.loadedStatus(usdt)), yieldSupplyApy = emptyMap()),
        NoEligibleModel(
            statuses = listOf(MockYieldSupply.loadedStatus(MockYieldSupply.coin())),
            yieldSupplyApy = mapOf("${MockYieldSupply.NETWORK_ID}_${MockYieldSupply.NETWORK_ID}" to BigDecimal("5")),
        ),
        NoEligibleModel(
            statuses = listOf(
                MockYieldSupply.loadedStatus(MockYieldSupply.token(contractAddress = "0xA0", network = jointNetwork)),
            ),
        ),
        NoEligibleModel(
            statuses = listOf(MockYieldSupply.loadedStatus(usdt)),
            yieldSupplyApy = mapOf(usdt.yieldSupplyKey() to BigDecimal.ZERO),
        ),
        NoEligibleModel(statuses = listOf(MockYieldSupply.loadedStatus(usdt, amount = BigDecimal.ZERO))),
        NoEligibleModel(
            statuses = listOf(
                MockYieldSupply.loadedStatus(usdt, amount = BigDecimal("100")),
                CryptoCurrencyStatus(
                    usdtInCardAccount,
                    CryptoCurrencyStatus.Unreachable(priceChange = null, fiatRate = null, networkAddress = null),
                ),
            ),
        ),
        NoEligibleModel(
            statuses = listOf(
                MockYieldSupply.loadedStatus(usdt, amount = BigDecimal("100"), networkSource = StatusSource.ONLY_CACHE),
            ),
        ),
        NoEligibleModel(
            statuses = listOf(MockYieldSupply.loadedStatus(usdt)),
            earnOffers = EarnYieldOffers(total = 0, best = null),
        ),
    )

    private fun provideStillLoadingStatuses() = listOf(
        CryptoCurrencyStatus(usdtInCardAccount, CryptoCurrencyStatus.Loading),
        MockYieldSupply.loadedStatus(usdtInCardAccount, networkSource = StatusSource.CACHE),
        CryptoCurrencyStatus(usdc, CryptoCurrencyStatus.Loading),
        MockYieldSupply.loadedStatus(
            usdc,
            yieldSupplyStatus = MockYieldSupply.activeYieldSupplyStatus(),
            networkSource = StatusSource.CACHE,
        ),
    )

    private fun provideUnusableTopRows() = listOf(null, MockEarn.yieldToken(apy = "0"))

    private fun apyOf(token: CryptoCurrency.Token) = mapOf(token.yieldSupplyKey() to BigDecimal("5"))

    private fun earnOffers() = EarnYieldOffers(total = 36, best = MockEarn.yieldToken())

    private fun ready(userMaxApy: YieldModePromoOffer) = YieldModePromoResolution.Ready(
        userMaxApy = userMaxApy,
        globalMaxApy = offer(apy = "9.20", tokenSymbol = "USDT"),
    )

    internal data class TieBreakModel(val statuses: List<CryptoCurrencyStatus>, val expected: YieldModePromoOffer)

    internal data class NoEligibleModel(
        val statuses: List<CryptoCurrencyStatus>,
        val yieldSupplyApy: Map<String, BigDecimal> = mapOf(
            MockYieldSupply.token(contractAddress = "0xA0").yieldSupplyKey() to BigDecimal("5"),
        ),
        val earnOffers: EarnYieldOffers = EarnYieldOffers(total = 36, best = MockEarn.yieldToken()),
    )

    private companion object {
        val cardNetwork = MockYieldSupply.network(derivationPath = Network.DerivationPath.Card("m/44'/60'/1'/0/0"))
        val jointNetwork = MockYieldSupply.network(derivationPath = Network.DerivationPath.Joint("m/44'/60'/0'/0/0"))
        val arbitrumNetwork = MockYieldSupply.network(id = "arbitrum-one", name = "Arbitrum One")
        val usdtInCardAccount = MockYieldSupply.token(contractAddress = "0xA0", symbol = "USDT", network = cardNetwork)

        fun offer(apy: String, tokenSymbol: String, blockchain: String = "Ethereum") = YieldModePromoOffer(
            apy = BigDecimal(apy),
            tokensCount = 36,
            tokenSymbol = tokenSymbol,
            blockchain = blockchain,
        )
    }
}