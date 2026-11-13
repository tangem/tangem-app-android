package com.tangem.features.earn.lending.impl.provider.aave

import app.cash.turbine.test
import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.yieldsupply.providers.YieldModuleVersionStatus
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.network.NetworkStatus
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.networks.single.SingleNetworkStatusProducer
import com.tangem.domain.networks.single.SingleNetworkStatusSupplier
import com.tangem.domain.yield.supply.YieldSupplyRepository
import com.tangem.domain.yield.supply.YieldSupplyTransactionRepository
import com.tangem.domain.yield.supply.models.YieldMarketToken
import com.tangem.domain.yield.supply.models.incentives.YieldIncentiveCampaign
import com.tangem.domain.yield.supply.models.status.YieldModuleStatus
import com.tangem.domain.yield.supply.status.YieldModuleStatusFetcher
import com.tangem.domain.yield.supply.status.YieldModuleStatusProducer
import com.tangem.domain.yield.supply.status.YieldModuleStatusSupplier
import com.tangem.features.earn.api.overview.EarnProductRate
import com.tangem.features.earn.api.overview.EarnRateType
import com.tangem.features.earn.lending.impl.provider.LendingAsset
import com.tangem.features.earn.lending.impl.provider.LendingDepositGate
import com.tangem.features.earn.lending.impl.provider.LendingMarket
import com.tangem.features.earn.lending.impl.provider.LendingPosition
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class AaveLendingProviderTest {

    private val userWalletId = UserWalletId("011")
    private val currencies = MockCryptoCurrencyFactory()
    private val eth = currencies.createCoin(Blockchain.Ethereum)
    private val weth = currencies.createToken(
        blockchain = Blockchain.Ethereum,
        contractAddress = WETH,
        symbol = "WETH",
        decimals = 18,
    )
    private val asset = LendingAsset(currency = eth, wrappedToken = weth)

    private val yieldTransactions: YieldSupplyTransactionRepository = mockk()
    private val yieldSupplyRepository: YieldSupplyRepository = mockk()
    private val statusSupplier: YieldModuleStatusSupplier = mockk()
    private val statusFetcher: YieldModuleStatusFetcher = mockk()
    private val networkStatusSupplier: SingleNetworkStatusSupplier = mockk()

    private val markets = MutableStateFlow(listOf(wethMarket()))
    private val statuses = MutableStateFlow<Set<YieldModuleStatus>>(emptySet())
    private val networkUpdates = MutableSharedFlow<NetworkStatus>(replay = 1)

    private val provider = AaveLendingProvider(
        assetResolver = mockk(),
        markets = AaveMarkets(yieldSupplyRepository),
        moduleReader = AaveModuleReader(yieldTransactions),
        minDepositProvider = mockk(),
        apyHistoryLoader = mockk(),
        yieldSupplyRepository = yieldSupplyRepository,
        networkStatusSupplier = networkStatusSupplier,
        statusSupplier = statusSupplier,
        statusFetcher = statusFetcher,
    )

    @BeforeEach
    fun setUp() {
        clearMocks(yieldTransactions, yieldSupplyRepository, statusSupplier, statusFetcher, networkStatusSupplier)
        markets.value = listOf(wethMarket())
        statuses.value = emptySet()
        networkUpdates.resetReplayCache()
        networkUpdates.tryEmit(mockk(relaxed = true))

        every { yieldSupplyRepository.getMarketsFlow() } returns markets
        coEvery { yieldSupplyRepository.isYieldSupplySupported(userWalletId, eth) } returns true
        coEvery { yieldSupplyRepository.updateMarkets() } returns listOf(wethMarket())
        every { statusSupplier(YieldModuleStatusProducer.Params(userWalletId)) } returns statuses
        coEvery { statusFetcher(any()) } returns Unit.right()
        every {
            networkStatusSupplier(SingleNetworkStatusProducer.Params(userWalletId, eth.network))
        } returns networkUpdates
        coEvery { yieldTransactions.getYieldModuleVersionStatus(userWalletId, eth.network) } returns
            YieldModuleVersionStatus.UpToDate
        coEvery { yieldTransactions.getWithdrawLiquidity(userWalletId, weth) } returns BigDecimal("10")
    }

    @Test
    fun `GIVEN active Merkl campaign WHEN market THEN rate is total APY estimate`() = runTest {
        // Arrange
        markets.value = listOf(wethMarket(campaign = campaign(isActive = true, totalApyEst = "5.5")))

        // Act & Assert
        provider.market(userWalletId, asset).test {
            assertThat(expectMostRecentItem().rate).isEqualTo(apy("0.055"))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `GIVEN inactive Merkl campaign WHEN market THEN rate is base APY`() = runTest {
        // Arrange
        markets.value = listOf(wethMarket(campaign = campaign(isActive = false, totalApyEst = "5.5")))

        // Act & Assert
        provider.market(userWalletId, asset).test {
            assertThat(expectMostRecentItem().rate).isEqualTo(apy("0.03"))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `GIVEN inactive market WHEN market THEN closed with rate kept`() = runTest {
        // Arrange
        markets.value = listOf(wethMarket(isActive = false))

        // Act & Assert
        provider.market(userWalletId, asset).test {
            val actual = expectMostRecentItem()
            assertThat(actual.isOpen).isFalse()
            assertThat(actual.rate).isEqualTo(apy("0.03"))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `GIVEN up to date module and no WETH status WHEN market THEN deposit loading with cache source`() = runTest {
        // Act & Assert
        provider.market(userWalletId, asset).test {
            val actual = expectMostRecentItem()
            assertThat(actual.deposit).isEqualTo(LendingDepositGate.Loading)
            assertThat(actual.position).isNull()
            assertThat(actual.source).isEqualTo(StatusSource.CACHE)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `GIVEN WETH status with balance WHEN market THEN position from module with actual source`() = runTest {
        // Arrange
        statuses.value = setOf(wethStatus(balance = "1.5"))

        // Act & Assert
        provider.market(userWalletId, asset).test {
            val actual = expectMostRecentItem()
            val expected = LendingMarket(
                rate = apy("0.03"),
                isOpen = true,
                deposit = LendingDepositGate.Open,
                position = LendingPosition(BigDecimal("1.5")),
                maxWithdraw = BigDecimal("1.5"),
                providerState = AaveModuleRead(
                    status = wethStatus(balance = "1.5"),
                    version = YieldModuleVersionStatus.UpToDate,
                    withdrawLiquidity = BigDecimal("10"),
                ),
                source = StatusSource.ACTUAL,
            )
            assertThat(actual).isEqualTo(expected)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `GIVEN zero balance in module WHEN market THEN no position`() = runTest {
        // Arrange
        statuses.value = setOf(wethStatus(balance = "0"))

        // Act & Assert
        provider.market(userWalletId, asset).test {
            val actual = expectMostRecentItem()
            assertThat(actual.position).isNull()
            assertThat(actual.maxWithdraw).isNull()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `GIVEN liquidity below position WHEN market THEN withdrawal capped by liquidity`() = runTest {
        // Arrange
        statuses.value = setOf(wethStatus(balance = "1.5"))
        coEvery { yieldTransactions.getWithdrawLiquidity(userWalletId, weth) } returns BigDecimal("1")

        // Act & Assert
        provider.market(userWalletId, asset).test {
            assertThat(expectMostRecentItem().maxWithdraw).isEqualTo(BigDecimal("1"))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `GIVEN liquidity read fails WHEN market THEN withdrawal capped by position only`() = runTest {
        // Arrange
        statuses.value = setOf(wethStatus(balance = "1.5"))
        coEvery { yieldTransactions.getWithdrawLiquidity(userWalletId, weth) } throws IllegalStateException("rpc")

        // Act & Assert
        provider.market(userWalletId, asset).test {
            assertThat(expectMostRecentItem().maxWithdraw).isEqualTo(BigDecimal("1.5"))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `GIVEN first version read fails WHEN market THEN deposit read failed`() = runTest {
        // Arrange
        statuses.value = setOf(wethStatus(balance = "1.5"))
        coEvery { yieldTransactions.getYieldModuleVersionStatus(userWalletId, eth.network) } throws
            IllegalStateException("rpc")

        // Act & Assert
        provider.market(userWalletId, asset).test {
            val actual = expectMostRecentItem()
            assertThat(actual.deposit).isEqualTo(LendingDepositGate.ReadFailed)
            assertThat(actual.source).isEqualTo(StatusSource.ONLY_CACHE)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `GIVEN successful read then version read fails WHEN network updates THEN previous state kept`() = runTest {
        // Arrange
        statuses.value = setOf(wethStatus(balance = "1.5"))

        provider.market(userWalletId, asset).test {
            assertThat(expectMostRecentItem().deposit).isEqualTo(LendingDepositGate.Open)
            coEvery { yieldTransactions.getYieldModuleVersionStatus(userWalletId, eth.network) } throws
                IllegalStateException("rpc")

            // Act
            networkUpdates.emit(mockk(relaxed = true))

            // Assert
            val actual = expectMostRecentItem()
            assertThat(actual.deposit).isEqualTo(LendingDepositGate.Open)
            assertThat(actual.position).isEqualTo(LendingPosition(BigDecimal("1.5")))
            assertThat(actual.source).isEqualTo(StatusSource.ONLY_CACHE)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `GIVEN successful read then version indeterminate WHEN network updates THEN previous state kept`() = runTest {
        // Arrange
        statuses.value = setOf(wethStatus(balance = "1.5"))

        provider.market(userWalletId, asset).test {
            assertThat(expectMostRecentItem().deposit).isEqualTo(LendingDepositGate.Open)
            coEvery { yieldTransactions.getYieldModuleVersionStatus(userWalletId, eth.network) } returns
                YieldModuleVersionStatus.Indeterminate(reason = "rpc")

            // Act
            networkUpdates.emit(mockk(relaxed = true))

            // Assert
            val actual = expectMostRecentItem()
            assertThat(actual.deposit).isEqualTo(LendingDepositGate.Open)
            assertThat(actual.source).isEqualTo(StatusSource.ONLY_CACHE)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `GIVEN WETH status fetch fails and no status stored WHEN market THEN read failed instead of loading`() =
        runTest {
            // Arrange
            coEvery { statusFetcher(any()) } returns IllegalStateException("rpc").left()

            // Act & Assert
            provider.market(userWalletId, asset).test {
                val actual = expectMostRecentItem()
                assertThat(actual.deposit).isEqualTo(LendingDepositGate.ReadFailed)
                assertThat(actual.source).isEqualTo(StatusSource.ONLY_CACHE)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `GIVEN WETH status fetch fails but status stored WHEN market THEN stored status is used`() = runTest {
        // Arrange
        statuses.value = setOf(wethStatus(balance = "1.5"))
        coEvery { statusFetcher(any()) } returns IllegalStateException("rpc").left()

        // Act & Assert
        provider.market(userWalletId, asset).test {
            val actual = expectMostRecentItem()
            assertThat(actual.deposit).isEqualTo(LendingDepositGate.Open)
            assertThat(actual.position).isEqualTo(LendingPosition(BigDecimal("1.5")))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `GIVEN network update WHEN market THEN WETH status is requested again`() = runTest {
        provider.market(userWalletId, asset).test {
            expectMostRecentItem()

            // Act
            networkUpdates.emit(mockk(relaxed = true))
            expectMostRecentItem()

            // Assert
            coVerify(exactly = 2) { statusFetcher(YieldModuleStatusFetcher.Params(userWalletId, setOf(weth))) }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `GIVEN status fetch failed then succeeded WHEN network updates THEN failure is cleared`() = runTest {
        // Arrange
        coEvery { statusFetcher(any()) } returns IllegalStateException("rpc").left()

        provider.market(userWalletId, asset).test {
            assertThat(expectMostRecentItem().deposit).isEqualTo(LendingDepositGate.ReadFailed)
            coEvery { statusFetcher(any()) } returns Unit.right()

            // Act
            networkUpdates.emit(mockk(relaxed = true))

            // Assert
            val actual = expectMostRecentItem()
            assertThat(actual.deposit).isEqualTo(LendingDepositGate.Loading)
            assertThat(actual.source).isEqualTo(StatusSource.CACHE)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `GIVEN market subscribed WHEN started THEN WETH status is requested`() = runTest {
        // Act & Assert
        provider.market(userWalletId, asset).test {
            expectMostRecentItem()
            coVerify(exactly = 1) { statusFetcher(YieldModuleStatusFetcher.Params(userWalletId, setOf(weth))) }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `GIVEN markets and status refresh succeed WHEN refresh THEN Right and both refreshed`() = runTest {
        // Act
        val actual = provider.refresh(userWalletId, asset)

        // Assert
        assertThat(actual).isEqualTo(Unit.right())
        coVerify(exactly = 1) { yieldSupplyRepository.updateMarkets() }
        coVerify(exactly = 1) { statusFetcher(YieldModuleStatusFetcher.Params(userWalletId, setOf(weth))) }
    }

    @Test
    fun `GIVEN markets update throws WHEN refresh THEN Left`() = runTest {
        // Arrange
        coEvery { yieldSupplyRepository.updateMarkets() } throws IllegalStateException("backend")

        // Act
        val actual = provider.refresh(userWalletId, asset)

        // Assert
        assertThat(actual.isLeft()).isTrue()
    }

    @Test
    fun `GIVEN status refresh fails WHEN refresh THEN Left`() = runTest {
        // Arrange
        coEvery { statusFetcher(any()) } returns IllegalStateException("rpc").left()

        // Act
        val actual = provider.refresh(userWalletId, asset)

        // Assert
        assertThat(actual.isLeft()).isTrue()
    }

    @Test
    fun `GIVEN status refresh succeeds WHEN refreshPosition THEN Right without markets update`() = runTest {
        // Act
        val actual = provider.refreshPosition(userWalletId, asset)

        // Assert
        assertThat(actual).isEqualTo(Unit.right())
        coVerify(exactly = 0) { yieldSupplyRepository.updateMarkets() }
        coVerify(exactly = 1) { statusFetcher(YieldModuleStatusFetcher.Params(userWalletId, setOf(weth))) }
    }

    @Test
    fun `GIVEN status refresh fails WHEN refreshPosition THEN Left`() = runTest {
        // Arrange
        val error = IllegalStateException("rpc")
        coEvery { statusFetcher(any()) } returns error.left()

        // Act
        val actual = provider.refreshPosition(userWalletId, asset)

        // Assert
        assertThat(actual).isEqualTo(error.left())
    }

    private fun wethMarket(isActive: Boolean = true, campaign: YieldIncentiveCampaign? = null) = YieldMarketToken(
        tokenAddress = WETH,
        chainId = 1,
        apy = BigDecimal("3"),
        isActive = isActive,
        maxFeeNative = BigDecimal.ONE,
        maxFeeUSD = BigDecimal.ONE,
        backendId = eth.network.rawId,
        campaign = campaign,
    )

    private fun campaign(isActive: Boolean, totalApyEst: String) = YieldIncentiveCampaign(
        hasActiveCampaign = isActive,
        rewardsApy = BigDecimal("2.5"),
        totalApyEst = BigDecimal(totalApyEst),
    )

    private fun wethStatus(balance: String) = YieldModuleStatus(
        networkId = eth.network.id,
        tokenAddress = WETH.lowercase(),
        isActive = true,
        isInitialized = true,
        isAllowedToSpend = true,
        effectiveProtocolBalance = BigDecimal(balance),
        effectiveBalance = null,
        source = StatusSource.ACTUAL,
    )

    private fun apy(fraction: String) = EarnProductRate(fraction = BigDecimal(fraction), type = EarnRateType.APY)

    private companion object {
        const val WETH = "0xC02aaA39b223FE8D0A0e5C4F27eAD9083C756Cc2"
    }
}