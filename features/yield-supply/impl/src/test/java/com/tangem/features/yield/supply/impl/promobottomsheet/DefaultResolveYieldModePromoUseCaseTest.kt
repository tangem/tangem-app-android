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
import com.tangem.features.yield.supply.api.promobottomsheet.YieldModePromoVariant
import com.tangem.test.core.ProvideTestModels
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
        val yieldApys = mapOf(usdt.yieldSupplyKey() to BigDecimal("3.10"), usdc.yieldSupplyKey() to BigDecimal("6.12"))
        val earnOffers = EarnYieldOffers(
            total = 36,
            best = MockYieldSupply.earnToken(apy = "0.1316", networkId = "arbitrum-one", tokenSymbol = "GHO"),
        )

        // Act
        val actual = useCase(statuses = statuses, yieldApys = yieldApys, earnOffers = earnOffers)

        // Assert
        val expected = YieldModePromoResolution.Ready(
            userMaxApy = YieldModePromoOffer(
                apy = BigDecimal("6.12"),
                tokensCount = 36,
                tokenSymbol = "USDC",
                blockchain = "Ethereum",
            ),
            globalMaxApy = YieldModePromoOffer(
                apy = BigDecimal("13.16"),
                tokensCount = 36,
                tokenSymbol = "GHO",
                blockchain = "Arbitrum One",
            ),
        )
        assertThat(actual).isEqualTo(expected)
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN equal APYs WHEN invoke THEN the tie-break picks the candidate`(model: TieBreakModel) {
        // Arrange
        val yieldApys = model.statuses.associate { status ->
            (status.currency as CryptoCurrency.Token).yieldSupplyKey() to BigDecimal("5.00")
        }

        // Act
        val actual = useCase(statuses = model.statuses, yieldApys = yieldApys, earnOffers = earnOffers())

        // Assert
        assertThat(actual.userMaxApy()?.tokenSymbol).isEqualTo(model.expectedSymbol)
    }

    @Test
    fun `GIVEN one token in two accounts WHEN invoke THEN their balances are summed`() {
        // Arrange
        val secondAccountUsdt = MockYieldSupply.token(contractAddress = "0xA0", symbol = "USDT", network = cardNetwork)
        val statuses = listOf(
            MockYieldSupply.loadedStatus(usdt, amount = BigDecimal.ZERO, fiatAmount = BigDecimal.ZERO),
            MockYieldSupply.loadedStatus(secondAccountUsdt, amount = BigDecimal("2"), fiatAmount = BigDecimal("2")),
        )

        // Act
        val actual = useCase(statuses = statuses, yieldApys = apyOf(usdt), earnOffers = earnOffers())

        // Assert
        assertThat(actual.userMaxApy()?.tokenSymbol).isEqualTo("USDT")
    }

    @ParameterizedTest
    @MethodSource("provideNoEligibleTokenModels")
    fun `GIVEN no eligible position WHEN invoke THEN no eligible token`(model: NoEligibleModel) {
        // Act
        val actual = useCase(statuses = model.statuses, yieldApys = model.yieldApys, earnOffers = model.earnOffers)

        // Assert
        assertThat(actual).isEqualTo(YieldModePromoResolution.NoEligibleToken)
    }

    @ParameterizedTest
    @MethodSource("provideWaitingModels")
    fun `GIVEN a matched token still loading WHEN invoke THEN waiting`(model: WaitingModel) {
        // Act
        val actual = useCase(
            statuses = listOf(model.status, MockYieldSupply.loadedStatus(usdc)),
            yieldApys = apyOf(usdt) + apyOf(usdc),
            earnOffers = earnOffers(),
        )

        // Assert
        assertThat(actual).isEqualTo(YieldModePromoResolution.Waiting)
    }

    @Test
    fun `GIVEN an unmatched token still loading WHEN invoke THEN it does not hold the decision`() {
        // Arrange
        val statuses = listOf(
            CryptoCurrencyStatus(usdc, CryptoCurrencyStatus.Loading),
            MockYieldSupply.loadedStatus(usdt),
        )

        // Act
        val actual = useCase(statuses = statuses, yieldApys = apyOf(usdt), earnOffers = earnOffers())

        // Assert
        assertThat(actual.userMaxApy()?.tokenSymbol).isEqualTo("USDT")
    }

    @ParameterizedTest
    @MethodSource("provideDataNotReadyModels")
    fun `GIVEN no usable Earn top row WHEN invoke THEN data not ready`(best: EarnToken?) {
        // Act
        val actual = useCase(
            statuses = listOf(MockYieldSupply.loadedStatus(usdt)),
            yieldApys = apyOf(usdt),
            earnOffers = EarnYieldOffers(total = 36, best = best),
        )

        // Assert
        assertThat(actual).isEqualTo(YieldModePromoResolution.DataNotReady)
    }

    @Test
    fun `GIVEN an Earn top row on an unknown network WHEN invoke THEN the network id names the blockchain`() {
        // Arrange
        val earnOffers = EarnYieldOffers(total = 36, best = MockYieldSupply.earnToken(networkId = "unknown-chain"))

        // Act
        val actual = useCase(
            statuses = listOf(MockYieldSupply.loadedStatus(usdt)),
            yieldApys = apyOf(usdt),
            earnOffers = earnOffers,
        )

        // Assert
        val ready = actual as YieldModePromoResolution.Ready
        assertThat(ready.globalMaxApy.blockchain).isEqualTo("unknown-chain")
    }

    @ParameterizedTest
    @MethodSource("provideVariantModels")
    fun `GIVEN a ready resolution WHEN offerFor THEN the variant picks its arm`(model: VariantModel) {
        // Arrange
        val ready = YieldModePromoResolution.Ready(userMaxApy = userOffer, globalMaxApy = globalOffer)

        // Act
        val actual = ready.offerFor(model.variant)

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    private fun provideTestModels() = listOf(
        TieBreakModel(
            statuses = listOf(
                MockYieldSupply.loadedStatus(usdt, fiatAmount = BigDecimal("10")),
                MockYieldSupply.loadedStatus(usdc, fiatAmount = BigDecimal("20")),
            ),
            expectedSymbol = "USDC",
        ),
        TieBreakModel(
            statuses = listOf(
                MockYieldSupply.loadedStatus(
                    MockYieldSupply.token(contractAddress = "0xA0", symbol = "ARB_USDT", network = arbitrumNetwork),
                ),
                MockYieldSupply.loadedStatus(MockYieldSupply.token(contractAddress = "0xF0", symbol = "ETH_USDT")),
            ),
            expectedSymbol = "ETH_USDT",
        ),
        TieBreakModel(
            statuses = listOf(MockYieldSupply.loadedStatus(usdc), MockYieldSupply.loadedStatus(usdt)),
            expectedSymbol = "USDT",
        ),
    )

    private fun provideNoEligibleTokenModels() = listOf(
        NoEligibleModel(
            statuses = listOf(
                MockYieldSupply.loadedStatus(usdt),
                MockYieldSupply.loadedStatus(usdc, yieldSupplyStatus = MockYieldSupply.yieldSupplyStatus()),
            ),
            yieldApys = apyOf(usdt),
        ),
        NoEligibleModel(statuses = listOf(MockYieldSupply.loadedStatus(usdt)), yieldApys = emptyMap()),
        NoEligibleModel(
            statuses = listOf(MockYieldSupply.loadedStatus(MockYieldSupply.coin())),
            yieldApys = mapOf("${MockYieldSupply.NETWORK_ID}_${MockYieldSupply.NETWORK_ID}" to BigDecimal("5")),
        ),
        NoEligibleModel(
            statuses = listOf(
                MockYieldSupply.loadedStatus(
                    MockYieldSupply.token(contractAddress = "0xA0", network = MockYieldSupply.network(
                        derivationPath = Network.DerivationPath.Joint("m/44'/60'/0'/0/0"),
                    )),
                ),
            ),
            yieldApys = apyOf(usdt),
        ),
        NoEligibleModel(
            statuses = listOf(MockYieldSupply.loadedStatus(usdt)),
            yieldApys = mapOf(usdt.yieldSupplyKey() to BigDecimal.ZERO),
        ),
        NoEligibleModel(
            statuses = listOf(MockYieldSupply.loadedStatus(usdt, amount = BigDecimal.ZERO)),
            yieldApys = apyOf(usdt),
        ),
        NoEligibleModel(
            statuses = listOf(
                MockYieldSupply.loadedStatus(usdt, amount = BigDecimal("100")),
                CryptoCurrencyStatus(
                    MockYieldSupply.token(contractAddress = "0xA0", network = cardNetwork),
                    CryptoCurrencyStatus.Unreachable(priceChange = null, fiatRate = null, networkAddress = null),
                ),
            ),
            yieldApys = apyOf(usdt),
        ),
        NoEligibleModel(
            statuses = listOf(
                MockYieldSupply.loadedStatus(usdt, amount = BigDecimal("100"), networkSource = StatusSource.ONLY_CACHE),
            ),
            yieldApys = apyOf(usdt),
        ),
        NoEligibleModel(
            statuses = listOf(MockYieldSupply.loadedStatus(usdt)),
            yieldApys = apyOf(usdt),
            earnOffers = EarnYieldOffers(total = 0, best = null),
        ),
    )

    private fun provideWaitingModels() = listOf(
        WaitingModel(status = CryptoCurrencyStatus(usdt, CryptoCurrencyStatus.Loading)),
        WaitingModel(status = MockYieldSupply.loadedStatus(usdt, networkSource = StatusSource.CACHE)),
    )

    private fun provideDataNotReadyModels() = listOf(
        null,
        MockYieldSupply.earnToken(apy = "0"),
        MockYieldSupply.earnToken(apy = "n/a"),
    )

    private fun provideVariantModels() = listOf(
        VariantModel(variant = YieldModePromoVariant.USER_MAX_APY, expected = userOffer),
        VariantModel(variant = YieldModePromoVariant.GLOBAL_MAX_APY, expected = globalOffer),
        VariantModel(variant = YieldModePromoVariant.CONTROL, expected = null),
    )

    private fun apyOf(token: CryptoCurrency.Token) = mapOf(token.yieldSupplyKey() to BigDecimal("5.00"))

    private fun earnOffers() = EarnYieldOffers(total = 36, best = MockYieldSupply.earnToken())

    private fun YieldModePromoResolution.userMaxApy() = (this as? YieldModePromoResolution.Ready)?.userMaxApy

    internal data class TieBreakModel(val statuses: List<CryptoCurrencyStatus>, val expectedSymbol: String)

    internal data class NoEligibleModel(
        val statuses: List<CryptoCurrencyStatus>,
        val yieldApys: Map<String, BigDecimal>,
        val earnOffers: EarnYieldOffers = EarnYieldOffers(total = 36, best = MockYieldSupply.earnToken()),
    )

    internal data class WaitingModel(val status: CryptoCurrencyStatus)

    internal data class VariantModel(val variant: YieldModePromoVariant, val expected: YieldModePromoOffer?)

    private companion object {
        val cardNetwork = MockYieldSupply.network(derivationPath = Network.DerivationPath.Card("m/44'/60'/1'/0/0"))
        val arbitrumNetwork = MockYieldSupply.network(id = "arbitrum-one", name = "Arbitrum One")

        val userOffer = YieldModePromoOffer(
            apy = BigDecimal("6.12"),
            tokensCount = 36,
            tokenSymbol = "USDC",
            blockchain = "Ethereum",
        )
        val globalOffer = YieldModePromoOffer(
            apy = BigDecimal("9.2"),
            tokensCount = 36,
            tokenSymbol = "USDT",
            blockchain = "Ethereum",
        )
    }
}