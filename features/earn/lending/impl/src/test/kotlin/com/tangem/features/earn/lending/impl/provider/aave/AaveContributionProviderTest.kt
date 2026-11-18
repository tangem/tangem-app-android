package com.tangem.features.earn.lending.impl.provider.aave

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.common.test.domain.wallet.MockUserWalletFactory
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.balance.BalanceContribution
import com.tangem.domain.models.currency.balance.ContributionResolver
import com.tangem.domain.yield.supply.YieldSupplyRepository
import com.tangem.domain.yield.supply.models.YieldMarketToken
import com.tangem.domain.yield.supply.models.incentives.YieldIncentiveCampaign
import com.tangem.domain.yield.supply.models.status.YieldModuleStatus
import com.tangem.domain.yield.supply.status.YieldModuleStatusProducer
import com.tangem.domain.yield.supply.status.YieldModuleStatusSupplier
import com.tangem.features.earn.api.EarnFeatureToggles
import com.tangem.features.earn.lending.impl.contribution.LendingContribution
import com.tangem.features.earn.lending.impl.provider.LendingAsset
import com.tangem.features.earn.lending.impl.provider.LendingProvider
import com.tangem.test.core.ProvideTestModels
import com.tangem.test.mock.yield.MockYieldSupply
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class AaveContributionProviderTest {

    private val userWallet = MockUserWalletFactory.create()
    private val statusParams = YieldModuleStatusProducer.Params(userWallet.walletId)
    private val currencies = MockCryptoCurrencyFactory()
    private val eth = currencies.createCoin(Blockchain.Ethereum)
    private val weth = currencies.createToken(
        blockchain = Blockchain.Ethereum,
        contractAddress = WETH,
        symbol = "WETH",
        decimals = 18,
    )
    private val asset = LendingAsset(currency = eth, wrappedToken = weth)

    private val statusSupplier: YieldModuleStatusSupplier = mockk()
    private val assetResolver: AaveAssetResolver = mockk()
    private val toggles: EarnFeatureToggles = mockk()
    private val yieldSupplyRepository: YieldSupplyRepository = mockk()
    private val lendingProvider: LendingProvider = mockk()

    private val provider = AaveContributionProvider(
        statusSupplier = statusSupplier,
        assetResolver = assetResolver,
        markets = AaveMarkets(yieldSupplyRepository),
        lendingProvider = lendingProvider,
        earnFeatureToggles = toggles,
    )

    @BeforeEach
    fun setUp() {
        clearMocks(statusSupplier, assetResolver, toggles, yieldSupplyRepository, lendingProvider)
        every { toggles.isEarnEnabled } returns true
        every { yieldSupplyRepository.getMarketsFlow() } returns flowOf(emptyList())
        every { lendingProvider.performanceFee } returns BigDecimal("0.15")
        every { assetResolver.resolve(any()) } returns null
        every { assetResolver.resolve(eth) } returns asset
    }

    @Test
    fun `GIVEN earn disabled WHEN contributions THEN single empty frame`() = runTest {
        // Arrange
        every { toggles.isEarnEnabled } returns false

        // Act
        val frames = provider.contributions(userWallet)

        // Assert
        frames.test {
            assertThat(awaitItem()).isEqualTo(ContributionResolver.Empty)
            awaitComplete()
        }
        verify(exactly = 0) { statusSupplier(any<YieldModuleStatusProducer.Params>()) }
    }

    @Test
    fun `GIVEN single-currency wallet WHEN contributions THEN single empty frame`() = runTest {
        // Act
        val frames = provider.contributions(userWallet.copy(isMultiCurrency = false))

        // Assert
        frames.test {
            assertThat(awaitItem()).isEqualTo(ContributionResolver.Empty)
            awaitComplete()
        }
    }

    @Test
    fun `GIVEN statuses not emitted yet WHEN contributions THEN first frame resolves nothing`() = runTest {
        // Arrange
        every { statusSupplier(statusParams) } returns MutableSharedFlow()

        // Act
        val frames = provider.contributions(userWallet)

        // Assert
        frames.test {
            assertThat(awaitItem().resolve(eth, null)).isNull()
        }
    }

    @ParameterizedTest
    @ProvideTestModels
    fun resolve(model: ResolveModel) = runTest {
        // Arrange
        every { statusSupplier(statusParams) } returns flowOf(model.statuses)

        // Act
        val frames = provider.contributions(userWallet)

        // Assert
        frames.test {
            skipItems(1)
            assertThat(awaitItem().resolve(model.currency, null)).isEqualTo(model.expected)
            awaitComplete()
        }
    }

    private fun provideTestModels() = listOf(
        ResolveModel(
            currency = eth,
            statuses = setOf(wethStatus(balance = "1.5", source = StatusSource.CACHE)),
            expected = LendingContribution(amount = BigDecimal("1.5"), source = StatusSource.CACHE),
        ),
        ResolveModel(currency = eth, statuses = setOf(wethStatus(balance = "0")), expected = null),
        ResolveModel(currency = eth, statuses = setOf(wethStatus(balance = null)), expected = null),
        ResolveModel(currency = eth, statuses = emptySet(), expected = null),
        // The wrapped token itself gets nothing: the reserve in the module belongs to the coin's Lending
        ResolveModel(currency = weth, statuses = setOf(wethStatus(balance = "1.5")), expected = null),
    )

    @Test
    fun `GIVEN WETH market WHEN contributions THEN position accrues at the base rate net of the fee`() = runTest {
        // Arrange
        every { statusSupplier(statusParams) } returns flowOf(setOf(wethStatus(balance = "1.5")))
        every { yieldSupplyRepository.getMarketsFlow() } returns flowOf(listOf(wethMarket(apy = "3", totalApyEst = "5")))

        // Act
        val frames = provider.contributions(userWallet)

        // Assert
        frames.test {
            skipItems(1)
            assertThat(awaitItem().resolve(eth, null)).isEqualTo(
                LendingContribution(
                    amount = BigDecimal("1.5"),
                    source = StatusSource.ACTUAL,
                    accrualRate = BigDecimal("0.0255"),
                ),
            )
            awaitComplete()
        }
    }

    @Test
    fun `GIVEN market rate changes WHEN contributions THEN new frame carries the new rate`() = runTest {
        // Arrange
        val markets = MutableStateFlow(listOf(wethMarket(apy = "3", totalApyEst = "3")))
        every { statusSupplier(statusParams) } returns flowOf(setOf(wethStatus(balance = "1.5")))
        every { yieldSupplyRepository.getMarketsFlow() } returns markets

        // Act
        val frames = provider.contributions(userWallet)

        // Assert
        frames.test {
            skipItems(2)
            markets.value = listOf(wethMarket(apy = "4", totalApyEst = "4"))
            assertThat(awaitItem().resolve(eth, null)).isEqualTo(
                LendingContribution(
                    amount = BigDecimal("1.5"),
                    source = StatusSource.ACTUAL,
                    accrualRate = BigDecimal("0.0340"),
                ),
            )
        }
    }

    @Test
    fun `GIVEN markets read fails WHEN contributions THEN position stays without a rate`() = runTest {
        // Arrange
        every { statusSupplier(statusParams) } returns flowOf(setOf(wethStatus(balance = "1.5")))
        every { yieldSupplyRepository.getMarketsFlow() } returns flow { throw IllegalStateException("read failed") }

        // Act
        val frames = provider.contributions(userWallet)

        // Assert
        frames.test {
            skipItems(1)
            assertThat(awaitItem().resolve(eth, null)).isEqualTo(
                LendingContribution(amount = BigDecimal("1.5"), source = StatusSource.ACTUAL),
            )
            awaitComplete()
        }
    }

    @Test
    fun `GIVEN lending contribution WHEN deltas read THEN total is the position and spendable is zero`() {
        // Arrange
        val contribution = LendingContribution(amount = BigDecimal("1.5"), source = StatusSource.ACTUAL)

        // Act
        val deltas = contribution.totalDeltaCryptoAmount() to contribution.spendableDeltaCryptoAmount()

        // Assert
        assertThat(deltas).isEqualTo(BigDecimal("1.5") to BigDecimal.ZERO)
    }

    private fun wethStatus(balance: String?, source: StatusSource = StatusSource.ACTUAL): YieldModuleStatus =
        MockYieldSupply.moduleStatus(
            networkId = eth.network.id,
            tokenAddress = WETH.lowercase(),
            effectiveProtocolBalance = balance?.let(::BigDecimal),
            source = source,
        )

    private fun wethMarket(apy: String, totalApyEst: String): YieldMarketToken = YieldMarketToken(
        tokenAddress = WETH,
        chainId = 1,
        apy = BigDecimal(apy),
        isActive = false,
        maxFeeNative = BigDecimal.ZERO,
        maxFeeUSD = BigDecimal.ZERO,
        backendId = eth.network.rawId,
        campaign = YieldIncentiveCampaign(
            hasActiveCampaign = true,
            rewardsApy = BigDecimal(totalApyEst) - BigDecimal(apy),
            totalApyEst = BigDecimal(totalApyEst),
        ),
    )

    internal data class ResolveModel(
        val currency: CryptoCurrency,
        val statuses: Set<YieldModuleStatus>,
        val expected: BalanceContribution?,
    )

    private companion object {
        const val WETH = "0xC02aaA39b223FE8D0A0e5C4F27eAD9083C756Cc2"
    }
}