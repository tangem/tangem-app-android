package com.tangem.domain.yield.supply.usecase

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.yield.supply.YieldSupplyTransactionRepository
import com.tangem.domain.yield.supply.incentives.YieldIncentivesRepository
import com.tangem.domain.yield.supply.models.YieldSupplyClaimOutcome
import com.tangem.domain.yield.supply.models.YieldSupplyTransactionOutcome
import com.tangem.domain.yield.supply.models.incentives.YieldIncentivesError
import com.tangem.domain.yield.supply.models.incentives.YieldRewardsSummary
import com.tangem.test.core.ProvideTestModels
import com.tangem.test.core.TestAppCoroutineScope
import com.tangem.test.mock.yield.MockYieldSupply
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.math.BigInteger

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class YieldSupplyClaimTrackerTest {

    private val transactionRepository: YieldSupplyTransactionRepository = mockk()
    private val incentivesRepository: YieldIncentivesRepository = mockk(relaxUnitFun = true)
    private val getRewardsSummaryUseCase: GetYieldRewardsSummaryUseCase = mockk()

    @BeforeEach
    fun resetMocks() {
        clearMocks(transactionRepository, incentivesRepository, getRewardsSummaryUseCase)
        coEvery {
            getRewardsSummaryUseCase(chainId = CHAIN_ID, userAddress = USER_ADDRESS)
        } returns YieldRewardsSummary.None.right()
    }

    @Test
    fun `GIVEN receipt confirms WHEN Merkl has not reflected it yet THEN confirmed is emitted while still in flight`() =
        runTest {
            // Arrange
            receiptReturns(YieldSupplyTransactionOutcome.Confirmed)
            rewardsReturn(MockYieldSupply.claimableSummary(claimable = listOf(CLAIMED_REWARD)).right())
            val tracker = createTracker(testScope = this)
            val outcomes = collectOutcomes(tracker)

            // Act
            tracker.track()
            advanceTimeBy(delayTimeMillis = 10_001)
            runCurrent()

            // Assert
            assertThat(outcomes).containsExactly(outcome(YieldSupplyTransactionOutcome.Confirmed))
            assertThat(tracker.isInFlight()).isTrue()
        }

    @Test
    fun `GIVEN receipt reverts WHEN the outcome arrives THEN the claim is already released`() = runTest {
        // Arrange
        receiptReturns(YieldSupplyTransactionOutcome.Reverted)
        val tracker = createTracker(testScope = this)
        val inFlightOnOutcome = mutableListOf<Boolean>()
        backgroundScope.launch { tracker.outcomes.collect { inFlightOnOutcome += tracker.isInFlight() } }
        runCurrent()

        // Act
        tracker.track()
        advanceUntilIdle()
        runCurrent()

        // Assert
        assertThat(inFlightOnOutcome).containsExactly(false)
    }

    @Test
    fun `GIVEN receipt confirms WHEN rewards are read THEN the user cache is invalidated before each read`() =
        runTest {
            // Arrange
            receiptReturns(YieldSupplyTransactionOutcome.Confirmed)
            coEvery { getRewardsSummaryUseCase(chainId = CHAIN_ID, userAddress = USER_ADDRESS) } returnsMany listOf(
                MockYieldSupply.claimableSummary(claimable = listOf(CLAIMED_REWARD)).right(),
                YieldRewardsSummary.None.right(),
            )
            val tracker = createTracker(testScope = this)

            // Act
            tracker.track()
            advanceUntilIdle()

            // Assert
            coVerifyOrder {
                incentivesRepository.invalidateUserCache(chainId = CHAIN_ID, userAddress = USER_ADDRESS)
                getRewardsSummaryUseCase(chainId = CHAIN_ID, userAddress = USER_ADDRESS)
                incentivesRepository.invalidateUserCache(chainId = CHAIN_ID, userAddress = USER_ADDRESS)
                getRewardsSummaryUseCase(chainId = CHAIN_ID, userAddress = USER_ADDRESS)
            }
        }

    @Test
    fun `GIVEN receipt reverts WHEN claim tracked THEN reverted outcome emitted and rewards are not read`() = runTest {
        // Arrange
        receiptReturns(YieldSupplyTransactionOutcome.Reverted)
        val tracker = createTracker(testScope = this)
        val outcomes = collectOutcomes(tracker)

        // Act
        tracker.track()
        advanceUntilIdle()
        runCurrent()

        // Assert
        assertThat(outcomes).containsExactly(outcome(YieldSupplyTransactionOutcome.Reverted))
        assertThat(tracker.isInFlight()).isFalse()
        coVerify(exactly = 0) { incentivesRepository.invalidateUserCache(any(), any()) }
        coVerify(exactly = 0) { getRewardsSummaryUseCase(chainId = any(), userAddress = any()) }
    }

    @Test
    fun `GIVEN a confirmed claim WHEN Merkl still offers the claimed rewards THEN the claim stays in flight`() =
        runTest {
            // Arrange
            receiptReturns(YieldSupplyTransactionOutcome.Confirmed)
            rewardsReturn(MockYieldSupply.claimableSummary(claimable = listOf(CLAIMED_REWARD)).right())
            val tracker = createTracker(testScope = this)

            // Act
            tracker.track()
            advanceTimeBy(delayTimeMillis = 60_000)

            // Assert
            assertThat(tracker.isInFlight()).isTrue()
        }

    @ParameterizedTest
    @MethodSource("provideReflectionModels")
    fun `GIVEN a confirmed claim WHEN Merkl reports the rewards THEN the claim is released only once reflected`(
        model: ReflectionModel,
    ) = runTest {
        // Arrange
        receiptReturns(YieldSupplyTransactionOutcome.Confirmed)
        rewardsReturn(model.summary)
        val tracker = createTracker(testScope = this)

        // Act
        tracker.track()
        advanceTimeBy(delayTimeMillis = 30_000)

        // Assert
        assertThat(tracker.isInFlight()).isEqualTo(!model.isReflected)
    }

    @Test
    fun `GIVEN a confirmed claim WHEN Merkl never reflects it THEN the claim is released after six minutes`() =
        runTest {
            // Arrange
            receiptReturns(YieldSupplyTransactionOutcome.Confirmed)
            rewardsReturn(MockYieldSupply.claimableSummary(claimable = listOf(CLAIMED_REWARD)).right())
            val tracker = createTracker(testScope = this)

            // Act
            tracker.track()
            advanceTimeBy(delayTimeMillis = 359_000)
            val isInFlightBeforeTimeout = tracker.isInFlight()
            advanceTimeBy(delayTimeMillis = 2_000)

            // Assert
            assertThat(isInFlightBeforeTimeout).isTrue()
            assertThat(tracker.isInFlight()).isFalse()
        }

    @Test
    fun `GIVEN a confirmed claim WHEN Merkl reflects it on a later read THEN the claim is released then`() = runTest {
        // Arrange
        receiptReturns(YieldSupplyTransactionOutcome.Confirmed)
        coEvery { getRewardsSummaryUseCase(chainId = CHAIN_ID, userAddress = USER_ADDRESS) } returnsMany listOf(
            MockYieldSupply.claimableSummary(claimable = listOf(CLAIMED_REWARD)).right(),
            MockYieldSupply.claimableSummary(claimable = listOf(CLAIMED_REWARD)).right(),
            YieldRewardsSummary.None.right(),
        )
        val tracker = createTracker(testScope = this)

        // Act
        tracker.track()
        advanceTimeBy(delayTimeMillis = 30_000)
        val isInFlightAfterSecondRead = tracker.isInFlight()
        advanceTimeBy(delayTimeMillis = 15_000)

        // Assert
        assertThat(isInFlightAfterSecondRead).isTrue()
        assertThat(tracker.isInFlight()).isFalse()
        coVerify(exactly = 3) { getRewardsSummaryUseCase(chainId = CHAIN_ID, userAddress = USER_ADDRESS) }
    }

    @Test
    fun `GIVEN transaction not mined WHEN Merkl never reflects it THEN no outcome and claim no longer in flight`() =
        runTest {
            // Arrange
            receiptReturns(null)
            rewardsReturn(MockYieldSupply.claimableSummary(claimable = listOf(CLAIMED_REWARD)).right())
            val tracker = createTracker(testScope = this)
            val outcomes = collectOutcomes(tracker)

            // Act
            tracker.track()
            advanceUntilIdle()

            // Assert
            assertThat(outcomes).isEmpty()
            assertThat(tracker.isInFlight()).isFalse()
        }

    @Test
    fun `GIVEN transaction not mined WHEN the receipt attempts run out THEN Merkl is asked whether it was reflected`() =
        runTest {
            // Arrange
            receiptReturns(null)
            val tracker = createTracker(testScope = this)

            // Act
            tracker.track()
            advanceTimeBy(delayTimeMillis = 301_000)

            // Assert
            assertThat(tracker.isInFlight()).isFalse()
            coVerify(exactly = 1) { getRewardsSummaryUseCase(chainId = CHAIN_ID, userAddress = USER_ADDRESS) }
        }

    @Test
    fun `GIVEN claim submitted WHEN receipt is still pending THEN claim reads as in flight`() = runTest {
        // Arrange
        receiptReturns(null)
        val tracker = createTracker(testScope = this)

        // Act
        tracker.track()
        advanceTimeBy(delayTimeMillis = 15_000)

        // Assert
        assertThat(tracker.isInFlight()).isTrue()
    }

    @Test
    fun `GIVEN claim already tracked WHEN tracked again THEN the receipt is polled by one watcher only`() = runTest {
        // Arrange
        receiptReturns(YieldSupplyTransactionOutcome.Confirmed)
        val tracker = createTracker(testScope = this)

        // Act
        tracker.track()
        tracker.track()
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 1) { transactionRepository.getTransactionOutcome(USER_WALLET_ID, TOKEN, TX_HASH) }
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN a claim in flight WHEN a module is asked about THEN only that module reads as in flight`(
        model: InFlightModel,
    ) = runTest {
        // Arrange
        receiptReturns(null)
        val tracker = createTracker(testScope = this)
        tracker.track()

        // Act
        val actual = tracker.isClaimInFlight(model.chainId, model.userAddress).first()

        // Assert
        assertThat(actual).isEqualTo(model.isInFlight)
    }

    @Test
    fun `GIVEN a claim of one token WHEN the same module is claimed from another token THEN it is not followed twice`() =
        runTest {
            // Arrange
            receiptReturns(YieldSupplyTransactionOutcome.Confirmed)
            coEvery {
                transactionRepository.getTransactionOutcome(USER_WALLET_ID, OTHER_TOKEN, OTHER_TX_HASH)
            } returns YieldSupplyTransactionOutcome.Confirmed
            val tracker = createTracker(testScope = this)

            // Act
            tracker.track()
            tracker.track(
                userWalletId = USER_WALLET_ID,
                cryptoCurrency = OTHER_TOKEN,
                chainId = CHAIN_ID,
                userAddress = USER_ADDRESS,
                transactionHash = OTHER_TX_HASH,
                claimedRewards = listOf(CLAIMED_REWARD),
            )
            advanceUntilIdle()

            // Assert
            coVerify(exactly = 0) {
                transactionRepository.getTransactionOutcome(USER_WALLET_ID, OTHER_TOKEN, OTHER_TX_HASH)
            }
        }

    private fun provideTestModels() = listOf(
        InFlightModel(chainId = CHAIN_ID, userAddress = USER_ADDRESS, isInFlight = true),
        InFlightModel(chainId = CHAIN_ID, userAddress = USER_ADDRESS.uppercase(), isInFlight = true),
        InFlightModel(chainId = OTHER_CHAIN_ID, userAddress = USER_ADDRESS, isInFlight = false),
        InFlightModel(chainId = CHAIN_ID, userAddress = OTHER_USER_ADDRESS, isInFlight = false),
    )

    private fun provideReflectionModels() = listOf(
        ReflectionModel(
            summary = MockYieldSupply.claimableSummary(claimable = listOf(CLAIMED_REWARD)).right(),
            isReflected = false,
        ),
        ReflectionModel(
            summary = MockYieldSupply.claimableSummary(
                claimable = listOf(CLAIMED_REWARD.copy(claimableAmount = BigInteger.ONE)),
            ).right(),
            isReflected = true,
        ),
        ReflectionModel(
            summary = MockYieldSupply.claimableSummary(
                claimable = listOf(CLAIMED_REWARD.copy(rewardToken = CLAIMED_REWARD.rewardToken.uppercase())),
            ).right(),
            isReflected = false,
        ),
        ReflectionModel(
            summary = MockYieldSupply.claimableSummary(claimable = listOf(OTHER_REWARD)).right(),
            isReflected = true,
        ),
        ReflectionModel(
            summary = MockYieldSupply.deferredSummary(claimable = listOf(CLAIMED_REWARD)).right(),
            isReflected = false,
        ),
        ReflectionModel(
            summary = MockYieldSupply.deferredSummary(
                claimable = listOf(CLAIMED_REWARD.copy(claimableAmount = BigInteger.ONE)),
            ).right(),
            isReflected = true,
        ),
        ReflectionModel(
            summary = YieldRewardsSummary.PendingOnly(pending = listOf(CLAIMED_REWARD)).right(),
            isReflected = true,
        ),
        ReflectionModel(summary = YieldRewardsSummary.None.right(), isReflected = true),
        ReflectionModel(summary = YieldIncentivesError.Unavailable.left(), isReflected = false),
    )

    private fun createTracker(testScope: TestScope) = YieldSupplyClaimTracker(
        yieldSupplyTransactionRepository = transactionRepository,
        yieldIncentivesRepository = incentivesRepository,
        getYieldRewardsSummaryUseCase = getRewardsSummaryUseCase,
        coroutineScope = TestAppCoroutineScope(testScope),
    )

    private fun receiptReturns(outcome: YieldSupplyTransactionOutcome?) {
        coEvery { transactionRepository.getTransactionOutcome(USER_WALLET_ID, TOKEN, TX_HASH) } returns outcome
    }

    private fun rewardsReturn(summary: Either<YieldIncentivesError, YieldRewardsSummary>) {
        coEvery { getRewardsSummaryUseCase(chainId = CHAIN_ID, userAddress = USER_ADDRESS) } returns summary
    }

    private fun YieldSupplyClaimTracker.track() = track(
        userWalletId = USER_WALLET_ID,
        cryptoCurrency = TOKEN,
        chainId = CHAIN_ID,
        userAddress = USER_ADDRESS,
        transactionHash = TX_HASH,
        claimedRewards = listOf(CLAIMED_REWARD),
    )

    private suspend fun YieldSupplyClaimTracker.isInFlight() = isClaimInFlight(CHAIN_ID, USER_ADDRESS).first()

    private fun outcome(outcome: YieldSupplyTransactionOutcome) = YieldSupplyClaimOutcome(
        userWalletId = USER_WALLET_ID,
        cryptoCurrencyId = TOKEN.id,
        chainId = CHAIN_ID,
        userAddress = USER_ADDRESS,
        outcome = outcome,
    )

    /** Subscribes before the claim is tracked; call sites drain the collector with an extra `runCurrent`. */
    private fun TestScope.collectOutcomes(tracker: YieldSupplyClaimTracker): List<YieldSupplyClaimOutcome> {
        val collected = mutableListOf<YieldSupplyClaimOutcome>()
        backgroundScope.launch { tracker.outcomes.collect(collected::add) }
        runCurrent()

        return collected
    }

    data class InFlightModel(val chainId: Int, val userAddress: String, val isInFlight: Boolean)

    data class ReflectionModel(
        val summary: Either<YieldIncentivesError, YieldRewardsSummary>,
        val isReflected: Boolean,
    )

    private companion object {
        val USER_WALLET_ID = UserWalletId("00")
        const val CHAIN_ID = 1
        const val USER_ADDRESS = "0xowner"
        const val TX_HASH = "0xclaim"
        const val OTHER_CHAIN_ID = 137
        const val OTHER_USER_ADDRESS = "0xotherowner"
        const val OTHER_TX_HASH = "0xotherclaim"

        val TOKEN = createToken(contractAddress = "0xusdt", symbol = "USDT")
        val OTHER_TOKEN = createToken(contractAddress = "0xusdc", symbol = "USDC")
        val CLAIMED_REWARD = MockYieldSupply.reward(claimableAmount = BigInteger("1000"))
        val OTHER_REWARD = MockYieldSupply.reward(rewardToken = "0xother", claimableAmount = BigInteger("5000"))

        private fun createToken(contractAddress: String, symbol: String) = MockYieldSupply.token(
            contractAddress = contractAddress,
            name = symbol,
            symbol = symbol,
        )
    }
}