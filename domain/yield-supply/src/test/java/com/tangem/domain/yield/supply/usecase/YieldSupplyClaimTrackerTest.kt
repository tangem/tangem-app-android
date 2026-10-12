package com.tangem.domain.yield.supply.usecase

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.yield.supply.YieldSupplyTransactionRepository
import com.tangem.domain.yield.supply.incentives.YieldIncentivesRepository
import com.tangem.domain.yield.supply.models.YieldSupplyClaimOrigin
import com.tangem.domain.yield.supply.models.YieldSupplyClaimOutcome
import com.tangem.domain.yield.supply.models.YieldSupplyTransactionOutcome
import com.tangem.test.core.ProvideTestModels
import com.tangem.test.core.TestAppCoroutineScope
import com.tangem.test.mock.yield.MockYieldSupply
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
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

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class YieldSupplyClaimTrackerTest {

    private val transactionRepository: YieldSupplyTransactionRepository = mockk()
    private val incentivesRepository: YieldIncentivesRepository = mockk(relaxUnitFun = true)

    @BeforeEach
    fun resetMocks() {
        clearMocks(transactionRepository, incentivesRepository)
    }

    @Test
    fun `GIVEN receipt confirms WHEN claim tracked THEN confirmed outcome emitted and user cache invalidated`() =
        runTest {
            // Arrange
            coEvery {
                transactionRepository.getTransactionOutcome(USER_WALLET_ID, TOKEN, TX_HASH)
            } returns YieldSupplyTransactionOutcome.Confirmed
            val tracker = createTracker(testScope = this)
            val outcomes = collectOutcomes(tracker)

            // Act
            tracker.track()
            advanceUntilIdle()
            runCurrent()

            // Assert
            assertThat(outcomes).containsExactly(
                YieldSupplyClaimOutcome(
                    userWalletId = USER_WALLET_ID,
                    cryptoCurrencyId = TOKEN.id,
                    chainId = CHAIN_ID,
                    userAddress = USER_ADDRESS,
                    outcome = YieldSupplyTransactionOutcome.Confirmed,
                    origin = YieldSupplyClaimOrigin.ClaimSheet,
                ),
            )
            coVerify(exactly = 1) {
                incentivesRepository.invalidateUserCache(chainId = CHAIN_ID, userAddress = USER_ADDRESS)
            }
        }

    @Test
    fun `GIVEN receipt reverts WHEN claim tracked THEN reverted outcome emitted and cache left alone`() = runTest {
        // Arrange
        coEvery {
            transactionRepository.getTransactionOutcome(USER_WALLET_ID, TOKEN, TX_HASH)
        } returns YieldSupplyTransactionOutcome.Reverted
        val tracker = createTracker(testScope = this)
        val outcomes = collectOutcomes(tracker)

        // Act
        tracker.track()
        advanceUntilIdle()
        runCurrent()

        // Assert
        assertThat(outcomes).containsExactly(
            YieldSupplyClaimOutcome(
                userWalletId = USER_WALLET_ID,
                cryptoCurrencyId = TOKEN.id,
                chainId = CHAIN_ID,
                userAddress = USER_ADDRESS,
                outcome = YieldSupplyTransactionOutcome.Reverted,
                origin = YieldSupplyClaimOrigin.ClaimSheet,
            ),
        )
        coVerify(exactly = 0) { incentivesRepository.invalidateUserCache(any(), any()) }
    }

    @Test
    fun `GIVEN transaction not mined WHEN attempts run out THEN no outcome and claim no longer in flight`() = runTest {
        // Arrange
        coEvery { transactionRepository.getTransactionOutcome(USER_WALLET_ID, TOKEN, TX_HASH) } returns null
        val tracker = createTracker(testScope = this)
        val outcomes = collectOutcomes(tracker)

        // Act
        tracker.track()
        advanceUntilIdle()

        // Assert
        assertThat(outcomes).isEmpty()
        assertThat(tracker.isClaimInFlight(CHAIN_ID, USER_ADDRESS).first()).isFalse()
    }

    @Test
    fun `GIVEN claim submitted WHEN receipt is still pending THEN claim reads as in flight`() = runTest {
        // Arrange
        coEvery { transactionRepository.getTransactionOutcome(USER_WALLET_ID, TOKEN, TX_HASH) } returns null

        val tracker = createTracker(testScope = this)

        // Act
        tracker.track()
        advanceTimeBy(delayTimeMillis = 15_000)

        // Assert
        assertThat(tracker.isClaimInFlight(CHAIN_ID, USER_ADDRESS).first()).isTrue()
    }

    @Test
    fun `GIVEN claim already tracked WHEN tracked again THEN the receipt is polled by one watcher only`() = runTest {
        // Arrange
        coEvery {
            transactionRepository.getTransactionOutcome(USER_WALLET_ID, TOKEN, TX_HASH)
        } returns YieldSupplyTransactionOutcome.Confirmed

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
        coEvery { transactionRepository.getTransactionOutcome(USER_WALLET_ID, TOKEN, TX_HASH) } returns null
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
            coEvery {
                transactionRepository.getTransactionOutcome(USER_WALLET_ID, TOKEN, TX_HASH)
            } returns YieldSupplyTransactionOutcome.Confirmed
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
                origin = YieldSupplyClaimOrigin.ClaimSheet,
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

    private fun createTracker(testScope: TestScope) = YieldSupplyClaimTracker(
        yieldSupplyTransactionRepository = transactionRepository,
        yieldIncentivesRepository = incentivesRepository,
        coroutineScope = TestAppCoroutineScope(testScope),
    )

    private fun YieldSupplyClaimTracker.track() = track(
        userWalletId = USER_WALLET_ID,
        cryptoCurrency = TOKEN,
        chainId = CHAIN_ID,
        userAddress = USER_ADDRESS,
        transactionHash = TX_HASH,
        origin = YieldSupplyClaimOrigin.ClaimSheet,
    )

    /**
     * `advanceUntilIdle` stops at the foreground work, so the collector is drained with an extra `runCurrent` at
     * the call sites rather than here — the subscription itself must exist before the claim is tracked.
     */
    private fun TestScope.collectOutcomes(tracker: YieldSupplyClaimTracker): List<YieldSupplyClaimOutcome> {
        val collected = mutableListOf<YieldSupplyClaimOutcome>()
        backgroundScope.launch { tracker.outcomes.collect(collected::add) }
        runCurrent()

        return collected
    }

    data class InFlightModel(val chainId: Int, val userAddress: String, val isInFlight: Boolean)

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

        private fun createToken(contractAddress: String, symbol: String) = MockYieldSupply.token(
            contractAddress = contractAddress,
            name = symbol,
            symbol = symbol,
        )
    }
}